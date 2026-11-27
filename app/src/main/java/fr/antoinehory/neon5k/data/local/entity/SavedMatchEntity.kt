package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PiggybackOffer
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.RollHistoryEntry
import fr.antoinehory.neon5k.domain.model.ScoreCheckpoint
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.model.TurnState
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.FiveOfAKindMode
import fr.antoinehory.neon5k.domain.rules.FourOfAKindMode
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.RulesPreset
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entité Room représentant une partie sauvegardée (en cours pour reprise automatique ou terminée dans l'historique).
 * Conserve strictement les 10 colonnes du schéma v2 (`rulesJson`, `playersJson`, `turnJson`) pour une
 * compatibilité 100 % transparente avec toutes les parties et statistiques existantes.
 */
@Entity(tableName = "saved_matches")
data class SavedMatchEntity(
    @PrimaryKey
    val matchUuid: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val isFinished: Boolean,
    val winnerPlayerId: Int?,
    val isInstantWin: Boolean,
    val totalRollsInMatch: Int,
    val rulesJson: String,
    val playersJson: String,
    val turnJson: String
) {
    /** Numéro de tour courant décodé depuis `turnJson`. */
    val turnNumber: Int
        get() = runCatching { JSONObject(turnJson).optInt("turnNumber", 1) }.getOrDefault(1)

    /** Liste des joueurs décodée pour l'affichage dans l'onglet Historique des parties. */
    fun decodedPlayers(): List<PlayerState> = runCatching {
        decodePlayersFromJson(playersJson)
    }.getOrDefault(emptyList())

    /** Règles décodées pour l'affichage dans l'onglet Historique des parties. */
    fun decodedRules(): GameRulesConfig = runCatching {
        decodeRulesFromJson(rulesJson)
    }.getOrDefault(GameRulesConfig.CLASSIC_5_DICE)

    /** Journal des lancers décodé depuis `turnJson` pour l'affichage dans l'onglet Historique des parties. */
    fun decodedRollHistory(): List<RollHistoryEntry> = runCatching {
        val turnObj = JSONObject(turnJson)
        decodeRollHistoryFromJson(turnObj.optJSONArray("rollHistoryLog"))
    }.getOrDefault(emptyList())

    companion object {
        fun fromDomain(
            state: MatchState,
            createdAtEpochMs: Long = System.currentTimeMillis(),
            updatedAtEpochMs: Long = System.currentTimeMillis()
        ): SavedMatchEntity = state.toEntity(
            createdAtEpochMs = createdAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs
        )

        fun toDomain(entity: SavedMatchEntity): MatchState = entity.toDomain()
    }
}

/**
 * Convertit un [MatchState] du domaine en [SavedMatchEntity] Room.
 */
fun MatchState.toEntity(
    createdAtEpochMs: Long = System.currentTimeMillis(),
    updatedAtEpochMs: Long = System.currentTimeMillis()
): SavedMatchEntity {
    val rulesObj = JSONObject().apply {
        put("preset", rules.preset.name)
        put("diceCount", rules.diceCount)
        put("targetScore", rules.targetScore)
        put("openingThreshold", rules.openingThreshold)
        put("minTurnScoreAfterOpening", rules.minTurnScoreAfterOpening)
        put("requireMultipleOf100ToBank", rules.requireMultipleOf100ToBank)
        put("finalSprintLast1000InOneTurn", rules.finalSprintLast1000InOneTurn)
        put("exactFinishRequired", rules.exactFinishRequired)
        put("mandatoryRerollOnHotDice", rules.mandatoryRerollOnHotDice)
        put("fourOfAKindMode", rules.fourOfAKindMode.name)
        put("fiveOfAKindMode", rules.fiveOfAKindMode.name)
        put("instantWinOnFiveOnes", rules.instantWinOnFiveOnes)
        put("straightScore", rules.straightScore)
        put("fullStraight6DiceScore", rules.fullStraight6DiceScore)
        put("threePairsScore", rules.threePairsScore)
        put("fullHouseBonus", rules.fullHouseBonus)
        put("piggybackEnabled", rules.piggybackEnabled)
        put("bumpOpponentMode", rules.bumpOpponentMode.name)
        put("threeStrikesMode", rules.threeStrikesMode.name)
    }

    val playersArr = JSONArray()
    for (p in players) {
        val checkpointsArr = JSONArray()
        for (cp in p.normalizedCheckpoints()) {
            checkpointsArr.put(
                JSONObject().apply {
                    put("score", cp.score)
                    put("strikes", cp.strikes)
                }
            )
        }
        playersArr.put(
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("type", p.type.name)
                put("score", p.score)
                put("previousScore", p.previousScore)
                put("isOpened", p.isOpened)
                put("consecutiveBusts", p.consecutiveBusts)
                put("checkpoints", checkpointsArr)
            }
        )
    }

    val diceArr = JSONArray()
    for (d in turn.dice) {
        diceArr.put(
            JSONObject().apply {
                put("index", d.index)
                put("face", d.face)
                put("isLocked", d.isLocked)
                put("isSelected", d.isSelected)
                put("canScoreOnCurrentRoll", d.canScoreOnCurrentRoll)
            }
        )
    }

    val rollHistoryArr = JSONArray()
    for (entry in rollHistoryLog) {
        val facesArr = JSONArray()
        entry.rolledFaces.forEach { facesArr.put(it) }
        rollHistoryArr.put(
            JSONObject().apply {
                put("turnNumber", entry.turnNumber)
                put("rollNumber", entry.rollNumber)
                put("playerId", entry.playerId)
                put("playerName", entry.playerName)
                put("rolledFaces", facesArr)
                put("summaryFr", entry.summaryFr)
                put("playerScoreAfter", entry.playerScoreAfter)
                put("playerStrikesAfter", entry.playerStrikesAfter)
                put("isBust", entry.isBust)
                put("isBank", entry.isBank)
                put("isHotDice", entry.isHotDice)
            }
        )
    }

    val turnObj = JSONObject().apply {
        put("turnNumber", turn.turnNumber)
        put("activePlayerIndex", turn.activePlayerIndex)
        put("phase", turn.phase.name)
        put("dice", diceArr)
        put("bankedInTurn", turn.bankedInTurn)
        put("selectedPoints", turn.selectedPoints)
        put("isSelectionValid", turn.isSelectionValid)
        put("rollIndexInTurn", turn.rollIndexInTurn)
        put("hotDiceCountInTurn", turn.hotDiceCountInTurn)
        put("usedPiggyback", turn.usedPiggyback)
        put("statusMessageFr", turn.statusMessageFr)
        put("rollHistoryLog", rollHistoryArr)
        turn.piggybackOffer?.let { offer ->
            put(
                "piggybackOffer",
                JSONObject().apply {
                    put("inheritedPoints", offer.inheritedPoints)
                    put("remainingDiceCount", offer.remainingDiceCount)
                    put("fromPlayerName", offer.fromPlayerName)
                }
            )
        }
    }

    return SavedMatchEntity(
        matchUuid = matchId,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs,
        isFinished = isFinished,
        winnerPlayerId = winnerPlayerId,
        isInstantWin = isInstantWin,
        totalRollsInMatch = totalRollsInMatch,
        rulesJson = rulesObj.toString(),
        playersJson = playersArr.toString(),
        turnJson = turnObj.toString()
    )
}

