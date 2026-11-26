package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import kotlin.math.pow

/**
 * Statistiques exactes d'un lancer de `remainingDice` dés (`1..6`) pour une [GameRulesConfig] donnée.
 *
 * @property remainingDice Nombre de dés lancés (`1..6`).
 * @property bustProbability Probabilité exacte (`0.0..1.0`) de faire un coup nul (aucun dé scorant).
 * @property expectedMaxGain Espérance mathématique du gain maximal sur ce lancer (en supposant qu'on prenne les meilleurs dés scorants).
 */
data class RollOdds(
    val remainingDice: Int,
    val bustProbability: Double,
    val expectedMaxGain: Double
) {
    val scoringProbability: Double
        get() = (1.0 - bustProbability).coerceIn(0.0, 1.0)
}

/**
 * Calculateur exact et cache des probabilités de coup nul et d'espérance de gain au 5000
 * par énumération exhaustive de tous les tirages possibles ($6^N$, de $6^1 = 6$ à $6^6 = 46\,656$ tirages).
 * Pur Kotlin (zéro dépendance Android).
 */
object RollProbabilityTable {

    private val cache = mutableMapOf<String, Map<Int, RollOdds>>()

    /**
     * Retourne les probabilités exactes pour un lancer de [remainingDice] dés (`1..6`) selon [config].
     */
    fun getOdds(
        remainingDice: Int,
        config: GameRulesConfig
    ): RollOdds {
        val clampedDice = remainingDice.coerceIn(1, config.diceCount)
        val table = getTableForConfig(config)
        return table.getValue(clampedDice)
    }

    /**
     * Retourne la table complète (`1..config.diceCount`) pour la configuration de règles fournie.
     */
    @Synchronized
    fun getTableForConfig(config: GameRulesConfig): Map<Int, RollOdds> {
        val key = config.fingerprint()
        cache[key]?.let { return it }

        val computed = (1..config.diceCount).associateWith { diceCount ->
            computeExactOdds(diceCount, config)
        }
        cache[key] = computed
        return computed
    }

    private fun computeExactOdds(
        diceCount: Int,
        config: GameRulesConfig
    ): RollOdds {
        val totalOutcomes = 6.0.pow(diceCount.toDouble()).toInt()
        val currentRoll = IntArray(diceCount)
        var bustCount = 0
        var totalMaxPoints = 0L

        fun enumerate(depth: Int) {
            if (depth == diceCount) {
                val faces = currentRoll.toList()
                val eval = DiceScorer.evaluateMaxScoringSubset(faces, config)
                if (!eval.isValid || eval.totalPoints == 0) {
                    bustCount++
                } else {
                    totalMaxPoints += eval.totalPoints
                }
                return
            }
            for (face in 1..6) {
                currentRoll[depth] = face
                enumerate(depth + 1)
            }
        }

        enumerate(0)

        return RollOdds(
            remainingDice = diceCount,
            bustProbability = bustCount.toDouble() / totalOutcomes.toDouble(),
            expectedMaxGain = totalMaxPoints.toDouble() / totalOutcomes.toDouble()
        )
    }
}
