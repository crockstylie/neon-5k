package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Suite de tests unitaires JVM pour [RollProbabilityTable] et [HeuristicBotPolicy].
 */
class HeuristicBotPolicyTest {

    private val classic5 = GameRulesConfig.CLASSIC_5_DICE
    private val variant6 = GameRulesConfig.VARIANT_6_DICE

    private fun EngineResult.requireSuccess() = (this as EngineResult.Success).state

    @Test
    fun `RollProbabilityTable computes exact bust probabilities for 1 to 5 dice`() {
        val odds1 = RollProbabilityTable.getOdds(1, classic5)
        assertEquals(4.0 / 6.0, odds1.bustProbability, 0.0001)
        assertEquals(150.0 / 6.0, odds1.expectedMaxGain, 0.0001)

        val odds2 = RollProbabilityTable.getOdds(2, classic5)
        assertEquals(16.0 / 36.0, odds2.bustProbability, 0.0001)

        val odds3 = RollProbabilityTable.getOdds(3, classic5)
        val odds4 = RollProbabilityTable.getOdds(4, classic5)
        val odds5 = RollProbabilityTable.getOdds(5, classic5)

        assertTrue(odds3.bustProbability < odds2.bustProbability)
        assertTrue(odds4.bustProbability < odds3.bustProbability)
        assertTrue(odds5.bustProbability < odds4.bustProbability)
        assertEquals(600.0 / 7776.0, odds5.bustProbability, 0.0001)
    }

