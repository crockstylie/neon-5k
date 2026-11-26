package fr.antoinehory.neon5k.data.repository

import fr.antoinehory.neon5k.data.local.AppDatabase
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity
import fr.antoinehory.neon5k.data.local.entity.toDomain
import fr.antoinehory.neon5k.data.local.entity.toDomainOrNull
import fr.antoinehory.neon5k.data.local.entity.toEntity
import fr.antoinehory.neon5k.domain.achievement.AchievementEvaluator
import fr.antoinehory.neon5k.domain.achievement.AchievementId
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress
import fr.antoinehory.neon5k.domain.achievement.AchievementUnlockNotification
import fr.antoinehory.neon5k.domain.achievement.TelemetryGameEvent
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.rules.CombinationKind
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository orchestrant la sauvegarde automatique de la partie en cours, les statistiques locales,
 * la télémétrie fun, les 20 succès à comptage continu post-déverrouillage et la journalisation
 * non-bloquante des décisions (`decision_logs`) sur `Dispatchers.IO`.
 */
class GameRepository(
    private val database: AppDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val matchDao = database.matchDao()
    private val statsDao = database.statsDao()
    private val decisionLogDao = database.decisionLogDao()
    private val achievementDao = database.achievementDao()

    val latestActiveMatchFlow: Flow<MatchState?> =
        matchDao.observeLatestActiveMatch().map { entity ->
            runCatching { entity?.toDomain() }.getOrNull()
        }

    val playerStatsFlow: Flow<PlayerStatsEntity> =
        statsDao.observeStats().map { it ?: PlayerStatsEntity() }

    val achievementsFlow: Flow<List<AchievementProgress>> =
        achievementDao.observeAllAchievements().map { entities ->
            val storedMap = entities.mapNotNull { it.toDomainOrNull() }.associateBy { it.id }
            AchievementId.entries.map { id ->
                storedMap[id] ?: AchievementProgress(id = id)
            }
        }

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
     * Enregistre la télémétrie d'un lancer de dés effectué par un joueur humain (distribution des faces 1..6,
     * combinaisons obtenues sur le lancer, secousse, sauvetage à 1 dé) et évalue les succès associés.
     */
    suspend fun recordHumanRollTelemetry(
        activeRollFaces: List<Int>,
        rules: GameRulesConfig,
        triggeredByShake: Boolean,
        pressDurationMs: Long
    ): List<AchievementUnlockNotification> = withContext(ioDispatcher) {
        try {
            if (activeRollFaces.isEmpty()) return@withContext emptyList()
            val currentStats = statsDao.getStats() ?: PlayerStatsEntity()
            val maxEval = DiceScorer.evaluateMaxScoringSubset(activeRollFaces, rules)
            val scoredOnSingleDie = activeRollFaces.size == 1 && maxEval.totalPoints > 0

            var f1 = 0
            var f2 = 0
            var f3 = 0
            var f4 = 0
            var f5 = 0
            var f6 = 0
            for (face in activeRollFaces) {
                when (face) {
                    1 -> f1++
                    2 -> f2++
                    3 -> f3++
                    4 -> f4++
                    5 -> f5++
                    6 -> f6++
                }
            }

            var t1 = 0
            var t2 = 0
            var t3 = 0
            var t4 = 0
            var t5 = 0
            var t6 = 0
            var fourKind = 0
            var fiveKind = 0
            var straights = 0
            var threePairs = 0
            var fullHouses = 0
            var single1 = 0
            var single5 = 0

            fun addTripleFace(face: Int) {
                when (face) {
                    1 -> t1++
                    2 -> t2++
                    3 -> t3++
                    4 -> t4++
                    5 -> t5++
                    6 -> t6++
                }
            }

            for (combo in maxEval.combinations) {
                when (combo.kind) {
                    CombinationKind.SINGLE_ONE -> single1 += combo.diceFaces.size
                    CombinationKind.SINGLE_FIVE -> single5 += combo.diceFaces.size
                    CombinationKind.THREE_OF_A_KIND -> {
                        addTripleFace(combo.diceFaces.firstOrNull() ?: 0)
                    }
                    CombinationKind.FOUR_OF_A_KIND -> {
                        fourKind++
                        addTripleFace(combo.diceFaces.firstOrNull() ?: 0)
                    }
                    CombinationKind.FIVE_OF_A_KIND, CombinationKind.SIX_OF_A_KIND -> {
                        fiveKind++
                        addTripleFace(combo.diceFaces.firstOrNull() ?: 0)
                    }
                    CombinationKind.STRAIGHT -> straights++
                    CombinationKind.THREE_PAIRS -> threePairs++
                    CombinationKind.FULL_HOUSE -> {
                        fullHouses++
                        val tripleFace = combo.diceFaces.groupingBy { it }.eachCount()
                            .entries.firstOrNull { it.value >= 3 }?.key ?: 0
                        addTripleFace(tripleFace)
                    }
                }
            }

            val updatedStats = currentStats.copy(
                totalRollsCount = currentStats.totalRollsCount + 1,
                totalIndividualDiceThrown = currentStats.totalIndividualDiceThrown + activeRollFaces.size,
                shakeRollsCount = currentStats.shakeRollsCount + if (triggeredByShake) 1 else 0,
                totalTouchPressDurationMs = currentStats.totalTouchPressDurationMs + pressDurationMs.coerceAtLeast(0L),
                face1Count = currentStats.face1Count + f1,
                face2Count = currentStats.face2Count + f2,
                face3Count = currentStats.face3Count + f3,
                face4Count = currentStats.face4Count + f4,
                face5Count = currentStats.face5Count + f5,
                face6Count = currentStats.face6Count + f6,
                triple1Count = currentStats.triple1Count + t1,
                triple2Count = currentStats.triple2Count + t2,
                triple3Count = currentStats.triple3Count + t3,
                triple4Count = currentStats.triple4Count + t4,
                triple5Count = currentStats.triple5Count + t5,
                triple6Count = currentStats.triple6Count + t6,
                fourOfAKindCount = currentStats.fourOfAKindCount + fourKind,
                fiveOfAKindCount = currentStats.fiveOfAKindCount + fiveKind,
                straightCount = currentStats.straightCount + straights,
                threePairsCount = currentStats.threePairsCount + threePairs,
                fullHouseCount = currentStats.fullHouseCount + fullHouses,
                singleOnesCount = currentStats.singleOnesCount + single1,
                singleFivesCount = currentStats.singleFivesCount + single5,
                oneDieClutchCount = currentStats.oneDieClutchCount + if (scoredOnSingleDie) 1 else 0
            )
            statsDao.upsertStats(updatedStats)

            evaluateAndPersistAchievements(
                event = TelemetryGameEvent.HumanRollExecuted(
                    rolledFaces = activeRollFaces,
                    combinationsOnRoll = maxEval.combinations,
                    triggeredByShake = triggeredByShake,
                    pressDurationMs = pressDurationMs,
                    scoredOnSingleRemainingDie = scoredOnSingleDie
                ),
                distinctTripleFacesMask = updatedStats.distinctTripleFacesMask
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Complète rétroactivement l'issue du tour (`turnFinalPoints`), met à jour la télémétrie fun et évalue les succès.
     */
    suspend fun recordTurnFinished(
        matchUuid: String,
        turnNumber: Int,
        actorType: PlayerType,
        finalTurnPoints: Int,
        hotDiceInTurn: Int,
        usedPiggyback: Boolean,
        wasOpeningTurn: Boolean = false,
        bumpedOpponentByActor: Boolean = false,
        humanWasBumpedByOpponent: Boolean = false,
        lostPotOnBust: Int = 0,
        triggeredThreeStrikesPenalty: Boolean = false
    ): List<AchievementUnlockNotification> = withContext(ioDispatcher) {
        try {
            decisionLogDao.updateTurnOutcome(
                matchUuid = matchUuid,
                turnIndex = turnNumber,
                finalPoints = finalTurnPoints
            )

            val current = statsDao.getStats() ?: PlayerStatsEntity()
            if (actorType != PlayerType.HUMAN) {
                if (humanWasBumpedByOpponent) {
                    statsDao.upsertStats(
                        current.copy(bumpsSufferedCount = current.bumpsSufferedCount + 1)
                    )
                }
                return@withContext emptyList()
            }

            val isBust = finalTurnPoints == 0
            val updated = current.copy(
                bestTurnScore = maxOf(current.bestTurnScore, finalTurnPoints),
                totalHotDiceCount = current.totalHotDiceCount + hotDiceInTurn,
                totalBustsCount = current.totalBustsCount + if (isBust) 1 else 0,
                biggestPiggybackScore = if (usedPiggyback && !isBust) {
                    maxOf(current.biggestPiggybackScore, finalTurnPoints)
                } else {
                    current.biggestPiggybackScore
                },
                totalPointsBanked = current.totalPointsBanked + finalTurnPoints.coerceAtLeast(0),
                totalPointsLostToBusts = current.totalPointsLostToBusts + if (isBust) lostPotOnBust.coerceAtLeast(0) else 0,
                worstBustPotLost = if (isBust) maxOf(current.worstBustPotLost, lostPotOnBust) else current.worstBustPotLost,
                piggybacksSucceededCount = current.piggybacksSucceededCount + if (usedPiggyback && !isBust) 1 else 0,
                bumpsInflictedCount = current.bumpsInflictedCount + if (bumpedOpponentByActor) 1 else 0,
                threeStrikesPenaltiesCount = current.threeStrikesPenaltiesCount + if (triggeredThreeStrikesPenalty) 1 else 0
            )
            statsDao.upsertStats(updated)

            val event = if (!isBust) {
                TelemetryGameEvent.HumanTurnBanked(
                    turnPoints = finalTurnPoints,
                    wasOpeningTurn = wasOpeningTurn,
                    hotDiceCountInTurn = hotDiceInTurn,
                    usedPiggyback = usedPiggyback,
                    bumpedOpponent = bumpedOpponentByActor
                )
            } else {
                TelemetryGameEvent.HumanTurnBusted(
                    lostPotPoints = lostPotOnBust,
                    triggeredThreeStrikesPenalty = triggeredThreeStrikesPenalty
                )
            }
            evaluateAndPersistAchievements(
                event = event,
                distinctTripleFacesMask = updated.distinctTripleFacesMask
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Complète rétroactivement l'issue de la partie (`matchWon`), incrémente le bilan contre chaque profil de Bot
     * et évalue les succès de fin de partie.
     */
    suspend fun recordMatchFinished(
        state: MatchState
    ): List<AchievementUnlockNotification> = withContext(ioDispatcher) {
        try {
            val winnerId = state.winnerPlayerId ?: return@withContext emptyList()
            decisionLogDao.updateMatchOutcome(
                matchUuid = state.matchId,
                winnerActorId = winnerId
            )
            val hasHumanPlayer = state.players.any { it.type == PlayerType.HUMAN }
            if (!hasHumanPlayer) return@withContext emptyList()

            val winner = state.winnerPlayer
            val humanWon = winner?.type == PlayerType.HUMAN
            val hasBotSafe = state.players.any { it.type == PlayerType.BOT_SAFE }
            val hasBotBalanced = state.players.any { it.type == PlayerType.BOT_BALANCED }
            val hasBotKamikaze = state.players.any { it.type == PlayerType.BOT_KAMIKAZE }

            val current = statsDao.getStats() ?: PlayerStatsEntity()
            val updated = current.copy(
                matchesPlayed = current.matchesPlayed + 1,
                matchesWon = current.matchesWon + if (humanWon) 1 else 0,
                winsVsBotSafe = current.winsVsBotSafe + if (humanWon && hasBotSafe) 1 else 0,
                lossesVsBotSafe = current.lossesVsBotSafe + if (!humanWon && winner?.type == PlayerType.BOT_SAFE) 1 else 0,
                winsVsBotBalanced = current.winsVsBotBalanced + if (humanWon && hasBotBalanced) 1 else 0,
                lossesVsBotBalanced = current.lossesVsBotBalanced + if (!humanWon && winner?.type == PlayerType.BOT_BALANCED) 1 else 0,
                winsVsBotKamikaze = current.winsVsBotKamikaze + if (humanWon && hasBotKamikaze) 1 else 0,
                lossesVsBotKamikaze = current.lossesVsBotKamikaze + if (!humanWon && winner?.type == PlayerType.BOT_KAMIKAZE) 1 else 0
            )
            statsDao.upsertStats(updated)

            val wonExact = humanWon && !state.isInstantWin && (winner?.score == state.rules.targetScore)
            evaluateAndPersistAchievements(
                event = TelemetryGameEvent.MatchCompleted(
                    humanWon = humanWon,
                    wonWithExactTarget = wonExact,
                    defeatedBotSafe = humanWon && hasBotSafe,
                    defeatedBotBalanced = humanWon && hasBotBalanced,
                    defeatedBotKamikaze = humanWon && hasBotKamikaze
                ),
                distinctTripleFacesMask = updated.distinctTripleFacesMask
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getAllAchievementsProgress(): List<AchievementProgress> = withContext(ioDispatcher) {
        val stored = achievementDao.getAllAchievements().mapNotNull { it.toDomainOrNull() }.associateBy { it.id }
        AchievementId.entries.map { id -> stored[id] ?: AchievementProgress(id = id) }
    }

    private suspend fun evaluateAndPersistAchievements(
        event: TelemetryGameEvent,
        distinctTripleFacesMask: Int
    ): List<AchievementUnlockNotification> {
        val currentMap = achievementDao.getAllAchievements()
            .mapNotNull { it.toDomainOrNull() }
            .associateBy { it.id }
        val outcome = AchievementEvaluator.evaluate(
            currentMap = currentMap,
            event = event,
            distinctTripleFacesMask = distinctTripleFacesMask
        )
        achievementDao.upsertAchievements(
            outcome.updatedProgressMap.values.map { it.toEntity() }
        )
        return outcome.notifications
    }

    suspend fun getAllDecisionsForExport(): List<DecisionLogEntity> = withContext(ioDispatcher) {
        decisionLogDao.getAllDecisions()
    }

    suspend fun clearDecisionLogs() = withContext(ioDispatcher) {
        decisionLogDao.clearAllDecisions()
    }
}
