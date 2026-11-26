package fr.antoinehory.neon5k.domain.rules

import fr.antoinehory.neon5k.domain.model.EngineResult
import fr.antoinehory.neon5k.domain.model.MoveValidationError
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de tests unitaires JVM pour [FiveThousandEngine].
 * Couvre le cycle de vie complet d'une partie et toutes les variantes configurables.
 */
class FiveThousandEngineTest {

    private fun twoPlayers(
        p1Score: Int = 0,
        p1Opened: Boolean = false,
        p2Score: Int = 0,
        p2Opened: Boolean = false
    ): List<PlayerState> = listOf(
        PlayerState(id = 0, name = "Cyber-1", score = p1Score, previousScore = 0, isOpened = p1Opened),
        PlayerState(id = 1, name = "Neon-2", score = p2Score, previousScore = 0, isOpened = p2Opened)
    )

    private fun EngineResult.requireSuccess() = (this as EngineResult.Success).state

    @Test
    fun `opening threshold blocks banking below 750 and unlocks at 750`() {
        var state = FiveThousandEngine.startNewMatch("m1", GameRulesConfig.CLASSIC_5_DICE, twoPlayers())

        // Lancer : Brelan de 6 (600) + deux dés morts (2, 3)
        state = FiveThousandEngine.rollDice(state, listOf(6, 6, 6, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        assertEquals(600, state.turn.selectedPoints)

        // Tenter de banquer à 600 alors que openingThreshold == 750 -> Rejeté
        val rejected = FiveThousandEngine.bankTurn(state)
        assertTrue(rejected is EngineResult.Rejected)
        val err = (rejected as EngineResult.Rejected).error
        assertTrue(err is MoveValidationError.OpeningThresholdNotMet)

        // Relancer les 2 dés restants -> un As (100) et un 5 (50) => total tour 750
        state = FiveThousandEngine.rollDice(state, listOf(1, 5)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        assertEquals(750, state.turn.totalTurnPointsWithSelection)

        // Comme les 5 dés ont scoré (Main pleine) et mandatoryRerollOnHotDice == true, il faut relancer !
        val hotDiceRejected = FiveThousandEngine.bankTurn(state)
        assertTrue(hotDiceRejected is EngineResult.Rejected)
        assertEquals(
            MoveValidationError.MandatoryHotDiceRerollRequired,
            (hotDiceRejected as EngineResult.Rejected).error
        )

        // Relance des 5 dés après Main pleine : un As (100) et 4 dés morts (2, 3, 4, 6) => total tour 850, 4 dés libres
        state = FiveThousandEngine.rollDice(state, listOf(1, 2, 3, 4, 6)).requireSuccess()
        state = FiveThousandEngine.toggleDieSelection(state, 0).requireSuccess()
        assertEquals(850, state.turn.totalTurnPointsWithSelection)

        // Banquer 850 pts -> Succès, Joueur 1 est ouvert à 850 pts et on passe au Joueur 2
        state = FiveThousandEngine.bankTurn(state).requireSuccess()
        assertEquals(850, state.players[0].score)
        assertTrue(state.players[0].isOpened)
        assertEquals(1, state.turn.activePlayerIndex)
    }

    @Test
    fun `bust loses turn pot and increments consecutiveBusts`() {
        var state = FiveThousandEngine.startNewMatch("m2", GameRulesConfig.CLASSIC_5_DICE, twoPlayers(p1Score = 1000, p1Opened = true))

        // Premier lancer : un As (100) + 4 dés morts
        state = FiveThousandEngine.rollDice(state, listOf(1, 2, 3, 4, 6)).requireSuccess()
        state = FiveThousandEngine.toggleDieSelection(state, 0).requireSuccess()

        // Deuxième lancer (4 dés) : 2, 3, 4, 6 -> Coup nul !
        state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6)).requireSuccess()
        assertEquals(TurnPhase.BUSTED, state.turn.phase)

        // Accuser réception du bust -> score inchangé (1000), consecutiveBusts = 1, tour suivant
        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        assertEquals(1000, state.players[0].score)
        assertEquals(1, state.players[0].consecutiveBusts)
        assertEquals(1, state.turn.activePlayerIndex)
    }

    @Test
    fun `three strikes penalty deducts points after 3 consecutive busts`() {
        val rules = GameRulesConfig.CLASSIC_5_DICE.copy(threeStrikesMode = ThreeStrikesMode.MINUS_500)
        val p1 = PlayerState(id = 0, name = "Cyber-1", score = 1200, isOpened = true, consecutiveBusts = 2)
        val p2 = PlayerState(id = 1, name = "Neon-2", score = 800, isOpened = true)
        var state = FiveThousandEngine.startNewMatch("m3", rules, listOf(p1, p2))

        // 3e coup nul consécutif pour Cyber-1
        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 3, 4, 6)).requireSuccess()
        assertEquals(TurnPhase.BUSTED, state.turn.phase)

        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        assertEquals(700, state.players[0].score)
        assertEquals(0, state.players[0].consecutiveBusts)
    }

    @Test
    fun `three strikes REVERT_TO_PREVIOUS_PERSIST_STRIKES rolls back to previous checkpoint and preserves saved strikes`() {
        val rules = GameRulesConfig.CLASSIC_5_DICE.copy(
            threeStrikesMode = ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES
        )
        // 1 joueur pour enchaîner les tours directement selon le scénario exact :
        // 1000 -> +200 => 1200 -> 2 busts (2 encoches sur 1200) -> +500 => 1700 -> 3 busts => redescend à 1200 avec 2 encoches -> 1 bust => 3e encoche sur 1200 => redescend à 1000 !
        var state = FiveThousandEngine.startNewMatch(
            "m3_persist",
            rules,
            listOf(PlayerState(id = 0, name = "Solo"))
        )

        // 1) Banquer 1000 pts (Brelan d'As = 1000) -> Palier #1 : 1000 (0✕)
        state = FiveThousandEngine.rollDice(state, listOf(1, 1, 1, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()
        assertEquals(1000, state.players[0].score)
        assertEquals(0, state.players[0].consecutiveBusts)

        // 2) Banquer +200 pts (Brelan de 2 = 200) -> Palier #2 : 1200 (0✕)
        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 2, 3, 4)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()
        assertEquals(1200, state.players[0].score)
        assertEquals(1000, state.players[0].previousScore)

        // 3) Louper 2 fois sur le palier 1200 -> 1200 (2✕)
        repeat(2) {
            state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
            state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        }
        assertEquals(1200, state.players[0].score)
        assertEquals(2, state.players[0].consecutiveBusts)

        // 4) Banquer +500 pts (Brelan de 5 = 500) -> Palier #3 : 1700 (0✕), tandis que 1200 garde ses 2✕ en mémoire
        state = FiveThousandEngine.rollDice(state, listOf(5, 5, 5, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()
        assertEquals(1700, state.players[0].score)
        assertEquals(1200, state.players[0].previousScore)
        assertEquals(0, state.players[0].consecutiveBusts)

        // 5) Louper 3 fois sur le palier 1700 -> retombe à 1200 avec ses 2 encoches historiques !
        repeat(3) {
            state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
            state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        }
        assertEquals(1200, state.players[0].score)
        assertEquals(1000, state.players[0].previousScore)
        assertEquals(2, state.players[0].consecutiveBusts)

        // 6) Louper 1 seule fois de plus sur 1200 -> 2 + 1 = 3 encoches -> retombe à 1000 (0✕) !
        state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        assertEquals(1000, state.players[0].score)
        assertEquals(0, state.players[0].previousScore)
        assertEquals(0, state.players[0].consecutiveBusts)
    }

    @Test
    fun `three strikes REVERT_TO_PREVIOUS_CLEAR_STRIKES rolls back to previous checkpoint and clears its strikes`() {
        val rules = GameRulesConfig.CLASSIC_5_DICE.copy(
            threeStrikesMode = ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES
        )
        var state = FiveThousandEngine.startNewMatch(
            "m3_clear",
            rules,
            listOf(PlayerState(id = 0, name = "Solo"))
        )

        // 1000 -> 1200 -> 2 encoches sur 1200 -> 1700 -> 3 encoches sur 1700 => retombe à 1200 mais avec 0 encoche
        state = FiveThousandEngine.rollDice(state, listOf(1, 1, 1, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 2, 3, 4)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        repeat(2) {
            state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
            state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        }
        assertEquals(2, state.players[0].consecutiveBusts)

        state = FiveThousandEngine.rollDice(state, listOf(5, 5, 5, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        repeat(3) {
            state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
            state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        }
        assertEquals(1200, state.players[0].score)
        assertEquals(0, state.players[0].consecutiveBusts)
    }

    @Test
    fun `exactFinishRequired triggers OVERFLOW_BUSTED on mandatory overshoot and wins on exact 5000`() {
        var state = FiveThousandEngine.startNewMatch(
            "m4",
            GameRulesConfig.CLASSIC_5_DICE,
            twoPlayers(p1Score = 4900, p1Opened = true)
        )

        // Lancer avec uniquement un Brelan de 2 (min score = 200) -> 4900 + 200 = 5100 > 5000 => OVERFLOW_BUSTED
        state = FiveThousandEngine.rollDice(state, listOf(2, 2, 2, 3, 4)).requireSuccess()
        assertEquals(TurnPhase.OVERFLOW_BUSTED, state.turn.phase)
        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()
        assertEquals(4900, state.players[0].score)

        // Tour du joueur 2 : coup nul rapide
        state = FiveThousandEngine.rollDice(state, listOf(2, 3, 4, 6, 6)).requireSuccess()
        state = FiveThousandEngine.acknowledgeBustAndEndTurn(state).requireSuccess()

        // Retour au joueur 1 (4900 pts) : tire un As (100) + dés morts -> 4900 + 100 = 5000 exact !
        state = FiveThousandEngine.rollDice(state, listOf(1, 2, 3, 4, 6)).requireSuccess()
        state = FiveThousandEngine.toggleDieSelection(state, 0).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        assertTrue(state.isFinished)
        assertEquals(0, state.winnerPlayerId)
        assertEquals(5000, state.players[0].score)
    }

    @Test
    fun `piggyback variant allows next opened player to inherit pot and remaining dice`() {
        val rules = GameRulesConfig.CLASSIC_5_DICE.copy(piggybackEnabled = true)
        var state = FiveThousandEngine.startNewMatch(
            "m5",
            rules,
            twoPlayers(p1Score = 1000, p1Opened = true, p2Score = 1000, p2Opened = true)
        )

        // Joueur 1 garde un brelan de 4 (400 pts) et laisse 2 dés libres en banquant
        state = FiveThousandEngine.rollDice(state, listOf(4, 4, 4, 2, 3)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        // Joueur 2 reçoit une offre de repiquage : 400 pts hérités avec 2 dés à lancer
        val offer = state.turn.piggybackOffer
        assertNotNull(offer)
        assertEquals(400, offer?.inheritedPoints)
        assertEquals(2, offer?.remainingDiceCount)

        // Joueur 2 accepte le repiquage et lance les 2 dés : un As (100) et un 2 (mort)
        state = FiveThousandEngine.acceptPiggybackAndRoll(state, listOf(1, 2)).requireSuccess()
        assertTrue(state.turn.usedPiggyback)
        assertEquals(400, state.turn.bankedInTurn)

        // Il sélectionne l'As (index 3 car 0,1,2 sont verrouillés) et banque 400 + 100 = 500 pts
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        assertEquals(500, state.turn.totalTurnPointsWithSelection)
        state = FiveThousandEngine.bankTurn(state).requireSuccess()
        assertEquals(1500, state.players[1].score)
    }

    @Test
    fun `bumpOpponentMode MINUS_500 and RESET_TO_PREVIOUS penalize opponent on exact score match`() {
        val rulesMinus = GameRulesConfig.CLASSIC_5_DICE.copy(bumpOpponentMode = BumpOpponentMode.MINUS_500)
        var state = FiveThousandEngine.startNewMatch(
            "m6",
            rulesMinus,
            twoPlayers(p1Score = 1000, p1Opened = true, p2Score = 1200, p2Opened = true)
        )

        // Joueur 1 marque 200 pts (deux As) -> atteint 1200 pts, égalisant le Joueur 2 !
        state = FiveThousandEngine.rollDice(state, listOf(1, 1, 2, 3, 4)).requireSuccess()
        state = FiveThousandEngine.selectAllScoringDice(state).requireSuccess()
        state = FiveThousandEngine.bankTurn(state).requireSuccess()

        assertEquals(1200, state.players[0].score)
        assertEquals(700, state.players[1].score) // 1200 - 500 = 700

        // Test RESET_TO_PREVIOUS
        val rulesReset = GameRulesConfig.CLASSIC_5_DICE.copy(bumpOpponentMode = BumpOpponentMode.RESET_TO_PREVIOUS)
        val p1 = PlayerState(id = 0, name = "P1", score = 1000, previousScore = 800, isOpened = true)
        val p2 = PlayerState(id = 1, name = "P2", score = 1100, previousScore = 750, isOpened = true)
        var stateReset = FiveThousandEngine.startNewMatch("m7", rulesReset, listOf(p1, p2))

        stateReset = FiveThousandEngine.rollDice(stateReset, listOf(1, 2, 3, 4, 6)).requireSuccess()
        stateReset = FiveThousandEngine.selectAllScoringDice(stateReset).requireSuccess()
        stateReset = FiveThousandEngine.bankTurn(stateReset).requireSuccess()

        assertEquals(1100, stateReset.players[0].score)
        assertEquals(750, stateReset.players[1].score)
    }

    @Test
    fun `five ones with INSTANT_WIN_ON_ONES ends match immediately`() {
        val rules = GameRulesConfig.CLASSIC_5_DICE.copy(fiveOfAKindMode = FiveOfAKindMode.INSTANT_WIN_ON_ONES)
        var state = FiveThousandEngine.startNewMatch("m8", rules, twoPlayers())
        assertNull(state.winnerPlayerId)

        state = FiveThousandEngine.rollDice(state, listOf(1, 1, 1, 1, 1)).requireSuccess()
        assertTrue(state.isFinished)
        assertTrue(state.isInstantWin)
        assertEquals(0, state.winnerPlayerId)
        assertFalse(state.players[1].isOpened)
    }
}
