package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.antoinehory.neon5k.domain.model.DecisionRecord

/**
 * Entité Room représentant un vecteur `(état, action, issue)` enregistré à chaque choix de dés (Humain ou Bot)
 * pour l'entraînement futur de modèles d'IA au 5000.
 */
@Entity(
    tableName = "decision_logs",
    indices = [
        Index(value = ["matchUuid", "turnIndex"]),
        Index(value = ["rulesFingerprint"])
    ]
)
data class DecisionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val matchUuid: String,
    val rulesFingerprint: String,
    val turnIndex: Int,
    val stepIndex: Int,
    val actorId: Int,
    val actorType: String,
    val myScore: Int,
    val bestOpponentScore: Int,
    val turnScoreBefore: Int,
    val remainingDiceBefore: Int,
    val isOpened: Boolean,
    val consecutiveBusts: Int,
    val rolledDiceCsv: String,
    val keptDiceCsv: String,
    val decision: String,
    val turnFinalPoints: Int? = null,
    val matchWon: Boolean? = null,
    val uploadedToCloud: Boolean = false
)

fun DecisionLogEntity.toDomain(): DecisionRecord = DecisionRecord(
    matchUuid = matchUuid,
    rulesFingerprint = rulesFingerprint,
    turnIndex = turnIndex,
    stepIndex = stepIndex,
    actorType = actorType,
    myScore = myScore,
    bestOpponentScore = bestOpponentScore,
    turnScoreBefore = turnScoreBefore,
    remainingDiceBefore = remainingDiceBefore,
    isOpened = isOpened,
    consecutiveBusts = consecutiveBusts,
    rolledDiceCsv = rolledDiceCsv,
    keptDiceCsv = keptDiceCsv,
    decision = decision,
    turnFinalPoints = turnFinalPoints,
    matchWon = matchWon
)
