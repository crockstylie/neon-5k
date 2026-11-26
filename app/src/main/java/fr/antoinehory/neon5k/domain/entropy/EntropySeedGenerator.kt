package fr.antoinehory.neon5k.domain.entropy

import fr.antoinehory.neon5k.domain.model.EntropyRawSample
import fr.antoinehory.neon5k.domain.model.EntropySnapshot
import java.security.MessageDigest
import java.util.Locale
import kotlin.random.Random

/**
 * Résultat complet d'un tirage de dés produit à partir d'un échantillon d'entropie hybride.
 */
data class EntropyRollOutcome(
    val faces: List<Int>,
    val snapshot: EntropySnapshot
)

/**
 * Générateur déterministe de graine (`SHA-256`) et de tirages uniformes de dés (`1..6`).
 * Pur Kotlin / JVM standard (aucun import `android.*`), 100 % testable sur JVM.
 */
object EntropySeedGenerator {

    /**
     * Hache un [EntropyRawSample] en `SHA-256`, extrait les 8 premiers octets en `seed: Long` (Big-Endian),
     * tire uniformément `diceCount` dés dans `1..6` et construit la trace [EntropySnapshot].
     */
    fun generateRoll(
        sample: EntropyRawSample,
        diceCount: Int
    ): EntropyRollOutcome {
        require(diceCount in 1..6) { "diceCount must be between 1 and 6 (was $diceCount)" }

        val canonical = sample.toCanonicalString()
        val digestBytes = sha256Bytes(canonical)
        val fullHashHex = bytesToHexUpper(digestBytes)
        val shortHashHex = "#" + fullHashHex.take(6)
        val seed = firstEightBytesToLong(digestBytes)

        val random = Random(seed)
        val faces = List(diceCount) { random.nextInt(1, 7) }

        val hasGyro = sample.gyroX != null && sample.gyroY != null && sample.gyroZ != null
        val hasAccel = sample.accelX != null && sample.accelY != null && sample.accelZ != null

        val gyroSummary = if (hasGyro) {
            String.format(
                Locale.US,
                "X=%.3f, Y=%.3f, Z=%.3f rad/s",
                sample.gyroX,
                sample.gyroY,
                sample.gyroZ
            )
        } else {
            "Capteur absent (fallback logiciel)"
        }

        val accelSummary = if (hasAccel) {
            String.format(
                Locale.US,
                "X=%.2f, Y=%.2f, Z=%.2f m/s²",
                sample.accelX,
                sample.accelY,
                sample.accelZ
            )
        } else {
            "Capteur absent (fallback logiciel)"
        }

        val timingSummary = "t=${sample.epochMillis}ms | Δnano=${sample.nanoTime % 1_000_000_000L}ns"
        val touchSummary = if (sample.triggeredByShake) {
            "Déclenché par SECOUSSE physique"
        } else {
            String.format(
                Locale.US,
                "(%.0f, %.0f) px | appui %d ms",
                sample.touchX,
                sample.touchY,
                sample.pressDurationMs
            )
        }

        val snapshot = EntropySnapshot(
            fullHashHex = fullHashHex,
            shortHashHex = shortHashHex,
            seed = seed,
            hasGyroscope = hasGyro,
            hasAccelerometer = hasAccel,
            triggeredByShake = sample.triggeredByShake,
            gyroSummary = gyroSummary,
            accelSummary = accelSummary,
            timingSummary = timingSummary,
            touchSummary = touchSummary,
            saltHex = sample.secureSaltHex,
            rollCounter = sample.rollCounter
        )

        return EntropyRollOutcome(
            faces = faces,
            snapshot = snapshot
        )
    }

    /**
     * Calcule l'empreinte SHA-256 (32 octets) d'une chaîne UTF-8.
     */
    fun sha256Bytes(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.UTF_8))
    }

    /**
     * Convertit les 8 premiers octets d'un tableau en un `Long` 64 bits (Big-Endian).
     */
    fun firstEightBytesToLong(bytes: ByteArray): Long {
        require(bytes.size >= 8) { "ByteArray must have at least 8 bytes (had ${bytes.size})" }
        var result = 0L
        for (i in 0 until 8) {
            result = (result shl 8) or (bytes[i].toLong() and 0xFFL)
        }
        return result
    }

    private fun bytesToHexUpper(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xFF
            sb.append(hexChars[v ushr 4])
            sb.append(hexChars[v and 0x0F])
        }
        return sb.toString()
    }
}