/**
 * Reconstruit le [MatchState] du domaine depuis une [SavedMatchEntity] Room.
 */
fun SavedMatchEntity.toDomain(): MatchState {
    val rules = decodeRulesFromJson(rulesJson)
    val players = decodePlayersFromJson(playersJson)

    val turnObj = JSONObject(turnJson)
    val phase = runCatching {
        TurnPhase.valueOf(turnObj.optString("phase", TurnPhase.AWAITING_INITIAL_ROLL.name))
    }.getOrDefault(TurnPhase.AWAITING_INITIAL_ROLL)

    val diceArr = turnObj.optJSONArray("dice") ?: JSONArray()
    val rawDice = List(diceArr.length()) { idx ->
        val d = diceArr.getJSONObject(idx)
        DieState(
            index = d.getInt("index"),
            face = d.getInt("face"),
            isLocked = d.optBoolean("isLocked", false),
            isSelected = d.optBoolean("isSelected", false),
            canScoreOnCurrentRoll = d.optBoolean("canScoreOnCurrentRoll", false)
        )
    }

    // Recalcul dynamique de canScoreOnCurrentRoll pour auto-réparer toute partie sauvegardée en phase SELECTING_DICE
    val dice = if (phase == TurnPhase.SELECTING_DICE) {
        val freeIndices = rawDice.mapIndexedNotNull { idx, die ->
            if (!die.isLocked && die.face in 1..6) idx else null
        }
        val freeFaces = freeIndices.map { rawDice[it].face }
        val scoringFreePositions = DiceScorer.findScoringDieIndices(freeFaces, rules)
        val scoringDieIndices = scoringFreePositions.map { pos -> freeIndices[pos] }.toSet()
        rawDice.mapIndexed { idx, die ->
            if (die.isLocked) {
                die.copy(isSelected = false, canScoreOnCurrentRoll = false)
            } else {
                val canScore = idx in scoringDieIndices
                die.copy(
                    canScoreOnCurrentRoll = canScore,
                    isSelected = die.isSelected && canScore
                )
            }
        }
    } else {
        rawDice
    }

    val selectedFaces = dice.filter { !it.isLocked && it.isSelected && it.face in 1..6 }.map { it.face }
    val eval = if (selectedFaces.isNotEmpty()) DiceScorer.evaluateSelection(selectedFaces, rules) else null

    val piggyObj = turnObj.optJSONObject("piggybackOffer")
    val piggybackOffer = piggyObj?.let {
        PiggybackOffer(
            inheritedPoints = it.getInt("inheritedPoints"),
            remainingDiceCount = it.getInt("remainingDiceCount"),
            fromPlayerName = it.getString("fromPlayerName")
        )
    }

    val turn = TurnState(
        turnNumber = turnObj.optInt("turnNumber", 1),
        activePlayerIndex = turnObj.optInt("activePlayerIndex", 0).coerceIn(0, (players.size - 1).coerceAtLeast(0)),
        phase = phase,
        dice = dice,
        bankedInTurn = turnObj.optInt("bankedInTurn", 0),
        selectedPoints = eval?.totalPoints ?: 0,
        selectedCombinations = eval?.combinations ?: emptyList(),
        isSelectionValid = eval?.isValid ?: false,
        rollIndexInTurn = turnObj.optInt("rollIndexInTurn", 0),
        hotDiceCountInTurn = turnObj.optInt("hotDiceCountInTurn", 0),
        piggybackOffer = piggybackOffer,
        usedPiggyback = turnObj.optBoolean("usedPiggyback", false),
        statusMessageFr = turnObj.optString("statusMessageFr", "")
    )

    val rollHistoryLog = decodeRollHistoryFromJson(turnObj.optJSONArray("rollHistoryLog"))

    return MatchState(
        matchId = matchUuid,
        rules = rules,
        players = players,
        turn = turn,
        winnerPlayerId = winnerPlayerId,
        isInstantWin = isInstantWin,
        totalRollsInMatch = totalRollsInMatch,
        rollHistoryLog = rollHistoryLog
    )
}

