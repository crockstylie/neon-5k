package fr.antoinehory.neon5k.ui.game

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.antoinehory.neon5k.data.sensors.SensorEntropyCollector
import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.MoveValidationError
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val hasActiveMatch: Boolean = false,
    val errorBannerFr: String? = null
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
    data object RestartSameMatch : GameUiEvent
}

/**
 * ViewModel principal de la table de jeu Neon 5K.
 * Orchestre le moteur pur [FiveThousandEngine] et le collecteur d'entropie hybride [SensorEntropyCollector].
 */
class GameViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val entropyCollector = SensorEntropyCollector(application)

    private val defaultPlayers = listOf(
        PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
        PlayerState(id = 1, name = "JOUEUR 2", type = PlayerType.HUMAN)
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
        // Écoute des secousses physiques détectées par l'accéléromètre lorsque GameScreen est au premier plan
        viewModelScope.launch {
            entropyCollector.shakeEvents.collect {
                val current = _uiState.value.match
                val canRollOnShake = !current.isFinished && (
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
     * Démarre une nouvelle partie avec les règles et joueurs choisis dans `SetupScreen` ou via le bouton Partie Rapide.
     */
    fun startMatch(
        rules: GameRulesConfig = GameRulesConfig.CLASSIC_5_DICE,
        players: List<PlayerState> = defaultPlayers
    ) {
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
                errorBannerFr = null
            )
        }
    }

    /**
     * Active les capteurs (gyroscope / accéléromètre) lorsque `GameScreen` entre en `ON_RESUME`.
     */
    fun onGameScreenResumed() {
        entropyCollector.startListening()
    }

    /**
     * Coupe immédiatement les capteurs lorsque `GameScreen` passe en `ON_PAUSE` (zéro drain batterie).
     */
    fun onGameScreenPaused() {
        entropyCollector.stopListening()
    }

    fun onEvent(event: GameUiEvent) {
        when (event) {
            is GameUiEvent.RollDice -> handleRollDice(event)
            is GameUiEvent.AcceptPiggybackAndRoll -> handlePiggybackRoll(event)
            is GameUiEvent.ToggleDie -> applyEngineResult(
                FiveThousandEngine.toggleDieSelection(_uiState.value.match, event.dieIndex)
            )
            GameUiEvent.SelectAllScoring -> applyEngineResult(
                FiveThousandEngine.selectAllScoringDice(_uiState.value.match)
            )
            GameUiEvent.BankTurn -> applyEngineResult(
                FiveThousandEngine.bankTurn(_uiState.value.match)
            )
            GameUiEvent.AcknowledgeBust -> applyEngineResult(
                FiveThousandEngine.acknowledgeBustAndEndTurn(_uiState.value.match)
            )
            GameUiEvent.ToggleEntropyDialog -> {
                _uiState.update { it.copy(showEntropyDialog = !it.showEntropyDialog) }
            }
            GameUiEvent.RestartSameMatch -> {
                val current = _uiState.value.match
                val resetPlayers = current.players.map { p ->
                    p.copy(score = 0, previousScore = 0, isOpened = false, consecutiveBusts = 0)
                }
                startMatch(rules = current.rules, players = resetPlayers)
            }
        }
    }

    private fun handleRollDice(event: GameUiEvent.RollDice) {
        val currentMatch = _uiState.value.match
        val nextRollNumber = currentMatch.totalRollsInMatch + 1
        val outcome = entropyCollector.captureAndRoll(
            diceCount = currentMatch.rules.diceCount,
            rollCounter = nextRollNumber,
            touchX = event.touchX,
            touchY = event.touchY,
            pressDurationMs = event.pressDurationMs,
            triggeredByShake = event.triggeredByShake
        )
        applyEngineResult(
            FiveThousandEngine.rollDice(
                state = currentMatch,
                rolledFaces = outcome.faces,
                entropySnapshot = outcome.snapshot
            )
        )
    }

    private fun handlePiggybackRoll(event: GameUiEvent.AcceptPiggybackAndRoll) {
        val currentMatch = _uiState.value.match
        val nextRollNumber = currentMatch.totalRollsInMatch + 1
        val outcome = entropyCollector.captureAndRoll(
            diceCount = currentMatch.rules.diceCount,
            rollCounter = nextRollNumber,
            touchX = event.touchX,
            touchY = event.touchY,
            pressDurationMs = event.pressDurationMs,
            triggeredByShake = false
        )
        applyEngineResult(
            FiveThousandEngine.acceptPiggybackAndRoll(
                state = currentMatch,
                rolledFaces = outcome.faces,
                entropySnapshot = outcome.snapshot
            )
        )
    }

    private fun applyEngineResult(result: EngineResult) {
        when (result) {
            is EngineResult.Success -> {
                _uiState.update {
                    it.copy(
                        match = result.state,
                        hasActiveMatch = !result.state.isFinished,
                        errorBannerFr = null
                    )
                }
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

    override fun onCleared() {
        entropyCollector.stopListening()
        super.onCleared()
    }
}
