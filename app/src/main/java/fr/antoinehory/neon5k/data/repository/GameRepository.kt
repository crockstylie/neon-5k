package fr.antoinehory.neon5k.data.repository

import fr.antoinehory.neon5k.data.local.AppDatabase
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity
import fr.antoinehory.neon5k.data.local.entity.toDomain
import fr.antoinehory.neon5k.data.local.entity.toEntity
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository orchestrant la sauvegarde automatique de la partie en cours, les statistiques locales
 * et la journalisation non-bloquante des décisions (`decision_logs`) sur `Dispatchers.IO`.
 */
class GameRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val matchDao = database.matchDao()
    private val statsDao = database.statsDao()
    private val decisionLogDao = database.decisionLogDao()

    val latestActiveMatchFlow: Flow<MatchState?> =
        matchDao.observeLatestActiveMatch().map { entity ->
            runCatching { entity?.toDomain() }.getOrNull()
        }

    val playerStatsFlow: Flow<PlayerStatsEntity> =
        statsDao.observeStats().map { it ?: PlayerStatsEntity() }

    val totalDecisionLogsCountFlow: Flow<Int> =
        decisionLogDao.observeTotalCount()

    val finishedMatchesFlow: Flow<List<SavedMatchEntity>> =
        matchDao.observeFinishedMatches(limit = 20)

    suspend fun getLatestActiveMatch(): MatchState? = withContext(ioDispatcher) {
        runCatching { matchDao.getLatestActiveMatch()?.toDomain() }.getOrNull()
    }

    /**
     * Sauvegarde ou met à jour l'état complet de la partie dans Room sur `Dispatchers.IO`.
     */
    suspend fun saveMatchSnapshot(state: MatchState) = withContext(ioDispatcher) {
        try {
            val existing = matchDao.getMatchById(state.matchId)
            val now = System.currentTimeMillis()
            val entity = state.toEntity(
                createdAtEpochMs = existing?.createdAtEpochMs ?: now,
                updatedAtEpochMs = now
            )
            matchDao.upsertMatch(entity)
            if (!state.isFinished) {
                matchDao.deleteOtherActiveMatches(state.matchId)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ne jamais faire crasher une partie en cours sur une erreur disque/SQLite
        }
    }

    /**
     * Enregistre une décision `(état, dés gardés, ROLL_AGAIN ou BANK)` avant d'exécuter l'action dans le moteur.
     */
    suspend fun logDecision(
        stateBeforeAction: MatchState,
        keptFaces: List<Int>,
        decision: String
    ) = withContext(ioDispatcher) {
        try {
            val player = stateBeforeAction.activePlayer
            val turn = stateBeforeAction.turn
            val bestOpponent = stateBeforeAction.players
                .filter { it.id != player.id }
                .maxOfOrNull { it.score } ?: 0

            val freeRolledFaces = turn.dice
                .filter { !it.isLocked && it.face in 1..6 }
                .map { it.face }
                .sorted()

            if (freeRolledFaces.isEmpty() || keptFaces.isEmpty()) return@withContext

            val entity = DecisionLogEntity(
                matchUuid = stateBeforeAction.matchId,
                rulesFingerprint = stateBeforeAction.rules.fingerprint(),
                turnIndex = turn.turnNumber,
                stepIndex = turn.rollIndexInTurn,
                actorId = player.id,
                actorType = player.type.name,
                myScore = player.score,
                bestOpponentScore = bestOpponent,
                turnScoreBefore = turn.bankedInTurn,
                remainingDiceBefore = freeRolledFaces.size,
                isOpened = player.isOpened,
                consecutiveBusts = player.consecutiveBusts,
                rolledDiceCsv = freeRolledFaces.joinToString(","),
                keptDiceCsv = keptFaces.sorted().joinToString(","),
                decision = decision
            )
            decisionLogDao.insertDecision(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Écriture non-bloquante
        }
    }

    /**
     * Complète rétroactivement l'issue du tour (`turnFinalPoints`) et met à jour les statistiques humaines.
     */
    suspend fun recordTurnFinished(
        matchUuid: String,
        turnNumber: Int,
        actorType: PlayerType,
        finalTurnPoints: Int,
        hotDiceInTurn: Int,
        usedPiggyback: Boolean,
        rollsInTurn: Int
    ) = withContext(ioDispatcher) {
        try {
            decisionLogDao.updateTurnOutcome(
                matchUuid = matchUuid,
                turnIndex = turnNumber,
                finalPoints = finalTurnPoints
            )
            if (actorType == PlayerType.HUMAN) {
                val current = statsDao.getStats() ?: PlayerStatsEntity()
                val isBust = finalTurnPoints == 0
                val updated = current.copy(
                    bestTurnScore = maxOf(current.bestTurnScore, finalTurnPoints),
                    totalHotDiceCount = current.totalHotDiceCount + hotDiceInTurn,
                    totalBustsCount = current.totalBustsCount + if (isBust) 1 else 0,
                    biggestPiggybackScore = if (usedPiggyback) {
                        maxOf(current.biggestPiggybackScore, finalTurnPoints)
                    } else {
                        current.biggestPiggybackScore
                    },
                    totalRollsCount = current.totalRollsCount + rollsInTurn
                )
                statsDao.upsertStats(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-bloquant
        }
    }

    /**
     * Complète rétroactivement l'issue de la partie (`matchWon`) et incrémente le compteur de parties jouées/gagnées.
     */
    suspend fun recordMatchFinished(
        state: MatchState
    ) = withContext(ioDispatcher) {
        try {
            val winnerId = state.winnerPlayerId ?: return@withContext
            decisionLogDao.updateMatchOutcome(
                matchUuid = state.matchId,
                winnerActorId = winnerId
            )
            val hasHumanPlayer = state.players.any { it.type == PlayerType.HUMAN }
            if (hasHumanPlayer) {
                val humanWon = state.winnerPlayer?.type == PlayerType.HUMAN
                val current = statsDao.getStats() ?: PlayerStatsEntity()
                statsDao.upsertStats(
                    current.copy(
                        matchesPlayed = current.matchesPlayed + 1,
                        matchesWon = current.matchesWon + if (humanWon) 1 else 0
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Non-bloquant
        }
    }

    suspend fun getAllDecisionsForExport(): List<DecisionLogEntity> = withContext(ioDispatcher) {
        decisionLogDao.getAllDecisions()
    }

    suspend fun clearDecisionLogs() = withContext(ioDispatcher) {
        decisionLogDao.clearAllDecisions()
    }
}
