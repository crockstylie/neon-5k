package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.DecisionRecord

/**
 * Synthèse de l'état d'apprentissage en local de [AdaptiveBotLearner] à partir du dataset SQLite `decision_logs`.
 */
data class AdaptiveLearningSummary(
    val completedSamplesCount: Int = 0,
    val activeStateBucketsCount: Int = 0,
    val averageEmpiricalRollOutcome: Int = 0,
    val learnedConfidencePercent: Int = 0
)

/**
 * Module d'apprentissage par renforcement tabulaire (Contextual Empirical Value / Tabular Q-Learning avec régularisation bayésienne)
 * 100 % Kotlin pur et sans dépendance externe.
 *
 * Il se nourrit automatiquement des vecteurs `(état, action, turnFinalPoints, matchWon)` stockés dans `decision_logs`
 * au fil des parties jouées sur l'appareil (par l'humain et les bots) pour affiner l'espérance de gain réelle
 * de l'action `ROLL_AGAIN` par rapport aux probabilités théoriques à 1 coup de [RollProbabilityTable].
 */
object AdaptiveBotLearner {

    /** Poids du prior théorique (évite le sur-apprentissage tant qu'une cellule a moins de ~10 observations). */
    private const val PRIOR_WEIGHT = 10.0

    private data class StateBucketKey(
        val remainingDice: Int,
        val potBucket: Int,
        val isOpened: Boolean
    )

    private data class BucketAccumulator(
        var weightedSumFinalPoints: Double = 0.0,
        var totalWeight: Double = 0.0,
        var rawCount: Int = 0
    ) {
        val empiricalMeanFinalPoints: Double
            get() = if (totalWeight > 0.0) weightedSumFinalPoints / totalWeight else 0.0
    }

    @Volatile
    private var learnedBuckets: Map<StateBucketKey, BucketAccumulator> = emptyMap()

    @Volatile
    private var currentSummary: AdaptiveLearningSummary = AdaptiveLearningSummary()

    /**
     * Retourne la synthèse actuelle de l'apprentissage local pour l'affichage dans l'onglet Télémétrie / Dataset IA.
     */
    fun getSummary(): AdaptiveLearningSummary = currentSummary

    /**
     * Ré-entraîne instantanément la table de valeur empirique à partir des décisions terminées du dataset local.
     * Complexité $O(N)$ en un seul passage mémoire (< 2 ms pour 10 000 coups).
     */
    fun trainFromRecords(records: List<DecisionRecord>) {
        val completedRollDecisions = records.filter {
            it.decision == "ROLL_AGAIN" && it.turnFinalPoints != null && it.remainingDiceBefore in 1..6
        }
        if (completedRollDecisions.isEmpty()) {
            learnedBuckets = emptyMap()
            currentSummary = AdaptiveLearningSummary()
            return
        }

        val map = mutableMapOf<StateBucketKey, BucketAccumulator>()
        var sumFinal = 0L

        for (rec in completedRollDecisions) {
            val keptCount = rec.keptDiceCsv.split(',').count { it.isNotBlank() }
            val remAfter = (rec.remainingDiceBefore - keptCount).let { if (it <= 0) 5 else it }.coerceIn(1, 6)
            val key = StateBucketKey(
                remainingDice = remAfter,
                potBucket = toPotBucket(rec.turnScoreBefore),
                isOpened = rec.isOpened
            )
            // On pondère davantage les trajectoires issues de parties gagnées (imitation des stratégies gagnantes)
            val weight = when (rec.matchWon) {
                true -> 1.35
                false -> 0.85
                null -> 1.0
            }
            val finalPts = (rec.turnFinalPoints ?: 0).coerceAtLeast(0)
            sumFinal += finalPts
            val acc = map.getOrPut(key) { BucketAccumulator() }
            acc.weightedSumFinalPoints += finalPts * weight
            acc.totalWeight += weight
            acc.rawCount += 1
        }

        learnedBuckets = map
        val avgOutcome = (sumFinal / completedRollDecisions.size).toInt()
        val confidence = ((completedRollDecisions.size.toDouble() / (completedRollDecisions.size + 60.0)) * 100.0)
            .toInt()
            .coerceIn(1, 99)

        currentSummary = AdaptiveLearningSummary(
            completedSamplesCount = completedRollDecisions.size,
            activeStateBucketsCount = map.size,
            averageEmpiricalRollOutcome = avgOutcome,
            learnedConfidencePercent = confidence
        )
    }

    /**
     * Combine par moyenne bayésienne (Empirical Bayes shrinkage) l'utilité théorique à 1 lancer
     * et l'espérance finale réellement observée dans le dataset local pour cet état `(remainingDice, projectedTurnPot, isOpened)`.
     */
    fun blendRollUtility(
        theoreticalRollUtility: Double,
        remainingDiceToRoll: Int,
        projectedTurnPot: Int,
        isOpened: Boolean,
        riskAversion: Double
    ): Double {
        val key = StateBucketKey(
            remainingDice = remainingDiceToRoll.coerceIn(1, 6),
            potBucket = toPotBucket(projectedTurnPot),
            isOpened = isOpened
        )
        val bucket = learnedBuckets[key] ?: return theoreticalRollUtility
        if (bucket.rawCount == 0) return theoreticalRollUtility

        // L'issue empirique moyenne d'un tour à partir de ce palier inclut à la fois les gains multi-lancers et les bust (0 pt)
        val empiricalExpectedPot = bucket.empiricalMeanFinalPoints
        // Ajustement selon l'aversion au risque du profil de bot
        val riskAdjustedEmpirical = empiricalExpectedPot - (projectedTurnPot * 0.12 * (riskAversion - 1.0))

        val alpha = (bucket.totalWeight / (bucket.totalWeight + PRIOR_WEIGHT)).coerceIn(0.0, 0.55)
        return (1.0 - alpha) * theoreticalRollUtility + alpha * riskAdjustedEmpirical
    }

    private fun toPotBucket(turnPot: Int): Int = when {
        turnPot < 200 -> 0
        turnPot < 400 -> 1
        turnPot < 650 -> 2
        turnPot < 1000 -> 3
        turnPot < 1500 -> 4
        else -> 5
    }
}
