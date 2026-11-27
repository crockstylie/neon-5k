package fr.antoinehory.neon5k.ui.game

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.antoinehory.neon5k.data.export.AiDatasetExporter
import fr.antoinehory.neon5k.data.export.DatasetExportFormat
import fr.antoinehory.neon5k.data.export.DatasetExportResult
import fr.antoinehory.neon5k.data.local.AppDatabase
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity
import fr.antoinehory.neon5k.data.playgames.GooglePlayGamesService
import fr.antoinehory.neon5k.data.playgames.PlayGamesStatus
import fr.antoinehory.neon5k.data.preferences.UserPreferences
import fr.antoinehory.neon5k.data.preferences.UserPreferencesRepository
import fr.antoinehory.neon5k.data.repository.GameRepository
import fr.antoinehory.neon5k.data.sensors.SensorEntropyCollector
import fr.antoinehory.neon5k.domain.achievement.AchievementId
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress
import fr.antoinehory.neon5k.domain.achievement.AchievementUnlockNotification
import fr.antoinehory.neon5k.domain.ai.AdaptiveLearningSummary
import fr.antoinehory.neon5k.domain.ai.BotPolicy
import fr.antoinehory.neon5k.domain.ai.BotProfile
import fr.antoinehory.neon5k.domain.ai.BotTurnAction
import fr.antoinehory.neon5k.domain.ai.HeuristicBotPolicy
import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.MoveValidationError
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.CyberThemePreset
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * État UI observable par les écrans de Neon 5K.
 */
data class GameUiState(
    val match: MatchState = FiveThousandEngine.startNewMatch(
        matchId = "match-init",
        rules = GameRulesConfig.CLASSIC_5_DICE,
        players = listOf(
            PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
            PlayerState(id = 1, name = "CYBER-BOT", type = PlayerType.BOT_BALANCED)
        )
    ),
    val hasActiveMatch: Boolean = false,
    val isBotThinking: Boolean = false,
    val isBotPaused: Boolean = false,
    val rollTriggerCounter: Int = 0,
    val showEntropyDialog: Boolean = false,
    val showRulesDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val errorBannerFr: String? = null,
    val exportStatusMessageFr: String? = null,
    val latestAchievementToast: AchievementUnlockNotification? = null
)

