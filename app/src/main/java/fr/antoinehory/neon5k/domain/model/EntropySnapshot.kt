package fr.antoinehory.neon5k.domain.model

/**
 * Échantillon brut des sources d'entropie matérielles et logicielles capturées au moment d'un lancer.
 * Pur Kotlin (aucun import Android).
 */
data class EntropyRawSample(
    val gyroX: Float? = null,
    val gyroY: Float? = null,
    val gyroZ: Float? = null,
    val accelX: Float? = null,
    val accelY: Float? = null,
    val accelZ: Float? = null,
    val epochMillis: Long,
    val nanoTime: Long,
    val uptimeNanos: Long,
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val pressDurationMs: Long = 0L,
    val rollCounter: Int = 1,
    val batteryLevel: Int? = null,
    val secureSaltHex: String,
    val triggeredByShake: Boolean = false
) {
    /**
     * Sérialisation canonique déterministe UTF-8 hachée par EntropySeedGenerator.
     */
    fun toCanonicalString(): String = buildString {
        append("gyro=").append(gyroX ?: "null").append(',').append(gyroY ?: "null").append(',').append(gyroZ ?: "null")
        append("|accel=").append(accelX ?: "null").append(',').append(accelY ?: "null").append(',').append(accelZ ?: "null")
        append("|epoch=").append(epochMillis)
        append("|nano=").append(nanoTime)
        append("|uptime=").append(uptimeNanos)
        append("|touch=").append(touchX).append(',').append(touchY).append(':').append(pressDurationMs)
        append("|roll=").append(rollCounter)
        append("|bat=").append(batteryLevel ?: "null")
        append("|salt=").append(secureSaltHex)
        append("|shake=").append(if (triggeredByShake) 1 else 0)
    }
}

/**
 * Trace immuable et lisible d'un tirage d'entropie, attachée à chaque lancer pour l'UI et l'auditabilité.
 */
data class EntropySnapshot(
    val fullHashHex: String,
    val shortHashHex: String,
    val seed: Long,
    val hasGyroscope: Boolean,
    val hasAccelerometer: Boolean,
    val triggeredByShake: Boolean,
    val gyroSummary: String,
    val accelSummary: String,
    val timingSummary: String,
    val touchSummary: String,
    val saltHex: String,
    val rollCounter: Int
)
