package fr.antoinehory.neon5k.domain.model

import fr.antoinehory.neon5k.domain.rules.ScoringCombination

/**
 * État d'un dé individuel sur le plateau.
 *
 * @property index Position du dé sur le tapis (`0 until diceCount`).
 * @property face Valeur de la face (`1..6`), ou `0` avant le tout premier lancer du tour.
 * @property isLocked Vrai si ce dé a déjà été validé lors d'un lancer précédent de ce tour.
 * @property isSelected Vrai si le joueur a sélectionné ce dé sur le lancer courant.
 * @property canScoreOnCurrentRoll Vrai si ce dé (non verrouillé) peut faire partie d'au moins une combinaison scorante sur le lancer courant.
 */
data class DieState(
    val index: Int,
    val face: Int,
    val isLocked: Boolean = false,
    val isSelected: Boolean = false,
    val canScoreOnCurrentRoll: Boolean = false
)

/**
 * Phase précise du tour actif dans la machine à états du 5000.
 */
enum class TurnPhase {
    /** Début du tour : le joueur doit lancer tous les dés (ou choisir de repiquer le pot précédent si disponible). */
    AWAITING_INITIAL_ROLL,
    /** Les dés viennent d'être lancés et au moins une combinaison score : le joueur sélectionne ses dés puis relance ou banque. */
    SELECTING_DICE,
    /** Tous les dés ont scoré (Main pleine) et `mandatoryRerollOnHotDice == true` : le joueur doit obligatoirement relancer tous les dés. */
    HOT_DICE_MANDATORY_REROLL,
    /** Aucun dé du lancer ne score (Coup nul / Glitch) : le joueur perd le cumul du tour et doit accuser réception pour passer au joueur suivant. */
    BUSTED,
    /** Le score total dépasserait la cible avec `exactFinishRequired == true` : le tour est annulé et passe au joueur suivant. */
    OVERFLOW_BUSTED
}

/**
 * Offre de repiquage (variante `piggybackEnabled`) transmise par le joueur précédent lorsqu'il banque sans avoir utilisé tous ses dés.
 */
data class PiggybackOffer(
    val inheritedPoints: Int,
    val remainingDiceCount: Int,
    val fromPlayerName: String
)

/**
 * État immuable complet du tour en cours.
 *
 * @property turnNumber Numéro séquentiel du tour dans la partie (1, 2, 3...).
 * @property activePlayerIndex Index du joueur actif dans `MatchState.players`.
 * @property phase Phase courante du tour.
 * @property dice Liste des `N` dés sur le plateau.
 * @property bankedInTurn Points déjà verrouillés lors des lancers précédents de ce tour.
 * @property selectedPoints Points rapportés par les dés actuellement sélectionnés (`isSelected == true`).
 * @property selectedCombinations Combinaisons formées par la sélection courante.
 * @property isSelectionValid Vrai si la sélection courante est non vide et sans aucun dé mort.
 * @property rollIndexInTurn Nombre de lancers effectués dans ce tour.
 * @property hotDiceCountInTurn Nombre de « Mains pleines » réalisées au cours de ce tour.
 * @property piggybackOffer Offre de repiquage éventuelle disponible au tout début du tour.
 * @property usedPiggyback Vrai si le joueur actif a choisi de repiquer le pot au début de ce tour.
 * @property lastEntropySnapshot Trace d'entropie du dernier lancer effectué.
 * @property statusMessageFr Message synthétique décrivant l'état ou l'événement du tour.
 */
data class TurnState(
    val turnNumber: Int = 1,
    val activePlayerIndex: Int = 0,
    val phase: TurnPhase = TurnPhase.AWAITING_INITIAL_ROLL,
    val dice: List<DieState> = emptyList(),
    val bankedInTurn: Int = 0,
    val selectedPoints: Int = 0,
    val selectedCombinations: List<ScoringCombination> = emptyList(),
    val isSelectionValid: Boolean = false,
    val rollIndexInTurn: Int = 0,
    val hotDiceCountInTurn: Int = 0,
    val piggybackOffer: PiggybackOffer? = null,
    val usedPiggyback: Boolean = false,
    val lastEntropySnapshot: EntropySnapshot? = null,
    val statusMessageFr: String = ""
) {
    /** Cumul virtuel du tour si la sélection courante est valide (`bankedInTurn + selectedPoints`). */
    val totalTurnPointsWithSelection: Int
        get() = bankedInTurn + if (isSelectionValid) selectedPoints else 0

    /** Nombre de dés qui resteraient à lancer après verrouillage de la sélection courante. */
    val remainingDiceAfterSelection: Int
        get() = dice.count { !it.isLocked && !it.isSelected }

    /** Vrai si la sélection courante utilise tous les dés restants (déclenche une Main pleine). */
    val selectionTriggersHotDice: Boolean
        get() = isSelectionValid && remainingDiceAfterSelection == 0
}