/**
 * Événements utilisateur envoyés depuis `GameScreen`.
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
    data object ToggleBotPause : GameUiEvent
    data object StepBotTurn : GameUiEvent
    data object RestartMatch : GameUiEvent
    data object ToggleEntropyDialog : GameUiEvent
    data object ToggleRulesDialog : GameUiEvent
    data object ToggleHistoryDialog : GameUiEvent
    data object DismissErrorBanner : GameUiEvent
    data object DismissAchievementToast : GameUiEvent
}

/**
 * ViewModel principal de Neon 5K.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val gameRepository = GameRepository(
        matchDao = database.matchDao(),
        statsDao = database.statsDao(),
        decisionLogDao = database.decisionLogDao(),
        achievementDao = database.achievementDao()
    )
    private val preferencesRepository = UserPreferencesRepository(application)
    private val datasetExporter = AiDatasetExporter(application)
    private val playGamesService = GooglePlayGamesService(application)
    private val sensorCollector = SensorEntropyCollector(application)
    private val botPolicy: BotPolicy = HeuristicBotPolicy

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences()
        )

    val playerStatsState: StateFlow<PlayerStatsEntity> = gameRepository.playerStatsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerStatsEntity()
        )

    val finishedMatchesState: StateFlow<List<SavedMatchEntity>> = gameRepository.finishedMatchesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val achievementsState: StateFlow<List<AchievementProgress>> = gameRepository.achievementsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementId.entries.map { AchievementProgress(id = it) }
        )

    val playGamesStatusState: StateFlow<PlayGamesStatus> = playGamesService.status

    val decisionLogsCountState: StateFlow<Int> = gameRepository.decisionLogsCountFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val adaptiveLearningState: StateFlow<AdaptiveLearningSummary> = gameRepository.adaptiveLearningFlow

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var botTurnJob: Job? = null
    private var currentTurnStepCounter: Int = 0

    init {
        viewModelScope.launch {
            gameRepository.refreshAdaptiveAiModel()
            val savedMatch = gameRepository.loadLatestActiveMatch()
            if (savedMatch != null && !savedMatch.isFinished) {
                _uiState.update {
                    it.copy(
                        match = savedMatch,
                        hasActiveMatch = true
                    )
                }
                scheduleBotTurnIfNeeded(savedMatch)
            }
        }
        viewModelScope.launch {
            gameRepository.activeMatchFlow.collectLatest { activeMatch ->
                _uiState.update { current ->
                    current.copy(hasActiveMatch = activeMatch != null && !activeMatch.isFinished)
                }
            }
        }
        viewModelScope.launch {
            playGamesService.status
                .map { it.isAuthenticated }
                .distinctUntilChanged()
                .collectLatest { authenticated ->
                    if (authenticated) {
                        val allProgress = gameRepository.getAllAchievementsProgress()
                        playGamesService.syncAchievementsToPlayGames(allProgress)
                    }
                }
        }
        viewModelScope.launch {
            sensorCollector.shakeEvents.collectLatest {
                if (preferencesState.value.shakeToRollEnabled) {
                    val currentMatch = _uiState.value.match
                    val phase = currentMatch.turn.phase
                    val isHumanTurn = currentMatch.activePlayer.type == PlayerType.HUMAN
                    if (isHumanTurn && !currentMatch.isFinished &&
                        (phase == TurnPhase.AWAITING_INITIAL_ROLL ||
                            phase == TurnPhase.HOT_DICE_MANDATORY_REROLL ||
                            (phase == TurnPhase.SELECTING_DICE && currentMatch.turn.isSelectionValid))
                    ) {
                        onEvent(GameUiEvent.RollDice(triggeredByShake = true))
                    }
                }
            }
        }
    }

    fun attachActivity(activity: Activity) {
        playGamesService.attachActivity(activity)
    }

    fun detachActivity(activity: Activity) {
        playGamesService.detachActivity(activity)
    }

    fun onGameScreenResumed() {
        sensorCollector.startListening()
        scheduleBotTurnIfNeeded(_uiState.value.match)
    }

    fun onGameScreenPaused() {
        sensorCollector.stopListening()
    }

    override fun onCleared() {
        super.onCleared()
        botTurnJob?.cancel()
        sensorCollector.stopListening()
    }

    fun startMatch(
        rules: GameRulesConfig,
        players: List<PlayerState>
    ) {
        botTurnJob?.cancel()
        currentTurnStepCounter = 0
        val newMatch = FiveThousandEngine.startNewMatch(
            matchId = "match-${UUID.randomUUID()}",
            rules = rules,
            players = players
        )
        _uiState.update {
            it.copy(
                match = newMatch,
                hasActiveMatch = true,
                isBotThinking = false,
                rollTriggerCounter = 0,
                errorBannerFr = null,
                showEntropyDialog = false,
                showRulesDialog = false,
                showHistoryDialog = false
            )
        }
        viewModelScope.launch {
            preferencesRepository.saveFavoriteRules(rules)
            gameRepository.saveMatchState(newMatch)
        }
        scheduleBotTurnIfNeeded(newMatch)
    }

    fun onEvent(event: GameUiEvent) {
        when (event) {
            is GameUiEvent.RollDice -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                executeRoll(
                    state = current,
                    touchX = event.touchX,
                    touchY = event.touchY,
                    pressDurationMs = event.pressDurationMs,
                    triggeredByShake = event.triggeredByShake,
                    isPiggyback = false,
                    isHumanActor = true
                )
            }

            is GameUiEvent.AcceptPiggybackAndRoll -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                executeRoll(
                    state = current,
                    touchX = event.touchX,
                    touchY = event.touchY,
                    pressDurationMs = event.pressDurationMs,
                    triggeredByShake = false,
                    isPiggyback = true,
                    isHumanActor = true
                )
            }

            is GameUiEvent.ToggleDie -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                when (val result = FiveThousandEngine.toggleDieSelection(current, event.dieIndex)) {
                    is EngineResult.Success -> updateMatchAndPersist(result.state)
                    is EngineResult.Rejected -> {
                        _uiState.update { it.copy(errorBannerFr = formatRejectionMessage(result.error)) }
                    }
                }
            }

            GameUiEvent.SelectAllScoring -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                when (val result = FiveThousandEngine.selectAllScoringDice(current)) {
                    is EngineResult.Success -> updateMatchAndPersist(result.state)
                    is EngineResult.Rejected -> {
                        _uiState.update { it.copy(errorBannerFr = formatRejectionMessage(result.error)) }
                    }
                }
            }

            GameUiEvent.BankTurn -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                executeBankTurn(current, isHumanActor = true)
            }

            GameUiEvent.AcknowledgeBust -> {
                val current = _uiState.value.match
                if (current.activePlayer.type != PlayerType.HUMAN) return
                executeAcknowledgeBust(current, isHumanActor = true)
            }

            GameUiEvent.ToggleBotPause -> {
                val nextPaused = !_uiState.value.isBotPaused
                if (nextPaused) {
                    botTurnJob?.cancel()
                    _uiState.update { it.copy(isBotPaused = true, isBotThinking = false) }
                } else {
                    _uiState.update { it.copy(isBotPaused = false) }
                    scheduleBotTurnIfNeeded(_uiState.value.match)
                }
            }

            GameUiEvent.StepBotTurn -> {
                val current = _uiState.value.match
                val botProfile = BotProfile.fromPlayerType(current.activePlayer.type)
                if (botProfile != null && !current.isFinished) {
                    executeSingleBotStep(current, botProfile)
                }
            }

            GameUiEvent.RestartMatch -> {
                val current = _uiState.value.match
                val resetPlayers = current.players.map { p ->
                    p.copy(
                        score = 0,
                        previousScore = 0,
                        isOpened = current.rules.openingThreshold == 0,
                        consecutiveBusts = 0,
                        checkpoints = emptyList()
                    )
                }
                startMatch(rules = current.rules, players = resetPlayers)
            }

            GameUiEvent.ToggleEntropyDialog -> {
                _uiState.update { it.copy(showEntropyDialog = !it.showEntropyDialog) }
            }

            GameUiEvent.ToggleRulesDialog -> {
                _uiState.update { it.copy(showRulesDialog = !it.showRulesDialog) }
            }

            GameUiEvent.ToggleHistoryDialog -> {
                _uiState.update { it.copy(showHistoryDialog = !it.showHistoryDialog) }
            }

            GameUiEvent.DismissErrorBanner -> {
                _uiState.update { it.copy(errorBannerFr = null) }
            }

            GameUiEvent.DismissAchievementToast -> {
                _uiState.update { it.copy(latestAchievementToast = null) }
            }
        }
    }

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

    fun exportAiDataset(uri: Uri, format: DatasetExportFormat) {
        viewModelScope.launch {
            val logs = gameRepository.getAllDecisionLogsForExport()
            if (logs.isEmpty()) {
                _uiState.update {
                    it.copy(exportStatusMessageFr = "Aucune décision enregistrée pour le moment. Jouez une partie d'abord !")
                }
                return@launch
            }
            when (val res = datasetExporter.exportToUri(uri, format, logs)) {
                is DatasetExportResult.Success -> {
                    _uiState.update {
                        it.copy(
                            exportStatusMessageFr = "Export ${res.format.name} réussi (${res.exportedCount} décisions enregistrées)."
                        )
                    }
                }
                is DatasetExportResult.Error -> {
                    _uiState.update {
                        it.copy(exportStatusMessageFr = res.messageFr)
                    }
                }
            }
        }
    }

    fun clearAiDataset() {
        viewModelScope.launch {
            gameRepository.clearAllDecisionLogs()
            _uiState.update {
                it.copy(exportStatusMessageFr = "Journal de décisions IA réinitialisé.")
            }
        }
    }

    fun openPlayGamesAchievements() {
        playGamesService.showNativePlayGamesAchievementsOrSignIn { messageFr ->
            _uiState.update { it.copy(exportStatusMessageFr = messageFr) }
        }
    }

    private suspend fun publishAchievementNotifications(notifications: List<AchievementUnlockNotification>) {
        if (notifications.isEmpty()) return
        val allProgress = gameRepository.getAllAchievementsProgress()
        playGamesService.syncAchievementsToPlayGames(allProgress)
        _uiState.update { current ->
            current.copy(latestAchievementToast = notifications.first())
        }
    }

    private fun executeRoll(
        state: MatchState,
        touchX: Float,
        touchY: Float,
        pressDurationMs: Long,
        triggeredByShake: Boolean,
        isPiggyback: Boolean,
        isHumanActor: Boolean
    ) {
        if (state.turn.phase == TurnPhase.SELECTING_DICE && state.turn.isSelectionValid) {
            val freeFacesBefore = state.turn.dice.filter { !it.isLocked }.map { it.face }
            val keptFaces = state.turn.dice.filter { !it.isLocked && it.isSelected }.map { it.face }
            currentTurnStepCounter++
            val step = currentTurnStepCounter
            viewModelScope.launch {
                gameRepository.logDecisionStep(
                    stateBeforeAction = state,
                    stepIndex = step,
                    rolledFaces = freeFacesBefore,
                    keptFaces = keptFaces,
                    decision = "ROLL_AGAIN"
                )
            }
        } else if (state.turn.phase == TurnPhase.AWAITING_INITIAL_ROLL) {
            currentTurnStepCounter = 0
        }

        val nextRollCounter = state.totalRollsInMatch + 1
        val outcome = sensorCollector.captureAndRoll(
            diceCount = state.rules.diceCount,
            rollCounter = nextRollCounter,
            touchX = touchX,
            touchY = touchY,
            pressDurationMs = pressDurationMs,
            triggeredByShake = triggeredByShake
        )

        val result = if (isPiggyback) {
            FiveThousandEngine.acceptPiggybackAndRoll(state, outcome.faces, outcome.snapshot)
        } else {
            FiveThousandEngine.rollDice(state, outcome.faces, outcome.snapshot)
        }

        when (result) {
            is EngineResult.Success -> {
                val newState = result.state
                val newlyRolledFaces = newState.turn.dice.filter { !it.isLocked }.map { it.face }
                val maxEval = DiceScorer.evaluateMaxScoringSubset(newlyRolledFaces, newState.rules)
                val scoredOnSingleDie = newlyRolledFaces.size == 1 && maxEval.totalPoints > 0

                if (isHumanActor) {
                    viewModelScope.launch {
                        val rollNotifs = gameRepository.recordHumanRoll(
                            rolledFaces = newlyRolledFaces,
                            combinationsOnRoll = maxEval.combinations,
                            triggeredByShake = triggeredByShake,
                            pressDurationMs = pressDurationMs,
                            scoredOnSingleRemainingDie = scoredOnSingleDie
                        )
                        publishAchievementNotifications(rollNotifs)
                    }
                }

                if (newState.isFinished && newState.isInstantWin) {
                    viewModelScope.launch {
                        gameRepository.finalizeTurnDecisionLogs(
                            matchUuid = newState.matchId,
                            turnNumber = newState.turn.turnNumber,
                            finalPoints = newState.rules.targetScore
                        )
                        newState.winnerPlayerId?.let { winnerId ->
                            gameRepository.finalizeMatchDecisionLogs(newState.matchId, winnerId)
                        }
                        val matchNotifs = gameRepository.recordMatchCompleted(newState)
                        publishAchievementNotifications(matchNotifs)
                    }
                }

                _uiState.update { current ->
                    current.copy(
                        match = newState,
                        hasActiveMatch = !newState.isFinished,
                        rollTriggerCounter = current.rollTriggerCounter + 1,
                        errorBannerFr = null
                    )
                }
                viewModelScope.launch { gameRepository.saveMatchState(newState) }
            }

            is EngineResult.Rejected -> {
                _uiState.update { current ->
                    current.copy(errorBannerFr = formatRejectionMessage(result.error))
                }
            }
        }
    }

    private fun executeBankTurn(
        state: MatchState,
        isHumanActor: Boolean
    ) {
        val turnBefore = state.turn
        val activePlayerBefore = state.activePlayer
        val wasOpeningTurn = !activePlayerBefore.isOpened
        val totalBankedPoints = turnBefore.totalTurnPointsWithSelection
        val freeFacesBefore = turnBefore.dice.filter { !it.isLocked }.map { it.face }
        val keptFaces = turnBefore.dice.filter { !it.isLocked && it.isSelected }.map { it.face }

        val projectedScore = activePlayerBefore.score + totalBankedPoints
        val bumpedOpponents = if (state.rules.bumpOpponentMode != BumpOpponentMode.DISABLED && projectedScore > 0) {
            state.players.filter { it.id != activePlayerBefore.id && it.score == projectedScore }
        } else {
            emptyList()
        }
        val didBumpOpponent = bumpedOpponents.isNotEmpty()
        val didBumpHuman = bumpedOpponents.any { it.type == PlayerType.HUMAN }

        when (val result = FiveThousandEngine.bankTurn(state)) {
            is EngineResult.Success -> {
                val newState = result.state
                currentTurnStepCounter++
                val step = currentTurnStepCounter

                viewModelScope.launch {
                    if (keptFaces.isNotEmpty()) {
                        gameRepository.logDecisionStep(
                            stateBeforeAction = state,
                            stepIndex = step,
                            rolledFaces = freeFacesBefore,
                            keptFaces = keptFaces,
                            decision = "BANK"
                        )
                    }
                    gameRepository.finalizeTurnDecisionLogs(
                        matchUuid = state.matchId,
                        turnNumber = turnBefore.turnNumber,
                        finalPoints = totalBankedPoints
                    )
                    if (didBumpHuman && !isHumanActor) {
                        gameRepository.recordHumanBumpedByOpponent()
                    }
                    if (isHumanActor) {
                        val bankNotifs = gameRepository.recordHumanBank(
                            turnPoints = totalBankedPoints,
                            wasOpeningTurn = wasOpeningTurn,
                            hotDiceCountInTurn = turnBefore.hotDiceCountInTurn +
                                if (turnBefore.selectionTriggersHotDice) 1 else 0,
                            usedPiggyback = turnBefore.usedPiggyback,
                            didBumpOpponent = didBumpOpponent
                        )
                        publishAchievementNotifications(bankNotifs)
                    }
                    if (newState.isFinished) {
                        newState.winnerPlayerId?.let { winnerId ->
                            gameRepository.finalizeMatchDecisionLogs(newState.matchId, winnerId)
                        }
                        val matchNotifs = gameRepository.recordMatchCompleted(newState)
                        publishAchievementNotifications(matchNotifs)
                    }
                    gameRepository.saveMatchState(newState)
                }

                _uiState.update {
                    it.copy(
                        match = newState,
                        hasActiveMatch = !newState.isFinished,
                        errorBannerFr = null
                    )
                }
                scheduleBotTurnIfNeeded(newState)
            }

            is EngineResult.Rejected -> {
                _uiState.update { it.copy(errorBannerFr = formatRejectionMessage(result.error)) }
            }
        }
    }

    private fun executeAcknowledgeBust(
        state: MatchState,
        isHumanActor: Boolean
    ) {
        val turnBefore = state.turn
        val playerBefore = state.activePlayer
        val triggeredThreeStrikes = state.rules.threeStrikesMode != ThreeStrikesMode.DISABLED &&
            (playerBefore.consecutiveBusts + 1) >= 3

        when (val result = FiveThousandEngine.acknowledgeBustAndEndTurn(state)) {
            is EngineResult.Success -> {
                val newState = result.state
                currentTurnStepCounter = 0

                viewModelScope.launch {
                    gameRepository.finalizeTurnDecisionLogs(
                        matchUuid = state.matchId,
                        turnNumber = turnBefore.turnNumber,
                        finalPoints = 0
                    )
                    if (isHumanActor) {
                        val bustNotifs = gameRepository.recordHumanBust(
                            lostTurnPot = turnBefore.bankedInTurn,
                            triggeredThreeStrikesPenalty = triggeredThreeStrikes
                        )
                        publishAchievementNotifications(bustNotifs)
                    }
                    gameRepository.saveMatchState(newState)
                }

                _uiState.update {
                    it.copy(
                        match = newState,
                        hasActiveMatch = !newState.isFinished,
                        errorBannerFr = null
                    )
                }
                scheduleBotTurnIfNeeded(newState)
            }

            is EngineResult.Rejected -> {
                _uiState.update { it.copy(errorBannerFr = formatRejectionMessage(result.error)) }
            }
        }
    }

    private fun updateMatchAndPersist(newState: MatchState) {
        _uiState.update {
            it.copy(
                match = newState,
                hasActiveMatch = !newState.isFinished,
                errorBannerFr = null
            )
        }
        viewModelScope.launch { gameRepository.saveMatchState(newState) }
    }

    private fun scheduleBotTurnIfNeeded(state: MatchState) {
        botTurnJob?.cancel()
        if (state.isFinished) {
            _uiState.update { it.copy(isBotThinking = false) }
            return
        }
        val botProfile = BotProfile.fromPlayerType(state.activePlayer.type)
        if (botProfile == null) {
            _uiState.update { it.copy(isBotThinking = false) }
            return
        }
        if (_uiState.value.isBotPaused) {
            _uiState.update { it.copy(isBotThinking = false) }
            return
        }

        botTurnJob = viewModelScope.launch {
            _uiState.update { it.copy(isBotThinking = true) }
            delay(550L)

            while (true) {
                if (_uiState.value.isBotPaused) break
                val current = _uiState.value.match
                val currentProfile = BotProfile.fromPlayerType(current.activePlayer.type)
                if (current.isFinished || currentProfile == null) break

                when (current.turn.phase) {
                    TurnPhase.AWAITING_INITIAL_ROLL -> {
                        val acceptPiggyback = botPolicy.shouldAcceptPiggyback(current, currentProfile)
                        executeRoll(
                            state = current,
                            touchX = 0f,
                            touchY = 0f,
                            pressDurationMs = 0L,
                            triggeredByShake = false,
                            isPiggyback = acceptPiggyback,
                            isHumanActor = false
                        )
                        delay(700L)
                    }

                    TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                        executeRoll(
                            state = current,
                            touchX = 0f,
                            touchY = 0f,
                            pressDurationMs = 0L,
                            triggeredByShake = false,
                            isPiggyback = false,
                            isHumanActor = false
                        )
                        delay(700L)
                    }

                    TurnPhase.SELECTING_DICE -> {
                        val plan = botPolicy.planMove(current, currentProfile)
                        var workingState = current
                        for (dieIndex in plan.indicesToSelect) {
                            val die = workingState.turn.dice.getOrNull(dieIndex)
                            if (die != null && !die.isLocked && !die.isSelected && die.canScoreOnCurrentRoll) {
                                val res = FiveThousandEngine.toggleDieSelection(workingState, dieIndex)
                                if (res is EngineResult.Success) {
                                    workingState = res.state
                                }
                            }
                        }
                        if (!workingState.turn.isSelectionValid) {
                            val fallback = FiveThousandEngine.selectAllScoringDice(current)
                            if (fallback is EngineResult.Success) {
                                workingState = fallback.state
                            }
                        }

                        _uiState.update { it.copy(match = workingState) }
                        delay(650L)
                        if (_uiState.value.isBotPaused) break

                        if (plan.nextAction == BotTurnAction.BANK) {
                            val beforeTurnNum = workingState.turn.turnNumber
                            executeBankTurn(workingState, isHumanActor = false)
                            if (_uiState.value.match.turn.turnNumber == beforeTurnNum &&
                                !_uiState.value.match.isFinished
                            ) {
                                executeRoll(
                                    state = _uiState.value.match,
                                    touchX = 0f,
                                    touchY = 0f,
                                    pressDurationMs = 0L,
                                    triggeredByShake = false,
                                    isPiggyback = false,
                                    isHumanActor = false
                                )
                                delay(700L)
                            } else {
                                break
                            }
                        } else {
                            executeRoll(
                                state = workingState,
                                touchX = 0f,
                                touchY = 0f,
                                pressDurationMs = 0L,
                                triggeredByShake = false,
                                isPiggyback = false,
                                isHumanActor = false
                            )
                            delay(700L)
                        }
                    }

                    TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                        delay(900L)
                        executeAcknowledgeBust(current, isHumanActor = false)
                        break
                    }
                }
            }

            _uiState.update { it.copy(isBotThinking = false) }
        }
    }

    private fun executeSingleBotStep(current: MatchState, currentProfile: BotProfile) {
        when (current.turn.phase) {
            TurnPhase.AWAITING_INITIAL_ROLL -> {
                val acceptPiggyback = botPolicy.shouldAcceptPiggyback(current, currentProfile)
                executeRoll(
                    state = current,
                    touchX = 0f,
                    touchY = 0f,
                    pressDurationMs = 0L,
                    triggeredByShake = false,
                    isPiggyback = acceptPiggyback,
                    isHumanActor = false
                )
            }

            TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                executeRoll(
                    state = current,
                    touchX = 0f,
                    touchY = 0f,
                    pressDurationMs = 0L,
                    triggeredByShake = false,
                    isPiggyback = false,
                    isHumanActor = false
                )
            }

            TurnPhase.SELECTING_DICE -> {
                val plan = botPolicy.planMove(current, currentProfile)
                val unselectedPlanIndices = plan.indicesToSelect.filter { idx ->
                    val d = current.turn.dice.getOrNull(idx)
                    d != null && !d.isLocked && !d.isSelected && d.canScoreOnCurrentRoll
                }

                if (unselectedPlanIndices.isNotEmpty() || !current.turn.isSelectionValid) {
                    var workingState = current
                    for (dieIndex in plan.indicesToSelect) {
                        val die = workingState.turn.dice.getOrNull(dieIndex)
                        if (die != null && !die.isLocked && !die.isSelected && die.canScoreOnCurrentRoll) {
                            val res = FiveThousandEngine.toggleDieSelection(workingState, dieIndex)
                            if (res is EngineResult.Success) {
                                workingState = res.state
                            }
                        }
                    }
                    if (!workingState.turn.isSelectionValid) {
                        val fallback = FiveThousandEngine.selectAllScoringDice(current)
                        if (fallback is EngineResult.Success) {
                            workingState = fallback.state
                        }
                    }
                    _uiState.update { it.copy(match = workingState) }
                    viewModelScope.launch { gameRepository.saveMatchState(workingState) }
                } else {
                    if (plan.nextAction == BotTurnAction.BANK) {
                        val beforeTurnNum = current.turn.turnNumber
                        executeBankTurn(current, isHumanActor = false)
                        if (_uiState.value.match.turn.turnNumber == beforeTurnNum &&
                            !_uiState.value.match.isFinished
                        ) {
                            executeRoll(
                                state = _uiState.value.match,
                                touchX = 0f,
                                touchY = 0f,
                                pressDurationMs = 0L,
                                triggeredByShake = false,
                                isPiggyback = false,
                                isHumanActor = false
                            )
                        }
                    } else {
                        executeRoll(
                            state = current,
                            touchX = 0f,
                            touchY = 0f,
                            pressDurationMs = 0L,
                            triggeredByShake = false,
                            isPiggyback = false,
                            isHumanActor = false
                        )
                    }
                }
            }

            TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                executeAcknowledgeBust(current, isHumanActor = false)
            }
        }
    }

    private fun formatRejectionMessage(error: MoveValidationError): String = when (error) {
        MoveValidationError.MatchAlreadyFinished ->
            "La partie est déjà terminée."
        MoveValidationError.InvalidPhaseForAction ->
            "Action impossible dans la phase actuelle du tour."
        MoveValidationError.DieNotSelectable ->
            "Ce dé est déjà verrouillé ou ne rapporte aucun point sur ce lancer."
        MoveValidationError.SelectionContainsDeadDiceOrEmpty ->
            "Sélectionnez au moins un dé scorant valide (sans dé mort)."
        MoveValidationError.MandatoryHotDiceRerollRequired ->
            "Main pleine ! La règle active impose de relancer tous les dés."
        MoveValidationError.PiggybackNotAvailable ->
            "Aucun pot à repiquer sur ce tour."
        MoveValidationError.MustBeOpenedToPiggyback ->
            "Vous devez avoir ouvert votre compteur avant de pouvoir repiquer."
        is MoveValidationError.OpeningThresholdNotMet ->
            "Ouverture requise : il faut au moins ${error.required} pts en un tour (actuel : ${error.current} pts)."
        is MoveValidationError.MinTurnScoreAfterOpeningNotMet ->
            "Minimum par tour requis : il faut au moins ${error.required} pts pour banquer (actuel : ${error.current} pts)."
        is MoveValidationError.RoundMultipleOf100Required ->
            "Chiffre rond requis (×100) : impossible de banquer ${error.currentTurnPot} pts (relancez ou associez vos 5)."
        is MoveValidationError.FinalSprint1000Required ->
            "Sprint final actif : les 1 000 derniers points jusqu'à ${error.targetScore} doivent être franchis en un seul tour !"
        is MoveValidationError.TargetOverflowNotAllowed ->
            "Dépassement interdit : ${error.projected} pts dépasse la cible exacte de ${error.target} pts."
    }
}
