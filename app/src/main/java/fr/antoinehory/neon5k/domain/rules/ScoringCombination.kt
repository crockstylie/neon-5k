package fr.antoinehory.neon5k.domain.rules

/**
 * Type de combinaison scorante détectée sur un lancer unique.
 */
enum class CombinationKind {
    SINGLE_ONE,
    SINGLE_FIVE,
    THREE_OF_A_KIND,
    FOUR_OF_A_KIND,
    FIVE_OF_A_KIND,
    SIX_OF_A_KIND,
    STRAIGHT,
    THREE_PAIRS,
    FULL_HOUSE
}

/**
 * Représente une combinaison scorante élémentaire issue d'un lancer de dés.
 *
 * @property kind Nature de la combinaison.
 * @property diceFaces Valeurs des dés utilisés par cette combinaison.
 * @property points Points rapportés par cette combinaison.
 * @property labelFr Libellé lisible pour l'interface (ex: "Brelan d'As", "Suite", "As").
 * @property triggersInstantWin Vrai si cette combinaison déclenche la victoire immédiate (ex: 5 As avec INSTANT_WIN_ON_ONES).
 */
data class ScoringCombination(
    val kind: CombinationKind,
    val diceFaces: List<Int>,
    val points: Int,
    val labelFr: String,
    val triggersInstantWin: Boolean = false
)

/**
 * Résultat de l'évaluation d'un sous-ensemble de dés sélectionnés par le joueur.
 *
 * @property isValid Vrai si et seulement si le sous-ensemble est non vide et que **chaque** dé sélectionné participe à une combinaison scorante.
 * @property totalPoints Somme des points des combinaisons formées par les dés sélectionnés (0 si invalide).
 * @property combinations Liste des combinaisons scorantes formées par la sélection.
 * @property scoringDiceCount Nombre de dés qui scorent dans le sous-ensemble.
 * @property deadDiceCount Nombre de dés « morts » (non scorants) présents dans le sous-ensemble.
 * @property triggersInstantWin Vrai si la sélection contient une quinte d'As victorieuse.
 */
data class SelectionEvaluation(
    val isValid: Boolean,
    val totalPoints: Int,
    val combinations: List<ScoringCombination>,
    val scoringDiceCount: Int,
    val deadDiceCount: Int,
    val triggersInstantWin: Boolean = false
)
