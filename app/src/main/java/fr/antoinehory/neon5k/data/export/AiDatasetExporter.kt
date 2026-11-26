package fr.antoinehory.neon5k.data.export

import android.content.Context
import android.net.Uri
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter

/**
 * Format d'export du dataset IA supporté par le Storage Access Framework (SAF).
 */
enum class DatasetExportFormat(
    val extension: String,
    val mimeType: String
) {
    JSONL(extension = "jsonl", mimeType = "application/jsonl"),
    CSV(extension = "csv", mimeType = "text/csv")
}

/**
 * Résultat typé d'un export SAF de dataset IA.
 */
sealed interface DatasetExportResult {
    data class Success(val exportedCount: Int, val format: DatasetExportFormat) : DatasetExportResult
    data class Error(val messageFr: String) : DatasetExportResult
}

/**
 * Exporteur du journal des décisions (`decision_logs`) en `.jsonl` ou `.csv` via le Storage Access Framework (SAF).
 * Ne nécessite aucune permission de stockage Android (`WRITE_EXTERNAL_STORAGE` proscrit).
 */
class AiDatasetExporter(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun exportToUri(
        uri: Uri,
        format: DatasetExportFormat,
        logs: List<DecisionLogEntity>
    ): DatasetExportResult = withContext(ioDispatcher) {
        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext DatasetExportResult.Error("Impossible d'ouvrir le fichier cible SAF.")

            outputStream.use { stream ->
                BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { writer ->
                    when (format) {
                        DatasetExportFormat.JSONL -> writeJsonLines(writer, logs)
                        DatasetExportFormat.CSV -> writeCsv(writer, logs)
                    }
                    writer.flush()
                }
            }
            DatasetExportResult.Success(exportedCount = logs.size, format = format)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DatasetExportResult.Error("Échec de l'export : ${e.localizedMessage ?: "erreur E/S"}")
        }
    }

    companion object {
        const val CSV_HEADER =
            "id,matchUuid,rulesFingerprint,turnIndex,stepIndex,actorId,actorType,myScore,bestOpponentScore,turnScoreBefore,remainingDiceBefore,isOpened,consecutiveBusts,rolledDiceCsv,keptDiceCsv,decision,turnFinalPoints,matchWon"

        fun formatAsJsonLine(log: DecisionLogEntity): String {
            return JSONObject().apply {
                put("id", log.id)
                put("matchUuid", log.matchUuid)
                put("rulesFingerprint", log.rulesFingerprint)
                put("turnIndex", log.turnIndex)
                put("stepIndex", log.stepIndex)
                put("actorId", log.actorId)
                put("actorType", log.actorType)
                put("myScore", log.myScore)
                put("bestOpponentScore", log.bestOpponentScore)
                put("turnScoreBefore", log.turnScoreBefore)
                put("remainingDiceBefore", log.remainingDiceBefore)
                put("isOpened", log.isOpened)
                put("consecutiveBusts", log.consecutiveBusts)
                put("rolledDiceCsv", log.rolledDiceCsv)
                put("keptDiceCsv", log.keptDiceCsv)
                put("decision", log.decision)
                put("turnFinalPoints", log.turnFinalPoints ?: JSONObject.NULL)
                put("matchWon", log.matchWon ?: JSONObject.NULL)
            }.toString()
        }

        fun formatAsCsvRow(log: DecisionLogEntity): String {
            return buildString {
                append(log.id).append(',')
                append(log.matchUuid).append(',')
                append(log.rulesFingerprint).append(',')
                append(log.turnIndex).append(',')
                append(log.stepIndex).append(',')
                append(log.actorId).append(',')
                append(log.actorType).append(',')
                append(log.myScore).append(',')
                append(log.bestOpponentScore).append(',')
                append(log.turnScoreBefore).append(',')
                append(log.remainingDiceBefore).append(',')
                append(log.isOpened).append(',')
                append(log.consecutiveBusts).append(',')
                append('"').append(log.rolledDiceCsv).append("\",")
                append('"').append(log.keptDiceCsv).append("\",")
                append(log.decision).append(',')
                append(log.turnFinalPoints?.toString().orEmpty()).append(',')
                append(log.matchWon?.toString().orEmpty())
            }
        }

        private fun writeJsonLines(writer: BufferedWriter, logs: List<DecisionLogEntity>) {
            for (log in logs) {
                writer.write(formatAsJsonLine(log))
                writer.newLine()
            }
        }

        private fun writeCsv(writer: BufferedWriter, logs: List<DecisionLogEntity>) {
            writer.write(CSV_HEADER)
            writer.newLine()
            for (log in logs) {
                writer.write(formatAsCsvRow(log))
                writer.newLine()
            }
        }
    }
}