    @Test
    fun `Bot banks immediately when selection reaches exact winning targetScore`() {
        val players = listOf(
            PlayerState(id = 0, name = "Bot-Safe", type = PlayerType.BOT_SAFE, score = 4850, isOpened = true),
            PlayerState(id = 1, name = "Human", type = PlayerType.HUMAN, score = 4000, isOpened = true)
        )
        var state = FiveThousandEngine.startNewMatch("bot-win", classic5, players)
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
        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 2, 2, 3)).requireSuccess()

        val safePlan = HeuristicBotPolicy.planMove(state, BotProfile.SAFE_RUNNER)
        assertEquals(BotTurnAction.BANK, safePlan.nextAction)
        assertEquals(listOf(0, 1, 2, 3), safePlan.indicesToSelect.sorted())

        var stateKeepOne = FiveThousandEngine.startNewMatch("bot-keep", classic5, players)
        stateKeepOne = FiveThousandEngine.rollDice(stateKeepOne, listOf(1, 5, 2, 2, 4)).requireSuccess()
        val kamikazePlan = HeuristicBotPolicy.planMove(stateKeepOne, BotProfile.OVERCLOCK_KAMIKAZE)
        assertEquals(BotTurnAction.ROLL_AGAIN, kamikazePlan.nextAction)
        assertEquals(listOf(0), kamikazePlan.indicesToSelect)
    }

    @Test
    fun `Bots never skip Aces or Brelans and handle 5-die straights in 6-dice mode without rejection`() {
        val players = listOf(
            PlayerState(id = 0, name = "Cyber-Bot", type = PlayerType.BOT_BALANCED, score = 1000, isOpened = true)
        )

        // 1. Deux As [1, 1, 2, 3, 4] -> le bot doit toujours prendre les 2 As (indices 0 et 1)
        var stateTwoAces = FiveThousandEngine.startNewMatch("two-aces", classic5, players)
        stateTwoAces = FiveThousandEngine.rollDice(stateTwoAces, listOf(1, 1, 2, 3, 4)).requireSuccess()
        for (profile in BotProfile.entries) {
            val plan = HeuristicBotPolicy.planMove(stateTwoAces, profile)
            assertEquals(listOf(0, 1), plan.indicesToSelect.sorted())
        }

        // 2. Brelan de 2 + un 5 [2, 2, 2, 5, 4] -> CYBER_BALANCED et SAFE_RUNNER prennent tout [0, 1, 2, 3]
        var stateTripleAndFive = FiveThousandEngine.startNewMatch("triple-five", classic5, players)
        stateTripleAndFive = FiveThousandEngine.rollDice(stateTripleAndFive, listOf(2, 2, 2, 5, 4)).requireSuccess()
        assertEquals(
            listOf(0, 1, 2, 3),
            HeuristicBotPolicy.planMove(stateTripleAndFive, BotProfile.CYBER_BALANCED).indicesToSelect.sorted()
        )

        // 3. Suite de 5 dés en mode 6 dés avec doublon [1, 2, 3, 4, 5, 2] -> sélection 100% acceptée par FiveThousandEngine
        var stateSixStraight = FiveThousandEngine.startNewMatch("six-straight", variant6, players)
        stateSixStraight = FiveThousandEngine.rollDice(stateSixStraight, listOf(1, 2, 3, 4, 5, 2)).requireSuccess()
        val plan6 = HeuristicBotPolicy.planMove(stateSixStraight, BotProfile.CYBER_BALANCED)
        for (idx in plan6.indicesToSelect) {
            stateSixStraight = FiveThousandEngine.toggleDieSelection(stateSixStraight, idx).requireSuccess()
        }
        assertTrue(stateSixStraight.turn.isSelectionValid)
        assertEquals(1500, stateSixStraight.turn.selectedPoints)
    }

    @Test
    fun `Full 5-bot matches in 5-dice and 6-dice modes run to completion without any rejected move`() {
        val rng = Random(42L)
        val fiveBots = listOf(
            PlayerState(id = 0, name = "Bot-1", type = PlayerType.BOT_SAFE),
            PlayerState(id = 1, name = "Bot-2", type = PlayerType.BOT_BALANCED),
            PlayerState(id = 2, name = "Bot-3", type = PlayerType.BOT_KAMIKAZE),
            PlayerState(id = 3, name = "Bot-4", type = PlayerType.BOT_BALANCED),
            PlayerState(id = 4, name = "Bot-5", type = PlayerType.BOT_SAFE)
        )

        for (rules in listOf(classic5, variant6)) {
            var state = FiveThousandEngine.startNewMatch("sim-${rules.diceCount}", rules, fiveBots)
            var safetySteps = 0
            while (!state.isFinished && safetySteps < 5000) {
                safetySteps++
                val profile = BotProfile.fromPlayerType(state.activePlayer.type)!!
                when (state.turn.phase) {
                    TurnPhase.AWAITING_INITIAL_ROLL, TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                        val faces = List(rules.diceCount) { rng.nextInt(1, 7) }
                        state = FiveThousandEngine.rollDice(state, faces).requireSuccess()
                    }
                    TurnPhase.SELECTING_DICE -> {
                        val plan = HeuristicBotPolicy.planMove(state, profile)
                        for (idx in plan.indicesToSelect) {
                            state = FiveThousandEngine.toggleDieSelection(state, idx).requireSuccess()
                        }
                        assertTrue("Selection must always be valid", state.turn.isSelectionValid)
                        state = when (plan.nextAction) {
                            BotTurnAction.ROLL_AGAIN -> {
                                val faces = List(rules.diceCount) { rng.nextInt(1, 7) }
                                FiveThousandEngine.rollDice(state, faces).requireSuccess()
                            }
                            BotTurnAction.BANK -> FiveThousandEngine.bankTurn(state).requireSuccess()
                        }
                    }
                    TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
                    }
                }
            }
            assertTrue("5-bot simulation for ${rules.diceCount} dice should finish cleanly", state.isFinished)
        }
    }

    @Test
    fun `shouldAcceptPiggyback respects opening state and risk profile`() {
        val piggyRules = classic5.copy(piggybackEnabled = true)
        val p1 = PlayerState(id = 0, name = "P1", score = 1000, isOpened = true)
        val p2 = PlayerState(id = 1, name = "Bot", score = 1000, isOpened = true)
        var state = FiveThousandEngine.startNewMatch("bot-piggy", piggyRules, listOf(p1, p2))

        state = FiveThousandEngine.rollDice(state, listOf(1, 2, 3, 4, 6)).requireSuccess()
        state = FiveThousandEngine.toggleDieSelection(state, 0).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        assertFalse(HeuristicBotPolicy.shouldAcceptPiggyback(state, BotProfile.SAFE_RUNNER))
        assertTrue(HeuristicBotPolicy.shouldAcceptPiggyback(state, BotProfile.OVERCLOCK_KAMIKAZE))
    }
}
