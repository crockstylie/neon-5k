package fr.antoinehory.neon5k.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de tests unitaires JVM pour [DiceScorer].
 * Vérifie la détection exhaustive des combinaisons, la règle stricte « zéro dé mort » et toutes les variantes.
 */
class DiceScorerTest {

    private val classic5 = GameRulesConfig.CLASSIC_5_DICE
    private val variant6 = GameRulesConfig.VARIANT_6_DICE

    @Test
    fun `empty selection is invalid and scores 0`() {
        val eval = DiceScorer.evaluateSelection(emptyList(), classic5)
        assertFalse(eval.isValid)
        assertEquals(0, eval.totalPoints)
        assertEquals(0, eval.scoringDiceCount)
    }

    @Test
    fun `single One scores 100 and single Five scores 50`() {
        val evalOne = DiceScorer.evaluateSelection(listOf(1), classic5)
        assertTrue(evalOne.isValid)
        assertEquals(100, evalOne.totalPoints)

        val evalFive = DiceScorer.evaluateSelection(listOf(5), classic5)
        assertTrue(evalFive.isValid)
        assertEquals(50, evalFive.totalPoints)

        val evalCombined = DiceScorer.evaluateSelection(listOf(1, 5, 1), classic5)
        assertTrue(evalCombined.isValid)
        assertEquals(250, evalCombined.totalPoints)
    }

    @Test
    fun `selection with any dead die is strictly invalid`() {
        val evalWithDead = DiceScorer.evaluateSelection(listOf(1, 2), classic5)
        assertFalse(evalWithDead.isValid)
        assertEquals(0, evalWithDead.totalPoints)
        assertEquals(1, evalWithDead.deadDiceCount)

        val evalPairOfTwos = DiceScorer.evaluateSelection(listOf(2, 2), classic5)
        assertFalse(evalPairOfTwos.isValid)
        assertEquals(0, evalPairOfTwos.totalPoints)
        assertEquals(2, evalPairOfTwos.deadDiceCount)
    }

    @Test
    fun `three of a kind scores 1000 for Ones and N x 100 for 2 to 6`() {
        val ones = DiceScorer.evaluateSelection(listOf(1, 1, 1), classic5)
        assertTrue(ones.isValid)
        assertEquals(1000, ones.totalPoints)

        for (face in 2..6) {
            val eval = DiceScorer.evaluateSelection(listOf(face, face, face), classic5)
            assertTrue("Face $face should be valid triple", eval.isValid)
            assertEquals(face * 100, eval.totalPoints)
        }
    }

    @Test
    fun `four of a kind doubles triple in DOUBLE_TRIPLE mode and rejects 4th non-single die when DISABLED`() {
        val fourTwos = DiceScorer.evaluateSelection(listOf(2, 2, 2, 2), classic5)
        assertTrue(fourTwos.isValid)
        assertEquals(400, fourTwos.totalPoints)

        val fourOnes = DiceScorer.evaluateSelection(listOf(1, 1, 1, 1), classic5)
        assertTrue(fourOnes.isValid)
        assertEquals(2000, fourOnes.totalPoints)

        val disabledFourConfig = classic5.copy(fourOfAKindMode = FourOfAKindMode.DISABLED)
        // Avec 4 dés de face 2 et Carré désactivé, le 4e dé '2' est un dé mort -> sélection de 4 dés invalide
        val fourTwosDisabled = DiceScorer.evaluateSelection(listOf(2, 2, 2, 2), disabledFourConfig)
        assertFalse(fourTwosDisabled.isValid)

        // En revanche, avec 4 As et Carré désactivé, cela vaut Brelan d'As (1000) + 1 As (100) = 1100
        val fourOnesDisabled = DiceScorer.evaluateSelection(listOf(1, 1, 1, 1), disabledFourConfig)
        assertTrue(fourOnesDisabled.isValid)
        assertEquals(1100, fourOnesDisabled.totalPoints)
    }

