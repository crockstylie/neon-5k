package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de tests unitaires JVM pour [RollProbabilityTable] et [HeuristicBotPolicy].
 */
class HeuristicBotPolicyTest {

    private val classic5 = GameRulesConfig.CLASSIC_5_DICE

    private fun EngineResult.requireSuccess() = (this as EngineResult.Success).state

    @Test
    fun `RollProbabilityTable computes exact bust probabilities for 1 to 5 dice`() {
        // Pour 1 dé : seuls 1 et 5 scorent -> 2 faces sur 6 scorent -> P(bust) = 4/6 = 2/3 ≈ 0.6667
        val odds1 = RollProbabilityTable.getOdds(1, classic5)
        assertEquals(4.0 / 6.0, odds1.bustProbability, 0.0001)
        assertEquals(150.0 / 6.0, odds1.expectedMaxGain, 0.0001)

        // Pour 2 dés : P(bust) = (4/6)^2 = 16/36 = 4/9 ≈ 0.4444
        val odds2 = RollProbabilityTable.getOdds(2, classic5)
        assertEquals(16.0 / 36.0, odds2.bustProbability, 0.0001)

        // Plus on a de dés à lancer, plus la probabilité de coup nul diminue strictement
        val odds3 = RollProbabilityTable.getOdds(3, classic5)
        val odds4 = RollProbabilityTable.getOdds(4, classic5)
        val odds5 = RollProbabilityTable.getOdds(5, classic5)

        assertTrue(odds3.bustProbability < odds2.bustProbability)
        assertTrue(odds4.bustProbability < odds3.bustProbability)
        assertTrue(odds5.bustProbability < odds4.bustProbability)
        // Pour 5 dés avec Suite active : exactement 600 tirages nuls sur 7776 (P(bust) = 600/7776 ≈ 7.716 %)
        assertEquals(600.0 / 7776.0, odds5.bustProbability, 0.0001)
    }

    @Test
    fun `Bot banks immediately when selection reaches exact winning targetScore`() {
        val players = listOf(
            PlayerState(id = 0, name = "Bot-Safe", type = PlayerType.BOT_SAFE, score = 4850, isOpened = true),
            PlayerState(id = 1, name = "Human", type = PlayerType.HUMAN, score = 4000, isOpened = true)
        )
        var state = FiveThousandEngine.startNewMatch("bot-win", classic5, players)
        // Lancer : un As (100), un 5 (50) et trois dés morts (2, 2, 4) -> 4850 + 150 = 5000 exact !
        state = FiveThousandEngine.rollDice(state, listOf(1, 5, 2, 2, 4)).requireSuccess()

        for (profile in BotProfile.entries) {
            val plan = HeuristicBotPolicy.planMove(state, profile)
            assertEquals("Profile $profile should bank winning points", BotTurnAction.BANK, plan.nextAction)
            assertEquals(listOf(0, 1), plan.indicesToSelect.sorted())
        }
    }

    @Test
    fun `Bot must roll again when not opened and turn pot is below openingThreshold`() {
        val players = listOf(
            PlayerState(id = 0, name = "Bot-Safe", type = PlayerType.BOT_SAFE, score = 0, isOpened = false)
        )
        var state = FiveThousandEngine.startNewMatch("bot-open", classic5, players)
        // Lancer : Brelan de 4 (400 pts) + deux dés morts (2, 3) -> < 750 (seuil d'ouverture)
        state = FiveThousandEngine.rollDice(state, listOf(4, 4, 4, 2, 3)).requireSuccess()

        val plan = HeuristicBotPolicy.planMove(state, BotProfile.SAFE_RUNNER)
        assertEquals(BotTurnAction.ROLL_AGAIN, plan.nextAction)
        assertEquals(listOf(0, 1, 2), plan.indicesToSelect.sorted())
    }

    @Test
    fun `SAFE_RUNNER banks when opened with 1 die left while OVERCLOCK_KAMIKAZE takes more risks`() {
        val players = listOf(
            PlayerState(id = 0, name = "Bot", type = PlayerType.BOT_SAFE, score = 1500, isOpened = true)
        )
        var state = FiveThousandEngine.startNewMatch("bot-risk", classic5, players)
        // Lancer : Carré de 2 (400 pts) + un dé mort (3) -> il ne reste qu'1 seul dé (P(bust) = 66.7%)
        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 2, 2, 3)).requireSuccess()

        val safePlan = HeuristicBotPolicy.planMove(state, BotProfile.SAFE_RUNNER)
        assertEquals(BotTurnAction.BANK, safePlan.nextAction)
        assertEquals(listOf(0, 1, 2, 3), safePlan.indicesToSelect.sorted())

        // Sur un lancer de 1 As (100) + 1 Cinq (50) + 3 dés morts (2, 2, 4) : un bot qui veut relancer
        // préférera garder uniquement l'As (100 pts, 4 dés à relancer) plutôt que As+5 (150 pts, 3 dés à relancer)
        var stateKeepOne = FiveThousandEngine.startNewMatch("bot-keep", classic5, players)
        stateKeepOne = FiveThousandEngine.rollDice(stateKeepOne, listOf(1, 5, 2, 2, 4)).requireSuccess()
        val kamikazePlan = HeuristicBotPolicy.planMove(stateKeepOne, BotProfile.OVERCLOCK_KAMIKAZE)
        assertEquals(BotTurnAction.ROLL_AGAIN, kamikazePlan.nextAction)
        assertEquals(listOf(0), kamikazePlan.indicesToSelect)
    }

    @Test
    fun `shouldAcceptPiggyback respects opening state and risk profile`() {
        val piggyRules = classic5.copy(piggybackEnabled = true)
        val p1 = PlayerState(id = 0, name = "P1", score = 1000, isOpened = true)
        val p2 = PlayerState(id = 1, name = "Bot", score = 1000, isOpened = true)
        var state = FiveThousandEngine.startNewMatch("bot-piggy", piggyRules, listOf(p1, p2))

        // P1 marque 100 pts avec 1 As et banque en laissant 4 dés libres
        state = FiveThousandEngine.rollDice(state, listOf(1, 2, 3, 4, 6)).requireSuccess()
        state = FiveThousandEngine.toggleDieSelection(state, 0).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        // Pot = 100 pts, 4 dés restants : SAFE_RUNNER refuse (< 400 pts), KAMIKAZE accepte (>= 2 dés)
        assertFalse(HeuristicBotPolicy.shouldAcceptPiggyback(state, BotProfile.SAFE_RUNNER))
        assertTrue(HeuristicBotPolicy.shouldAcceptPiggyback(state, BotProfile.OVERCLOCK_KAMIKAZE))
    }
}
