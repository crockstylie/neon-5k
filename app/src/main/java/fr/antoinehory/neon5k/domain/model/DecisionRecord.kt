package fr.antoinehory.neon5k.domain.model

/**
 * Vecteur pur d'état/action/issue représentant une décision de jeu pour l'entraînement futur d'IA (utilisé dès le Lot 2).
 */
data class DecisionRecord(
    val matchUuid: String,
    val rulesFingerprint: String,
    val turnIndex: Int,
    val stepIndex: Int,
    val actorType: String,
    val myScore: Int,
    val bestOpponentScore: Int,
    val turnScoreBefore: Int,
    val remainingDiceBefore: Int,
    val isOpened: Boolean,
    val consecutiveBusts: Int,
    val rolledDiceCsv: String,
    val keptDiceCsv: String,
    val decision: String,
    val turnFinalPoints: Int? = null,
    val matchWon: Boolean? = null
)
