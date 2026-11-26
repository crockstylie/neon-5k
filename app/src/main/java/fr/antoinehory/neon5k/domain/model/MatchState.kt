package fr.antoinehory.neon5k.domain.model

import fr.antoinehory.neon5k.domain.rules.GameRulesConfig

/**
 * Erreurs métier explicites lors d'une action invalide sur `FiveThousandEngine`.
 */
sealed interface MoveValidationError {
    data object MatchAlreadyFinished : MoveValidationError
    data object InvalidPhaseForAction : MoveValidationError
    data object PiggybackNotAvailable : MoveValidationError
    data object MustBeOpenedToPiggyback : MoveValidationError
    data object DieNotSelectable : MoveValidationError
    data object SelectionContainsDeadDiceOrEmpty : MoveValidationError
    data object MandatoryHotDiceRerollRequired : MoveValidationError
    data class OpeningThresholdNotMet(val required: Int, val current: Int) : MoveValidationError
    data class TargetOverflowNotAllowed(val target: Int, val projected: Int) : MoveValidationError
}

/**
 * Résultat typé d'une transition du moteur `FiveThousandEngine`.
 */
sealed interface EngineResult {
    data class Success(val state: MatchState) : EngineResult
    data class Rejected(val state: MatchState, val error: MoveValidationError) : EngineResult
}

/**
 * État immuable complet d'une partie de Neon 5K.
 *
 * @property matchId Identifiant unique de la partie.
 * @property rules Configuration complète des règles et variantes actives.
 * @property players Liste ordonnée des joueurs.
 * @property turn État du tour courant.
 * @property winnerPlayerId Identifiant du joueur vainqueur (`null` tant que la partie est en cours).
 * @property isInstantWin Vrai si la victoire a été obtenue par 5 As instantanés.
 * @property totalRollsInMatch Compteur global de lancers depuis le début de la partie (sert au compteur d'entropie).
 */
data class MatchState(
    val matchId: String,
    val rules: GameRulesConfig,
    val players: List<PlayerState>,
    val turn: TurnState,
    val winnerPlayerId: Int? = null,
    val isInstantWin: Boolean = false,
    val totalRollsInMatch: Int = 0
) {
    val isFinished: Boolean
        get() = winnerPlayerId != null

    val activePlayer: PlayerState
        get() = players[turn.activePlayerIndex]

    val winnerPlayer: PlayerState?
        get() = winnerPlayerId?.let { id -> players.firstOrNull { it.id == id } }
}
