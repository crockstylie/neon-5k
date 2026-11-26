package fr.antoinehory.neon5k.ui.game

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.antoinehory.neon5k.data.export.AiDatasetExporter
import fr.antoinehory.neon5k.data.export.DatasetExportFormat
import fr.antoinehory.neon5k.data.export.DatasetExportResult
import fr.antoinehory.neon5k.data.local.AppDatabase
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.preferences.UserPreferences
import fr.antoinehory.neon5k.data.preferences.UserPreferencesRepository
import fr.antoinehory.neon5k.data.repository.GameRepository
import fr.antoinehory.neon5k.data.sensors.SensorEntropyCollector
import fr.antoinehory.neon5k.domain.ai.BotProfile
import fr.antoinehory.neon5k.domain.ai.BotTurnAction
import fr.antoinehory.neon5k.domain.ai.HeuristicBotPolicy
import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.MoveValidationError
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.CyberThemePreset
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * État immuable exposé à `GameScreen` via `StateFlow` (UDF strict).
 */
@Immutable
data class GameUiState(
    val match: MatchState,
    val lastConfiguredRules: GameRulesConfig = GameRulesConfig.CLASSIC_5_DICE,
    val showEntropyDialog: Boolean = false,
    val showRulesDialog: Boolean = false,
    val hasActiveMatch: Boolean = false,
    val isBotThinking: Boolean = false,
    val errorBannerFr: String? = null,
    val exportStatusMessageFr: String? = null
)

/**
 * Événements utilisateur envoyés depuis `GameScreen` vers [GameViewModel].
 */
sealed interface GameUiEvent {
    data class RollDice(
        val touchX: Float = 0f,
        val touchY: Float = 0f,
        val pressDurationMs: Long = 0L,
        val triggeredByShake: Boolean = false
    ) : GameUiEvent

    data class AcceptPiggybackAndRoll(
        val touchX: Float = 0f,
        val touchY: Float = 0f,
        val pressDurationMs: Long = 0L
    ) : GameUiEvent

    data class ToggleDie(val dieIndex: Int) : GameUiEvent
    data object SelectAllScoring : GameUiEvent
    data object BankTurn : GameUiEvent
    data object AcknowledgeBust : GameUiEvent
    data object ToggleEntropyDialog : GameUiEvent
    data object ToggleRulesDialog : GameUiEvent
    data object RestartSameMatch : GameUiEvent
}

/**
 * ViewModel principal de Neon 5K (Lots 1 & 2).
 * Orchestre :
 * - le moteur pur [FiveThousandEngine],
 * - l'IA locale [HeuristicBotPolicy] avec délais visuels lisibles,
 * - le collecteur d'entropie hybride [SensorEntropyCollector],
 * - la persistance Room [GameRepository] (reprise automatique + stats + logs IA) et DataStore [UserPreferencesRepository].
 */
class GameViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val entropyCollector = SensorEntropyCollector(application)
    private val database = AppDatabase.getInstance(application)
    private val gameRepository = GameRepository(database)
    private val preferencesRepository = UserPreferencesRepository(application)
    private val datasetExporter = AiDatasetExporter(application)

    private var botTurnJob: Job? = null

    private val defaultPlayers = listOf(
        PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
        PlayerState(id = 1, name = "CYBER-BOT", type = PlayerType.BOT_BALANCED)
    )

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val playerStatsState: StateFlow<PlayerStatsEntity> = gameRepository.playerStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerStatsEntity()
        )

    val decisionLogsCountState: StateFlow<Int> = gameRepository.totalDecisionLogsCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private val _uiState = MutableStateFlow(
        GameUiState(
            match = FiveThousandEngine.startNewMatch(
                matchId = UUID.randomUUID().toString(),
                rules = GameRulesConfig.CLASSIC_5_DICE,
                players = defaultPlayers
            ),
            lastConfiguredRules = GameRulesConfig.CLASSIC_5_DICE,
            hasActiveMatch = false
        )
    )
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        // Restauration automatique de la dernière partie en cours sauvegardée dans Room
        viewModelScope.launch {
            val savedMatch = gameRepository.getLatestActiveMatch()
            if (savedMatch != null && !savedMatch.isFinished) {
                _uiState.update {
                    it.copy(
                        match = savedMatch,
                        lastConfiguredRules = savedMatch.rules,
                        hasActiveMatch = true
                    )
                }
                scheduleBotActionIfNeeded()
            }
        }

        // Écoute des secousses physiques détectées par l'accéléromètre lorsque GameScreen est au premier plan
        viewModelScope.launch {
            entropyCollector.shakeEvents.collect {
                if (!preferencesState.value.shakeToRollEnabled) return@collect
                val current = _uiState.value.match
                val isHumanTurn = current.activePlayer.type == PlayerType.HUMAN
                val canRollOnShake = isHumanTurn && !current.isFinished && (
                    current.turn.phase == TurnPhase.AWAITING_INITIAL_ROLL ||
                        current.turn.phase == TurnPhase.HOT_DICE_MANDATORY_REROLL ||
                        (current.turn.phase == TurnPhase.SELECTING_DICE && current.turn.isSelectionValid)
                    )
                if (canRollOnShake) {
                    onEvent(GameUiEvent.RollDice(triggeredByShake = true))
                }
            }
        }
    }

    /**
     * Démarre une nouvelle partie, sauvegarde la configuration favorite dans DataStore et persiste l'état initial dans Room.
     */
    fun startMatch(
        rules: GameRulesConfig = preferencesState.value.favoriteRules,
        players: List<PlayerState> = defaultPlayers
    ) {
        botTurnJob?.cancel()
        val freshMatch = FiveThousandEngine.startNewMatch(
            matchId = UUID.randomUUID().toString(),
            rules = rules,
            players = players
        )
        _uiState.update {
            it.copy(
                match = freshMatch,
                lastConfiguredRules = rules,
                showEntropyDialog = false,
                hasActiveMatch = true,
                isBotThinking = false,
                errorBannerFr = null
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveFavoriteRules(rules)
            gameRepository.saveMatchSnapshot(freshMatch)
        }
        scheduleBotActionIfNeeded()
    }

    /**
     * Active les capteurs (gyroscope / accéléromètre) lorsque `GameScreen` entre en `ON_RESUME`.
     */
    fun onGameScreenResumed() {
        entropyCollector.startListening()
        scheduleBotActionIfNeeded()
    }

    /**
     * Coupe immédiatement les capteurs lorsque `GameScreen` passe en `ON_PAUSE` (zéro drain batterie).
     */
    fun onGameScreenPaused() {
        entropyCollector.stopListening()
        botTurnJob?.cancel()
    }

    fun onEvent(event: GameUiEvent) {
        // Ignorer les clics de jeu manuels pendant que le bot joue son tour (hors boîtes de dialogue / revanche)
        val currentMatch = _uiState.value.match
        val isBotTurn = BotProfile.fromPlayerType(currentMatch.activePlayer.type) != null && !currentMatch.isFinished
        val isDialogOrRestart = event is GameUiEvent.ToggleEntropyDialog ||
            event is GameUiEvent.ToggleRulesDialog ||
            event is GameUiEvent.RestartSameMatch
        if (isBotTurn && !isDialogOrRestart) {
            return
        }

        when (event) {
            is GameUiEvent.RollDice -> executeRollDice(event)
            is GameUiEvent.AcceptPiggybackAndRoll -> executePiggybackRoll(event)
            is GameUiEvent.ToggleDie -> applyEngineResult(
                FiveThousandEngine.toggleDieSelection(currentMatch, event.dieIndex),
                persistSnapshot = false
            )
            GameUiEvent.SelectAllScoring -> applyEngineResult(
                FiveThousandEngine.selectAllScoringDice(currentMatch),
                persistSnapshot = false
            )
            GameUiEvent.BankTurn -> executeBankTurn()
            GameUiEvent.AcknowledgeBust -> executeAcknowledgeBust()
            GameUiEvent.ToggleEntropyDialog -> {
                _uiState.update { it.copy(showEntropyDialog = !it.showEntropyDialog) }
            }
            GameUiEvent.ToggleRulesDialog -> {
                _uiState.update { it.copy(showRulesDialog = !it.showRulesDialog) }
            }
            GameUiEvent.RestartSameMatch -> {
                val resetPlayers = currentMatch.players.map { p ->
                    p.copy(
                        score = 0,
                        previousScore = 0,
                        isOpened = false,
                        consecutiveBusts = 0,
                        checkpoints = emptyList()
                    )
                }
                startMatch(rules = currentMatch.rules, players = resetPlayers)
            }
        }
    }

    // --- Actions de Personnalisation (DataStore) ---

    fun setThemePreset(preset: CyberThemePreset) {
        viewModelScope.launch { preferencesRepository.setThemePreset(preset) }
    }

    fun updateCustomPalette(palette: CyberPalette) {
        viewModelScope.launch { preferencesRepository.updateCustomPalette(palette) }
    }

    fun setDiceStyle(style: DiceVisualStyle) {
        viewModelScope.launch { preferencesRepository.setDiceStyle(style) }
    }

    fun setShowScanlines(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShowScanlines(enabled) }
    }

    fun setLeftHandedMode(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLeftHandedMode(enabled) }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setHapticEnabled(enabled) }
    }

    fun setShakeToRollEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setShakeToRollEnabled(enabled) }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setKeepScreenOn(enabled) }
    }

    // --- Export SAF & Purge du Dataset IA ---

    fun exportAiDataset(uri: Uri, format: DatasetExportFormat) {
        viewModelScope.launch {
            val logs = gameRepository.getAllDecisionsForExport()
            when (val res = datasetExporter.exportToUri(uri, format, logs)) {
                is DatasetExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exportStatusMessageFr = "Export réussi : ${res.exportedCount} décisions exportées en .${res.format.extension}"
                        )
                    }
                }
                is DatasetExportResult.Error -> {
                    _uiState.update { it.copy(exportStatusMessageFr = res.messageFr) }
                }
            }
        }
    }

    fun clearAiDataset() {
        viewModelScope.launch {
            gameRepository.clearDecisionLogs()
            _uiState.update { it.copy(exportStatusMessageFr = "Journal des décisions purgé.") }
        }
    }

    // --- Exécution Métier & Journalisation IA ---

    private fun executeRollDice(event: GameUiEvent.RollDice) {
        val beforeMatch = _uiState.value.match
        // Si on relance depuis SELECTING_DICE, enregistrer la décision ROLL_AGAIN dans decision_logs
        if (beforeMatch.turn.phase == TurnPhase.SELECTING_DICE && beforeMatch.turn.isSelectionValid) {
            val keptFaces = beforeMatch.turn.dice.filter { !it.isLocked && it.isSelected }.map { it.face }
            viewModelScope.launch {
                gameRepository.logDecision(
                    stateBeforeAction = beforeMatch,
                    keptFaces = keptFaces,
                    decision = "ROLL_AGAIN"
                )
            }
        }

        val nextRollNumber = beforeMatch.totalRollsInMatch + 1
        val outcome = entropyCollector.captureAndRoll(
            diceCount = beforeMatch.rules.diceCount,
            rollCounter = nextRollNumber,
            touchX = event.touchX,
            touchY = event.touchY,
            pressDurationMs = event.pressDurationMs,
            triggeredByShake = event.triggeredByShake
        )
        val result = FiveThousandEngine.rollDice(
            state = beforeMatch,
            rolledFaces = outcome.faces,
            entropySnapshot = outcome.snapshot
        )
        applyEngineResult(result, persistSnapshot = true)

        if (result is EngineResult.Success && result.state.isFinished) {
            recordInstantWinCompletion(beforeMatch, result.state)
        }
    }

    private fun executePiggybackRoll(event: GameUiEvent.AcceptPiggybackAndRoll) {
        val beforeMatch = _uiState.value.match
        val nextRollNumber = beforeMatch.totalRollsInMatch + 1
        val outcome = entropyCollector.captureAndRoll(
            diceCount = beforeMatch.rules.diceCount,
            rollCounter = nextRollNumber,
            touchX = event.touchX,
            touchY = event.touchY,
            pressDurationMs = event.pressDurationMs,
            triggeredByShake = false
        )
        val result = FiveThousandEngine.acceptPiggybackAndRoll(
            state = beforeMatch,
            rolledFaces = outcome.faces,
            entropySnapshot = outcome.snapshot
        )
        applyEngineResult(result, persistSnapshot = true)
    }

    private fun executeBankTurn() {
        val beforeMatch = _uiState.value.match
        val turnBefore = beforeMatch.turn
        val actorBefore = beforeMatch.activePlayer
        val keptFaces = turnBefore.dice.filter { !it.isLocked && it.isSelected }.map { it.face }
        val finalTurnPoints = turnBefore.totalTurnPointsWithSelection
        val hotDiceInTurn = turnBefore.hotDiceCountInTurn + if (turnBefore.selectionTriggersHotDice) 1 else 0

        val result = FiveThousandEngine.bankTurn(beforeMatch)
        applyEngineResult(result, persistSnapshot = true)

        if (result is EngineResult.Success) {
            val afterState = result.state
            viewModelScope.launch {
                gameRepository.logDecision(
                    stateBeforeAction = beforeMatch,
                    keptFaces = keptFaces,
                    decision = "BANK"
                )
                gameRepository.recordTurnFinished(
                    matchUuid = beforeMatch.matchId,
                    turnNumber = turnBefore.turnNumber,
                    actorType = actorBefore.type,
                    finalTurnPoints = finalTurnPoints,
                    hotDiceInTurn = hotDiceInTurn,
                    usedPiggyback = turnBefore.usedPiggyback,
                    rollsInTurn = turnBefore.rollIndexInTurn
                )
                if (afterState.isFinished) {
                    gameRepository.recordMatchFinished(afterState)
                }
            }
        }
    }

    private fun executeAcknowledgeBust() {
        val beforeMatch = _uiState.value.match
        val turnBefore = beforeMatch.turn
        val actorBefore = beforeMatch.activePlayer

        val result = FiveThousandEngine.acknowledgeBustAndEndTurn(beforeMatch)
        applyEngineResult(result, persistSnapshot = true)

        if (result is EngineResult.Success) {
            viewModelScope.launch {
                gameRepository.recordTurnFinished(
                    matchUuid = beforeMatch.matchId,
                    turnNumber = turnBefore.turnNumber,
                    actorType = actorBefore.type,
                    finalTurnPoints = 0,
                    hotDiceInTurn = turnBefore.hotDiceCountInTurn,
                    usedPiggyback = turnBefore.usedPiggyback,
                    rollsInTurn = turnBefore.rollIndexInTurn
                )
            }
        }
    }

    private fun recordInstantWinCompletion(beforeMatch: MatchState, afterState: MatchState) {
        val turnBefore = beforeMatch.turn
        val actorBefore = beforeMatch.activePlayer
        viewModelScope.launch {
            gameRepository.recordTurnFinished(
                matchUuid = afterState.matchId,
                turnNumber = turnBefore.turnNumber,
                actorType = actorBefore.type,
                finalTurnPoints = afterState.turn.totalTurnPointsWithSelection,
                hotDiceInTurn = afterState.turn.hotDiceCountInTurn + 1,
                usedPiggyback = afterState.turn.usedPiggyback,
                rollsInTurn = afterState.turn.rollIndexInTurn
            )
            gameRepository.recordMatchFinished(afterState)
        }
    }

    private fun applyEngineResult(
        result: EngineResult,
        persistSnapshot: Boolean
    ) {
        when (result) {
            is EngineResult.Success -> {
                _uiState.update {
                    it.copy(
                        match = result.state,
                        hasActiveMatch = !result.state.isFinished,
                        errorBannerFr = null
                    )
                }
                if (persistSnapshot) {
                    viewModelScope.launch {
                        gameRepository.saveMatchSnapshot(result.state)
                    }
                }
                scheduleBotActionIfNeeded()
            }

            is EngineResult.Rejected -> {
                val message = when (val err = result.error) {
                    is MoveValidationError.OpeningThresholdNotMet ->
                        "Ouverture requise : ${err.required} pts minimum (actuel : ${err.current} pts)."
                    is MoveValidationError.TargetOverflowNotAllowed ->
                        "Dépassement interdit : ${err.projected} > ${err.target} pts (score exact actif)."
                    MoveValidationError.MandatoryHotDiceRerollRequired ->
                        "Main pleine ! Relance obligatoire de tous les dés."
                    MoveValidationError.SelectionContainsDeadDiceOrEmpty ->
                        "Sélectionnez au moins une combinaison scorante valide."
                    MoveValidationError.MustBeOpenedToPiggyback ->
                        "Vous devez être ouvert pour repiquer le pot précédent."
                    else -> null
                }
                _uiState.update { it.copy(errorBannerFr = message) }
            }
        }
    }

    /**
     * Orchestre le tour automatique d'un Bot local avec des délais lisibles (`550ms`–`750ms`)
     * pour que le joueur humain voie clairement les dés tirés, la sélection du bot et sa décision.
     */
    private fun scheduleBotActionIfNeeded() {
        botTurnJob?.cancel()
        val currentMatch = _uiState.value.match
        if (currentMatch.isFinished) {
            _uiState.update { it.copy(isBotThinking = false) }
            return
        }

        val botProfile = BotProfile.fromPlayerType(currentMatch.activePlayer.type)
        if (botProfile == null) {
            _uiState.update { it.copy(isBotThinking = false) }
            return
        }

        botTurnJob = viewModelScope.launch {
            _uiState.update { it.copy(isBotThinking = true) }
            when (currentMatch.turn.phase) {
                TurnPhase.AWAITING_INITIAL_ROLL -> {
                    delay(600L)
                    if (HeuristicBotPolicy.shouldAcceptPiggyback(currentMatch, botProfile)) {
                        executePiggybackRoll(GameUiEvent.AcceptPiggybackAndRoll())
                    } else {
                        executeRollDice(GameUiEvent.RollDice())
                    }
                }

                TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                    delay(700L)
                    executeRollDice(GameUiEvent.RollDice())
                }

                TurnPhase.SELECTING_DICE -> {
                    delay(650L)
                    val plan = HeuristicBotPolicy.planMove(currentMatch, botProfile)
                    // 1. Appliquer visuellement la sélection de dés du bot
                    var workingState = _uiState.value.match
                    for (dieIdx in plan.indicesToSelect) {
                        val die = workingState.turn.dice.getOrNull(dieIdx)
                        if (die != null && !die.isLocked && !die.isSelected) {
                            val toggled = FiveThousandEngine.toggleDieSelection(workingState, dieIdx)
                            if (toggled is EngineResult.Success) {
                                workingState = toggled.state
                            }
                        }
                    }
                    _uiState.update { it.copy(match = workingState) }

                    // 2. Laisser 650ms au joueur pour voir les dés sélectionnés par le bot avant qu'il ne relance ou banque
                    delay(650L)
                    when (plan.nextAction) {
                        BotTurnAction.ROLL_AGAIN -> executeRollDice(GameUiEvent.RollDice())
                        BotTurnAction.BANK -> executeBankTurn()
                    }
                }

                TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                    delay(950L)
                    executeAcknowledgeBust()
                }
            }
        }
    }

    override fun onCleared() {
        botTurnJob?.cancel()
        entropyCollector.stopListening()
    }
}