private fun decodeRulesFromJson(rulesJson: String): GameRulesConfig {
    val rulesObj = JSONObject(rulesJson)
    val parsedThreeStrikesMode = rulesObj.optString("threeStrikesMode", "")
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { ThreeStrikesMode.valueOf(it) }.getOrNull() }
        ?: when (rulesObj.optInt("threeStrikesPenalty", 0)) {
            500 -> ThreeStrikesMode.MINUS_500
            1000 -> ThreeStrikesMode.MINUS_1000
            else -> ThreeStrikesMode.DISABLED
        }

    val rawFiveKind = runCatching {
        FiveOfAKindMode.valueOf(rulesObj.optString("fiveOfAKindMode", FiveOfAKindMode.QUAD_TRIPLE.name))
    }.getOrDefault(FiveOfAKindMode.QUAD_TRIPLE)

    val instantWinOnes = if (rulesObj.has("instantWinOnFiveOnes")) {
        rulesObj.optBoolean("instantWinOnFiveOnes", false)
    } else {
        rawFiveKind == FiveOfAKindMode.INSTANT_WIN_ON_ONES
    }

    return GameRulesConfig(
        preset = runCatching {
            RulesPreset.valueOf(rulesObj.optString("preset", RulesPreset.CLASSIC_5_DICE.name))
        }.getOrDefault(RulesPreset.CLASSIC_5_DICE),
        diceCount = rulesObj.optInt("diceCount", 5).coerceIn(5, 6),
        targetScore = rulesObj.optInt("targetScore", 5000).coerceAtLeast(1000),
        openingThreshold = rulesObj.optInt("openingThreshold", 750).coerceAtLeast(0),
        minTurnScoreAfterOpening = rulesObj.optInt("minTurnScoreAfterOpening", 0).coerceAtLeast(0),
        requireMultipleOf100ToBank = rulesObj.optBoolean("requireMultipleOf100ToBank", false),
        finalSprintLast1000InOneTurn = rulesObj.optBoolean("finalSprintLast1000InOneTurn", false),
        exactFinishRequired = rulesObj.optBoolean("exactFinishRequired", true),
        mandatoryRerollOnHotDice = rulesObj.optBoolean("mandatoryRerollOnHotDice", true),
        fourOfAKindMode = runCatching {
            FourOfAKindMode.valueOf(rulesObj.optString("fourOfAKindMode", FourOfAKindMode.DOUBLE_TRIPLE.name))
        }.getOrDefault(FourOfAKindMode.DOUBLE_TRIPLE),
        fiveOfAKindMode = if (rawFiveKind == FiveOfAKindMode.INSTANT_WIN_ON_ONES) {
            FiveOfAKindMode.QUAD_TRIPLE
        } else {
            rawFiveKind
        },
        instantWinOnFiveOnes = instantWinOnes,
        straightScore = rulesObj.optInt("straightScore", 1500),
        fullStraight6DiceScore = rulesObj.optInt("fullStraight6DiceScore", 2500),
        threePairsScore = rulesObj.optInt("threePairsScore", 1000),
        fullHouseBonus = rulesObj.optInt("fullHouseBonus", 0),
        piggybackEnabled = rulesObj.optBoolean("piggybackEnabled", false),
        bumpOpponentMode = runCatching {
            BumpOpponentMode.valueOf(rulesObj.optString("bumpOpponentMode", BumpOpponentMode.DISABLED.name))
        }.getOrDefault(BumpOpponentMode.DISABLED),
        threeStrikesMode = parsedThreeStrikesMode
    )
}

