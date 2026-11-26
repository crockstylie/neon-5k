package fr.antoinehory.neon5k.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import fr.antoinehory.neon5k.data.local.entity.toDomain
import fr.antoinehory.neon5k.data.local.entity.toEntity
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests instrumentés Android pour [AppDatabase], `MatchDao` et `DecisionLogDao`.
 */
@RunWith(AndroidJUnit4::class)
class NeonDatabaseInstrumentedTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun matchDaoUpsertsAndRestoresActiveMatchAccurately() = runBlocking {
        val players = listOf(
            PlayerState(id = 0, name = "Alice", type = PlayerType.HUMAN, score = 1200, isOpened = true),
            PlayerState(id = 1, name = "Cyber-Bot", type = PlayerType.BOT_BALANCED, score = 850, isOpened = true)
        )
        val match = FiveThousandEngine.startNewMatch(
            matchId = "match-test-1",
            rules = GameRulesConfig.VARIANT_6_DICE,
            players = players
        )

        db.matchDao().upsertMatch(match.toEntity())
        val loaded = db.matchDao().getLatestActiveMatch()?.toDomain()

        assertNotNull(loaded)
        assertEquals("match-test-1", loaded?.matchId)
        assertEquals(6, loaded?.rules?.diceCount)
        assertEquals(1200, loaded?.players?.get(0)?.score)
        assertEquals(PlayerType.BOT_BALANCED, loaded?.players?.get(1)?.type)
    }

    @Test
    fun decisionLogDaoInsertsAndRetroactivelyUpdatesTurnAndMatchOutcomes() = runBlocking {
        val log1 = DecisionLogEntity(
            matchUuid = "m-ai-1",
            rulesFingerprint = GameRulesConfig.CLASSIC_5_DICE.fingerprint(),
            turnIndex = 1,
            stepIndex = 1,
            actorId = 0,
            actorType = "HUMAN",
            myScore = 1000,
            bestOpponentScore = 800,
            turnScoreBefore = 0,
            remainingDiceBefore = 5,
            isOpened = true,
            consecutiveBusts = 0,
            rolledDiceCsv = "1,2,3,4,5",
            keptDiceCsv = "1,2,3,4,5",
            decision = "ROLL_AGAIN"
        )
        val log2 = log1.copy(
            stepIndex = 2,
            turnScoreBefore = 1500,
            rolledDiceCsv = "1,2,2,4,6",
            keptDiceCsv = "1",
            decision = "BANK"
        )

        db.decisionLogDao().insertDecision(log1)
        db.decisionLogDao().insertDecision(log2)

        db.decisionLogDao().updateTurnOutcome(matchUuid = "m-ai-1", turnIndex = 1, finalPoints = 1600)
        db.decisionLogDao().updateMatchOutcome(matchUuid = "m-ai-1", winnerActorId = 0)

        val all = db.decisionLogDao().getAllDecisions()
        assertEquals(2, all.size)
        assertTrue(all.all { it.turnFinalPoints == 1600 })
        assertTrue(all.all { it.matchWon == true })
    }
}
