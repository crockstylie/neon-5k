package fr.antoinehory.neon5k.data.repository

import fr.antoinehory.neon5k.data.local.dao.AchievementDao
import fr.antoinehory.neon5k.data.local.dao.DecisionLogDao
import fr.antoinehory.neon5k.data.local.dao.MatchDao
import fr.antoinehory.neon5k.data.local.dao.StatsDao
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
import fr.antoinehory.neon5k.domain.ai.AdaptiveBotLearner
import fr.antoinehory.neon5k.domain.ai.AdaptiveLearningSummary
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.rules.CombinationKind
import fr.antoinehory.neon5k.domain.rules.ScoringCombination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository centralisant :
 * - la sauvegarde/reprise automatique de partie (`saved_matches`),
 * - l'historique des parties terminées (`finishedMatchesFlow`),
 * - l'agrégation des statistiques de carrière (`player_stats`),
 * - l'évaluation et la persistance des 20 Hauts Faits locaux & Play Games (`achievements`),
 * - la journalisation SQLite des vecteurs `(état, action, issue)` pour le dataset IA (`decision_logs`),
 * - et le ré-entraînement automatique en mémoire de l'IA locale ([AdaptiveBotLearner]).
 */
class GameRepository(
    private val matchDao: MatchDao,
    private val statsDao: StatsDao,
    private val decisionLogDao: DecisionLogDao,
    private val achievementDao: AchievementDao
) {
    /** Flux réactif de la dernière partie active non terminée (`null` si aucune partie en cours). */
    val activeMatchFlow: Flow<MatchState?> = matchDao.observeLatestActiveMatch()
        .map { entity -> entity?.let(SavedMatchEntity::toDomain) }

    /** Flux réactif des 30 dernières parties terminées pour l'onglet Historique & Graphiques. */
    val finishedMatchesFlow: Flow<List<SavedMatchEntity>> = matchDao.observeFinishedMatches(limit = 30)

    /** Flux réactif des statistiques agrégées du joueur humain. */
    val playerStatsFlow: Flow<PlayerStatsEntity> = statsDao.observeStats()
        .map { it ?: PlayerStatsEntity() }

    /** Flux réactif des 20 Hauts Faits fusionnant le catalogue [AchievementId] et la base Room. */
    val achievementsFlow: Flow<List<AchievementProgress>> = achievementDao.observeAllAchievements()
        .map { rows ->
            val byId = rows.mapNotNull { it.toDomainOrNull() }.associateBy { it.id }
            AchievementId.entries.map { id ->
                byId[id] ?: AchievementProgress(id = id)
            }
        }

    /** Flux réactif du nombre total de décisions enregistrées dans le dataset IA local. */
    val decisionLogsCountFlow: Flow<Int> = decisionLogDao.observeTotalCount()

    private val _adaptiveLearningFlow = MutableStateFlow(AdaptiveBotLearner.getSummary())
    val adaptiveLearningFlow: StateFlow<AdaptiveLearningSummary> = _adaptiveLearningFlow.asStateFlow()

    /**
     * Recharge les décisions terminées de `decision_logs` et met à jour le modèle tabulaire de [AdaptiveBotLearner].
     */
    suspend fun refreshAdaptiveAiModel() {
        val records = decisionLogDao.getAllDecisions().map { it.toDomain() }
        AdaptiveBotLearner.trainFromRecords(records)
        _adaptiveLearningFlow.value = AdaptiveBotLearner.getSummary()
    }

    /**
     * Sauvegarde ou met à jour l'état complet de la partie courante dans Room.
     */
    suspend fun saveMatchState(state: MatchState) {
        val now = System.currentTimeMillis()
        val existing = matchDao.getMatchById(state.matchId)
        val createdAt = existing?.createdAtEpochMs ?: now
        val entity = SavedMatchEntity.fromDomain(
            state = state,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = now
        )
        matchDao.upsertMatch(entity)
        if (!state.isFinished) {
            matchDao.deleteOtherActiveMatches(keepMatchUuid = state.matchId)
        }
    }

    /**
     * Charge la dernière partie active non terminée si elle existe.
     */
    suspend fun loadLatestActiveMatch(): MatchState? {
        return matchDao.getLatestActiveMatch()?.let(SavedMatchEntity::toDomain)
    }

    /**
     * Enregistre un lancer de dés effectué par un joueur humain dans les statistiques globales
     * et évalue les succès liés aux combinaisons tirées.
     */
    suspend fun recordHumanRoll(
        rolledFaces: List<Int>,
        combinationsOnRoll: List<ScoringCombination>,
        triggeredByShake: Boolean,
        pressDurationMs: Long,
        scoredOnSingleRemainingDie: Boolean
    ): List<AchievementUnlockNotification> {
        if (rolledFaces.isEmpty()) return emptyList()
        val current = statsDao.getStats() ?: PlayerStatsEntity()

        var c1 = current.face1Count
        var c2 = current.face2Count
        var c3 = current.face3Count
        var c4 = current.face4Count
        var c5 = current.face5Count
        var c6 = current.face6Count
        for (face in rolledFaces) {
            when (face) {
                1 -> c1++
                2 -> c2++
                3 -> c3++
                4 -> c4++
                5 -> c5++
                6 -> c6++
            }
        }

        var t1 = current.triple1Count
        var t2 = current.triple2Count
        var t3 = current.triple3Count
        var t4 = current.triple4Count
        var t5 = current.triple5Count
        var t6 = current.triple6Count
        var fourKind = current.fourOfAKindCount
        var fiveKind = current.fiveOfAKindCount
        var straights = current.straightCount
        var threePairs = current.threePairsCount
        var fullHouses = current.fullHouseCount
        var singleOnes = current.singleOnesCount
        var singleFives = current.singleFivesCount

        for (combo in combinationsOnRoll) {
            when (combo.kind) {
                CombinationKind.THREE_OF_A_KIND -> {
                    when (combo.diceFaces.firstOrNull() ?: 0) {
                        1 -> t1++
                        2 -> t2++
                        3 -> t3++
                        4 -> t4++
                        5 -> t5++
                        6 -> t6++
                    }
                }
                CombinationKind.FOUR_OF_A_KIND -> fourKind++
                CombinationKind.FIVE_OF_A_KIND, CombinationKind.SIX_OF_A_KIND -> fiveKind++
                CombinationKind.STRAIGHT -> straights++
                CombinationKind.THREE_PAIRS -> threePairs++
                CombinationKind.FULL_HOUSE -> fullHouses++
                CombinationKind.SINGLE_ONE -> singleOnes += combo.diceFaces.size
                CombinationKind.SINGLE_FIVE -> singleFives += combo.diceFaces.size
            }
        }

        val updatedStats = current.copy(
            totalRollsCount = current.totalRollsCount + 1,
            totalIndividualDiceThrown = current.totalIndividualDiceThrown + rolledFaces.size,
            shakeRollsCount = current.shakeRollsCount + if (triggeredByShake) 1 else 0,
            totalTouchPressDurationMs = current.totalTouchPressDurationMs + pressDurationMs.coerceAtLeast(0L),
            face1Count = c1,
            face2Count = c2,
            face3Count = c3,
            face4Count = c4,
            face5Count = c5,
            face6Count = c6,
            triple1Count = t1,
            triple2Count = t2,
            triple3Count = t3,
            triple4Count = t4,
            triple5Count = t5,
            triple6Count = t6,
            fourOfAKindCount = fourKind,
            fiveOfAKindCount = fiveKind,
            straightCount = straights,
            threePairsCount = threePairs,
            fullHouseCount = fullHouses,
            singleOnesCount = singleOnes,
            singleFivesCount = singleFives,
            oneDieClutchCount = current.oneDieClutchCount + if (scoredOnSingleRemainingDie) 1 else 0
        )
        statsDao.upsertStats(updatedStats)

        return evaluateAndPersistEvent(
            event = TelemetryGameEvent.HumanRollExecuted(
                rolledFaces = rolledFaces,
                combinationsOnRoll = combinationsOnRoll,
                triggeredByShake = triggeredByShake,
                pressDurationMs = pressDurationMs,
                scoredOnSingleRemainingDie = scoredOnSingleRemainingDie
            ),
            distinctTripleFacesMask = updatedStats.distinctTripleFacesMask
        )
    }

    /**
     * Enregistre une banque réussie par un joueur humain et évalue les succès associés.
     */
    suspend fun recordHumanBank(
        turnPoints: Int,
        wasOpeningTurn: Boolean,
        hotDiceCountInTurn: Int,
        usedPiggyback: Boolean,
        didBumpOpponent: Boolean
    ): List<AchievementUnlockNotification> {
        val current = statsDao.getStats() ?: PlayerStatsEntity()
        val updatedStats = current.copy(
            totalPointsBanked = current.totalPointsBanked + turnPoints,
            bestTurnScore = maxOf(current.bestTurnScore, turnPoints),
            totalHotDiceCount = current.totalHotDiceCount + hotDiceCountInTurn,
            biggestPiggybackScore = if (usedPiggyback) {
                maxOf(current.biggestPiggybackScore, turnPoints)
            } else {
                current.biggestPiggybackScore
            },
            piggybacksSucceededCount = current.piggybacksSucceededCount + if (usedPiggyback) 1 else 0,
            bumpsInflictedCount = current.bumpsInflictedCount + if (didBumpOpponent) 1 else 0
        )
        statsDao.upsertStats(updatedStats)

        return evaluateAndPersistEvent(
            event = TelemetryGameEvent.HumanTurnBanked(
                turnPoints = turnPoints,
                wasOpeningTurn = wasOpeningTurn,
                hotDiceCountInTurn = hotDiceCountInTurn,
                usedPiggyback = usedPiggyback,
                bumpedOpponent = didBumpOpponent
            ),
            distinctTripleFacesMask = updatedStats.distinctTripleFacesMask
        )
    }

    /**
     * Enregistre un Chapeau subi par un joueur humain.
     */
    suspend fun recordHumanBumpedByOpponent() {
        val current = statsDao.getStats() ?: PlayerStatsEntity()
        statsDao.upsertStats(current.copy(bumpsSufferedCount = current.bumpsSufferedCount + 1))
    }

    /**
     * Enregistre un coup nul (Glitch) subi par un joueur humain.
     */
    suspend fun recordHumanBust(
        lostTurnPot: Int,
        triggeredThreeStrikesPenalty: Boolean
    ): List<AchievementUnlockNotification> {
        val current = statsDao.getStats() ?: PlayerStatsEntity()
        val updatedStats = current.copy(
            totalBustsCount = current.totalBustsCount + 1,
            totalPointsLostToBusts = current.totalPointsLostToBusts + lostTurnPot.coerceAtLeast(0),
            worstBustPotLost = maxOf(current.worstBustPotLost, lostTurnPot.coerceAtLeast(0)),
            threeStrikesPenaltiesCount = current.threeStrikesPenaltiesCount +
                if (triggeredThreeStrikesPenalty) 1 else 0
        )
        statsDao.upsertStats(updatedStats)

        return evaluateAndPersistEvent(
            event = TelemetryGameEvent.HumanTurnBusted(
                lostPotPoints = lostTurnPot.coerceAtLeast(0),
                triggeredThreeStrikesPenalty = triggeredThreeStrikesPenalty
            ),
            distinctTripleFacesMask = updatedStats.distinctTripleFacesMask
        )
    }

    /**
     * Enregistre la fin d'une partie dans les statistiques de carrière, ré-entraîne [AdaptiveBotLearner]
     * et évalue les succès de victoire.
     */
    suspend fun recordMatchCompleted(state: MatchState): List<AchievementUnlockNotification> {
        val winner = state.winnerPlayer ?: return emptyList()
        val hasHumanPlayer = state.players.any { it.type == PlayerType.HUMAN }
        if (!hasHumanPlayer) {
            refreshAdaptiveAiModel()
            return emptyList()
        }

        val current = statsDao.getStats() ?: PlayerStatsEntity()
        val humanWon = winner.type == PlayerType.HUMAN
        val botTypes = state.players.filter { it.type != PlayerType.HUMAN }.map { it.type }.toSet()

        val hasSafe = PlayerType.BOT_SAFE in botTypes
        val hasBalanced = PlayerType.BOT_BALANCED in botTypes
        val hasKamikaze = PlayerType.BOT_KAMIKAZE in botTypes

        val updatedStats = current.copy(
            matchesPlayed = current.matchesPlayed + 1,
            matchesWon = current.matchesWon + if (humanWon) 1 else 0,
            winsVsBotSafe = current.winsVsBotSafe + if (humanWon && hasSafe) 1 else 0,
            lossesVsBotSafe = current.lossesVsBotSafe + if (!humanWon && hasSafe) 1 else 0,
            winsVsBotBalanced = current.winsVsBotBalanced + if (humanWon && hasBalanced) 1 else 0,
            lossesVsBotBalanced = current.lossesVsBotBalanced + if (!humanWon && hasBalanced) 1 else 0,
            winsVsBotKamikaze = current.winsVsBotKamikaze + if (humanWon && hasKamikaze) 1 else 0,
            lossesVsBotKamikaze = current.lossesVsBotKamikaze + if (!humanWon && hasKamikaze) 1 else 0
        )
        statsDao.upsertStats(updatedStats)
        refreshAdaptiveAiModel()

        val wonWithExactTarget = humanWon && !state.isInstantWin && winner.score == state.rules.targetScore
        return evaluateAndPersistEvent(
            event = TelemetryGameEvent.MatchCompleted(
                humanWon = humanWon,
                wonWithExactTarget = wonWithExactTarget,
                defeatedBotSafe = humanWon && hasSafe,
                defeatedBotBalanced = humanWon && hasBalanced,
                defeatedBotKamikaze = humanWon && hasKamikaze
            ),
            distinctTripleFacesMask = updatedStats.distinctTripleFacesMask
        )
    }

    private suspend fun evaluateAndPersistEvent(
        event: TelemetryGameEvent,
        distinctTripleFacesMask: Int
    ): List<AchievementUnlockNotification> {
        val existingRows = achievementDao.getAllAchievements()
            .mapNotNull { it.toDomainOrNull() }
            .associateBy { it.id }

        val currentMap = AchievementId.entries.associateWith { id ->
            existingRows[id] ?: AchievementProgress(id = id)
        }

        val outcome = AchievementEvaluator.evaluate(
            currentMap = currentMap,
            event = event,
            distinctTripleFacesMask = distinctTripleFacesMask,
            nowEpochMs = System.currentTimeMillis()
        )

        val entities = outcome.updatedProgressMap.values.map { it.toEntity() }
        achievementDao.upsertAchievements(entities)
        return outcome.notifications
    }

    /**
     * Récupère la liste courante des 20 succès pour la synchronisation Google Play Jeux.
     */
    suspend fun getAllAchievementsProgress(): List<AchievementProgress> {
        val existingRows = achievementDao.getAllAchievements()
            .mapNotNull { it.toDomainOrNull() }
            .associateBy { it.id }
        return AchievementId.entries.map { id -> existingRows[id] ?: AchievementProgress(id = id) }
    }

    /**
     * Insère une décision `(état, action)` prise par un joueur humain ou un bot au cours d'un tour.
     */
    suspend fun logDecisionStep(
        stateBeforeAction: MatchState,
        stepIndex: Int,
        rolledFaces: List<Int>,
        keptFaces: List<Int>,
        decision: String
    ) {
        val activePlayer = stateBeforeAction.activePlayer
        val bestOpponentScore = stateBeforeAction.players
            .filter { it.id != activePlayer.id }
            .maxOfOrNull { it.score } ?: 0

        val entity = DecisionLogEntity(
            matchUuid = stateBeforeAction.matchId,
            rulesFingerprint = stateBeforeAction.rules.toFingerprint(),
            turnIndex = stateBeforeAction.turn.turnNumber,
            stepIndex = stepIndex,
            actorId = activePlayer.id,
            actorType = activePlayer.type.name,
            myScore = activePlayer.score,
            bestOpponentScore = bestOpponentScore,
            turnScoreBefore = stateBeforeAction.turn.bankedInTurn + keptFaces.let { faces ->
                fr.antoinehory.neon5k.domain.rules.DiceScorer.evaluateSelection(faces, stateBeforeAction.rules).totalPoints
            },
            remainingDiceBefore = rolledFaces.size,
            isOpened = activePlayer.isOpened,
            consecutiveBusts = activePlayer.consecutiveBusts,
            rolledDiceCsv = rolledFaces.joinToString(","),
            keptDiceCsv = keptFaces.joinToString(","),
            decision = decision
        )
        decisionLogDao.insertDecision(entity)
    }

    /**
     * Met à jour rétroactivement `turnFinalPoints` pour toutes les décisions du tour qui vient de se terminer.
     */
    suspend fun finalizeTurnDecisionLogs(matchUuid: String, turnNumber: Int, finalPoints: Int) {
        decisionLogDao.updateTurnOutcome(
            matchUuid = matchUuid,
            turnIndex = turnNumber,
            finalPoints = finalPoints
        )
    }

    /**
     * Met à jour rétroactivement `matchWon` pour toutes les décisions de la partie terminée.
     */
    suspend fun finalizeMatchDecisionLogs(matchUuid: String, winnerActorId: Int) {
        decisionLogDao.updateMatchOutcome(
            matchUuid = matchUuid,
            winnerActorId = winnerActorId
        )
    }

    /**
     * Récupère l'intégralité des enregistrements du dataset IA pour l'export SAF.
     */
    suspend fun getAllDecisionLogsForExport(): List<DecisionLogEntity> {
        return decisionLogDao.getAllDecisions()
    }

    /**
     * Vide les journaux de décisions IA locaux et réinitialise le modèle auto-apprenant.
     */
    suspend fun clearAllDecisionLogs() {
        decisionLogDao.clearAllDecisions()
        refreshAdaptiveAiModel()
    }
}
