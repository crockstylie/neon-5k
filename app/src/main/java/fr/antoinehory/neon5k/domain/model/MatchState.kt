package fr.antoinehory.neon5k.domain.model

import fr.antoinehory.neon5k.domain.rules.GameRulesConfig

/**
 * Entrée du journal des lancers et actions d'une partie de 5000.
 * Permet d'afficher en temps réel au milieu de l'écran de jeu (ou en modale) l'historique des tirages,
 * les points gagnés, les banques, les coups nuls et le nombre d'encoches (`✕`) sur le palier du joueur.
 */
data class RollHistoryEntry(
    val turnNumber: Int,
    val rollNumber: Int,
    val playerId: Int,
    val playerName: String,
    val rolledFaces: List<Int>,
    val summaryFr: String,
    val playerScoreAfter: Int,
    val playerStrikesAfter: Int,
    val isBust: Boolean = false,
    val isBank: Boolean = false,
    val isHotDice: Boolean = false
)

/**
 * État immuable complet d'une partie de 5000 en cours ou terminée.
 */
data class MatchState(
    val matchId: String,
    val rules: GameRulesConfig,
    val players: List<PlayerState>,
    val turn: TurnState,
    val winnerPlayerId: Int? = null,
    val isInstantWin: Boolean = false,
    val totalRollsInMatch: Int = 0,
    val rollHistoryLog: List<RollHistoryEntry> = emptyList()
) {
    /** Joueur dont c'est actuellement le tour. */
    val activePlayer: PlayerState
        get() = players[turn.activePlayerIndex]

    /** Vrai si la partie possède un vainqueur. */
    val isFinished: Boolean
        get() = winnerPlayerId != null

    /** Joueur vainqueur le cas échéant. */
    val winnerPlayer: PlayerState?
        get() = winnerPlayerId?.let { id -> players.firstOrNull { it.id == id } }
}

/**
 * Résultat typé d'une transition demandée à [fr.antoinehory.neon5k.domain.rules.FiveThousandEngine].
 */
sealed interface EngineResult {
    data class Success(val state: MatchState) : EngineResult
    data class Rejected(
        val state: MatchState,
        val error: MoveValidationError
    ) : EngineResult
}

/**
 * Motif précis de rejet d'une action illégale par la machine à états.
 */
sealed interface MoveValidationError {
    data object MatchAlreadyFinished : MoveValidationError
    data object InvalidPhaseForAction : MoveValidationError
    data object DieNotSelectable : MoveValidationError
    data object SelectionContainsDeadDiceOrEmpty : MoveValidationError
    data object MandatoryHotDiceRerollRequired : MoveValidationError
    data object PiggybackNotAvailable : MoveValidationError
    data object MustBeOpenedToPiggyback : MoveValidationError

    data class OpeningThresholdNotMet(
        val required: Int,
        val current: Int
    ) : MoveValidationError

    data class MinTurnScoreAfterOpeningNotMet(
        val required: Int,
        val current: Int
    ) : MoveValidationError

    data class RoundMultipleOf100Required(
        val currentTurnPot: Int
    ) : MoveValidationError

    data class FinalSprint1000Required(
        val targetScore: Int,
        val projectedScore: Int
    ) : MoveValidationError

    data class TargetOverflowNotAllowed(
        val target: Int,
        val projected: Int
    ) : MoveValidationError
}
