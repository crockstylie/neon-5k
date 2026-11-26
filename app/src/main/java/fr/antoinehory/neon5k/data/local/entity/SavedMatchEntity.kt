package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PiggybackOffer
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.model.TurnState
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.FiveOfAKindMode
import fr.antoinehory.neon5k.domain.rules.FourOfAKindMode
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.RulesPreset
import org.json.JSONArray
import org.json.JSONObject

/**
 * Entité Room représentant une partie sauvegardée (en cours pour reprise automatique ou terminée dans l'historique).
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
)

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
        put("exactFinishRequired", rules.exactFinishRequired)
        put("mandatoryRerollOnHotDice", rules.mandatoryRerollOnHotDice)
        put("fourOfAKindMode", rules.fourOfAKindMode.name)
        put("fiveOfAKindMode", rules.fiveOfAKindMode.name)
        put("straightScore", rules.straightScore)
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
    val rulesObj = JSONObject(rulesJson)
    val parsedThreeStrikesMode = rulesObj.optString("threeStrikesMode", "")
        .takeIf { it.isNotEmpty() }
        ?.let { runCatching { fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode.valueOf(it) }.getOrNull() }
        ?: when (rulesObj.optInt("threeStrikesPenalty", 0)) {
            500 -> fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode.MINUS_500
            1000 -> fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode.MINUS_1000
            else -> fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode.DISABLED
        }

    val rules = GameRulesConfig(
        preset = RulesPreset.valueOf(rulesObj.optString("preset", RulesPreset.CLASSIC_5_DICE.name)),
        diceCount = rulesObj.optInt("diceCount", 5),
        targetScore = rulesObj.optInt("targetScore", 5000),
        openingThreshold = rulesObj.optInt("openingThreshold", 750),
        exactFinishRequired = rulesObj.optBoolean("exactFinishRequired", true),
        mandatoryRerollOnHotDice = rulesObj.optBoolean("mandatoryRerollOnHotDice", true),
        fourOfAKindMode = FourOfAKindMode.valueOf(rulesObj.optString("fourOfAKindMode", FourOfAKindMode.DOUBLE_TRIPLE.name)),
        fiveOfAKindMode = FiveOfAKindMode.valueOf(rulesObj.optString("fiveOfAKindMode", FiveOfAKindMode.QUAD_TRIPLE.name)),
        straightScore = rulesObj.optInt("straightScore", 1500),
        threePairsScore = rulesObj.optInt("threePairsScore", 1000),
        fullHouseBonus = rulesObj.optInt("fullHouseBonus", 0),
        piggybackEnabled = rulesObj.optBoolean("piggybackEnabled", false),
        bumpOpponentMode = BumpOpponentMode.valueOf(rulesObj.optString("bumpOpponentMode", BumpOpponentMode.DISABLED.name)),
        threeStrikesMode = parsedThreeStrikesMode
    )

    val playersArr = JSONArray(playersJson)
    val players = List(playersArr.length()) { idx ->
        val obj = playersArr.getJSONObject(idx)
        val cpArr = obj.optJSONArray("checkpoints")
        val checkpoints = if (cpArr != null) {
            List(cpArr.length()) { cpIdx ->
                val cpObj = cpArr.getJSONObject(cpIdx)
                fr.antoinehory.neon5k.domain.model.ScoreCheckpoint(
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
            type = PlayerType.valueOf(obj.optString("type", PlayerType.HUMAN.name)),
            score = obj.optInt("score", 0),
            previousScore = obj.optInt("previousScore", 0),
            isOpened = obj.optBoolean("isOpened", false),
            consecutiveBusts = obj.optInt("consecutiveBusts", 0),
            checkpoints = checkpoints
        )
    }

    val turnObj = JSONObject(turnJson)
    val diceArr = turnObj.getJSONArray("dice")
    val dice = List(diceArr.length()) { idx ->
        val d = diceArr.getJSONObject(idx)
        DieState(
            index = d.getInt("index"),
            face = d.getInt("face"),
            isLocked = d.optBoolean("isLocked", false),
            isSelected = d.optBoolean("isSelected", false),
            canScoreOnCurrentRoll = d.optBoolean("canScoreOnCurrentRoll", false)
        )
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
        activePlayerIndex = turnObj.optInt("activePlayerIndex", 0),
        phase = TurnPhase.valueOf(turnObj.optString("phase", TurnPhase.AWAITING_INITIAL_ROLL.name)),
        dice = dice,
        bankedInTurn = turnObj.optInt("bankedInTurn", 0),
        selectedPoints = eval?.totalPoints ?: turnObj.optInt("selectedPoints", 0),
        selectedCombinations = eval?.combinations ?: emptyList(),
        isSelectionValid = eval?.isValid ?: turnObj.optBoolean("isSelectionValid", false),
        rollIndexInTurn = turnObj.optInt("rollIndexInTurn", 0),
        hotDiceCountInTurn = turnObj.optInt("hotDiceCountInTurn", 0),
        piggybackOffer = piggybackOffer,
        usedPiggyback = turnObj.optBoolean("usedPiggyback", false),
        statusMessageFr = turnObj.optString("statusMessageFr", "")
    )

    return MatchState(
        matchId = matchUuid,
        rules = rules,
        players = players,
        turn = turn,
        winnerPlayerId = winnerPlayerId,
        isInstantWin = isInstantWin,
        totalRollsInMatch = totalRollsInMatch
    )
}