    @Test
    fun `five of a kind quadruples triple in QUAD_TRIPLE and triggers instant win on Ones when configured`() {
        val fiveThrees = DiceScorer.evaluateSelection(listOf(3, 3, 3, 3, 3), classic5)
        assertTrue(fiveThrees.isValid)
        assertEquals(1200, fiveThrees.totalPoints)
        assertFalse(fiveThrees.triggersInstantWin)

        val fiveOnesQuad = DiceScorer.evaluateSelection(listOf(1, 1, 1, 1, 1), classic5)
        assertTrue(fiveOnesQuad.isValid)
        assertEquals(4000, fiveOnesQuad.totalPoints)
        assertFalse(fiveOnesQuad.triggersInstantWin)

        val instantWinConfig = classic5.copy(fiveOfAKindMode = FiveOfAKindMode.INSTANT_WIN_ON_ONES)
        val fiveOnesInstant = DiceScorer.evaluateSelection(listOf(1, 1, 1, 1, 1), instantWinConfig)
        assertTrue(fiveOnesInstant.isValid)
        assertTrue(fiveOnesInstant.triggersInstantWin)
        assertEquals(5000, fiveOnesInstant.totalPoints)
    }

    @Test
    fun `straights 1-5 and 2-6 score straightScore when enabled`() {
        val lowStraight = DiceScorer.evaluateSelection(listOf(3, 1, 4, 2, 5), classic5)
        assertTrue(lowStraight.isValid)
        assertEquals(1500, lowStraight.totalPoints)
        assertEquals(CombinationKind.STRAIGHT, lowStraight.combinations.single().kind)

        val highStraight = DiceScorer.evaluateSelection(listOf(6, 2, 4, 3, 5), classic5)
        assertTrue(highStraight.isValid)
        assertEquals(1500, highStraight.totalPoints)

        // Si la suite est désactivée (straightScore = 0), 1-2-3-4-5 contient 3 dés morts (2, 3, 4)
        val noStraightConfig = classic5.copy(straightScore = 0)
        val disabledStraight = DiceScorer.evaluateSelection(listOf(1, 2, 3, 4, 5), noStraightConfig)
        assertFalse(disabledStraight.isValid)
    }

    @Test
    fun `three pairs in 6-dice mode scores threePairsScore`() {
        val threePairs = DiceScorer.evaluateSelection(listOf(2, 2, 4, 4, 6, 6), variant6)
        assertTrue(threePairs.isValid)
        assertEquals(1000, threePairs.totalPoints)
        assertEquals(CombinationKind.THREE_PAIRS, threePairs.combinations.single().kind)
    }

    @Test
    fun `full house adds bonus to triple when fullHouseBonus is active`() {
        val fullConfig = classic5.copy(fullHouseBonus = 200)
        // Brelan de 4 (400) + Paire de 2 + Bonus Full (200) = 600 pts, et les 5 dés sont scorants
        val eval = DiceScorer.evaluateSelection(listOf(4, 4, 4, 2, 2), fullConfig)
        assertTrue(eval.isValid)
        assertEquals(600, eval.totalPoints)
        assertEquals(CombinationKind.FULL_HOUSE, eval.combinations.single().kind)

        // Si la paire est une paire d'As (200 pts) et que le bonus Full est de 200, les deux valent triple + 200
        val evalWithOnes = DiceScorer.evaluateSelection(listOf(3, 3, 3, 1, 1), fullConfig)
        assertTrue(evalWithOnes.isValid)
        assertEquals(500, evalWithOnes.totalPoints)
    }

    @Test
    fun `findScoringDieIndices detects bust and partial scoring dice accurately`() {
        // Lancer nul : 2, 3, 4, 6, 6
        val bustIndices = DiceScorer.findScoringDieIndices(listOf(2, 3, 4, 6, 6), classic5)
        assertTrue(bustIndices.isEmpty())
        assertFalse(DiceScorer.hasAnyScoringCombination(listOf(2, 3, 4, 6, 6), classic5))

        // Lancer partiel : 1, 2, 3, 3, 3 -> indices 0, 2, 3, 4 scorent (seul l'index 1 avec face '2' est mort)
        val partialIndices = DiceScorer.findScoringDieIndices(listOf(1, 2, 3, 3, 3), classic5)
        assertEquals(setOf(0, 2, 3, 4), partialIndices)
    }
}