private fun decodePlayersFromJson(playersJson: String): List<PlayerState> {
    val playersArr = JSONArray(playersJson)
    return List(playersArr.length()) { idx ->
        val obj = playersArr.getJSONObject(idx)
        val cpArr = obj.optJSONArray("checkpoints")
        val checkpoints = if (cpArr != null) {
            List(cpArr.length()) { cpIdx ->
                val cpObj = cpArr.getJSONObject(cpIdx)
                ScoreCheckpoint(
                    score = cpObj.optInt("score", 0),
                    strikes = cpObj.optInt("strikes", 0)
                )
            }
        } else {
            emptyList()
        }
        PlayerState(
            id = obj.getInt("id"),
            name = obj.getString("name"),
            type = runCatching {
                PlayerType.valueOf(obj.optString("type", PlayerType.HUMAN.name))
            }.getOrDefault(PlayerType.HUMAN),
            score = obj.optInt("score", 0),
            previousScore = obj.optInt("previousScore", 0),
            isOpened = obj.optBoolean("isOpened", false),
            consecutiveBusts = obj.optInt("consecutiveBusts", 0),
            checkpoints = checkpoints
        )
    }
}

private fun decodeRollHistoryFromJson(arr: JSONArray?): List<RollHistoryEntry> {
    if (arr == null) return emptyList()
    return List(arr.length()) { idx ->
        val obj = arr.getJSONObject(idx)
        val facesArr = obj.optJSONArray("rolledFaces")
        val faces = if (facesArr != null) {
            List(facesArr.length()) { fIdx -> facesArr.optInt(fIdx, 1) }
        } else {
            emptyList()
        }
        RollHistoryEntry(
            turnNumber = obj.optInt("turnNumber", 1),
            rollNumber = obj.optInt("rollNumber", 1),
            playerId = obj.optInt("playerId", 0),
            playerName = obj.optString("playerName", ""),
            rolledFaces = faces,
            summaryFr = obj.optString("summaryFr", ""),
            playerScoreAfter = obj.optInt("playerScoreAfter", 0),
            playerStrikesAfter = obj.optInt("playerStrikesAfter", 0),
            isBust = obj.optBoolean("isBust", false),
            isBank = obj.optBoolean("isBank", false),
            isHotDice = obj.optBoolean("isHotDice", false)
        )
    }
}
