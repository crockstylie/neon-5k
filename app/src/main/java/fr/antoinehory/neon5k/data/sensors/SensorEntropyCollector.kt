package fr.antoinehory.neon5k.data.sensors

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.SystemClock
import fr.antoinehory.neon5k.domain.entropy.EntropyRollOutcome
import fr.antoinehory.neon5k.domain.entropy.EntropySeedGenerator
import fr.antoinehory.neon5k.domain.model.EntropyRawSample
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.SecureRandom
import kotlin.math.sqrt

/**
 * Collecteur non-bloquant d'entropie matérielle et logicielle pour les tirages de dés.
 *
 * - Écoute `TYPE_GYROSCOPE` et `TYPE_ACCELEROMETER` à `SENSOR_DELAY_UI` uniquement entre [startListening] et [stopListening].
 * - Gère gracieusement l'absence de gyroscope ou d'accéléromètre (fallback logiciel silencieux).
 * - Détecte une secousse physique franche (`gForce > 2.25f` avec anti-rebond `750ms`) et l'émet sur [shakeEvents].
 */
class SensorEntropyCollector(
    context: Context
) : SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val gyroscopeSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometerSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val secureRandom = SecureRandom()

    @Volatile
    private var isListening: Boolean = false

    @Volatile
    private var latestGyro: FloatArray? = null

    @Volatile
    private var latestAccel: FloatArray? = null

    @Volatile
    private var lastShakeTimestampMs: Long = 0L

    private val _shakeEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shakeEvents: SharedFlow<Unit> = _shakeEvents.asSharedFlow()

    val hasHardwareGyroscope: Boolean
        get() = gyroscopeSensor != null

    val hasHardwareAccelerometer: Boolean
        get() = accelerometerSensor != null

    /**
     * Démarre l'écoute des capteurs disponibles (`ON_RESUME` de `GameScreen`).
     */
    fun startListening() {
        if (isListening || sensorManager == null) return
        gyroscopeSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        accelerometerSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        isListening = true
    }

    /**
     * Coupe immédiatement l'écoute des capteurs (`ON_PAUSE` / `onCleared`) pour garantir zéro drain batterie en arrière-plan.
     */
    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    /**
     * Capture un échantillon instantané d'entropie hybride et génère un lancer de `diceCount` dés (`1..6`).
     */
    fun captureAndRoll(
        diceCount: Int,
        rollCounter: Int,
        touchX: Float = 0f,
        touchY: Float = 0f,
        pressDurationMs: Long = 0L,
        triggeredByShake: Boolean = false
    ): EntropyRollOutcome {
        val gyro = latestGyro
        val accel = latestAccel

        val saltBytes = ByteArray(4)
        secureRandom.nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02X".format(it) }

        val sample = EntropyRawSample(
            gyroX = gyro?.getOrNull(0),
            gyroY = gyro?.getOrNull(1),
            gyroZ = gyro?.getOrNull(2),
            accelX = accel?.getOrNull(0),
            accelY = accel?.getOrNull(1),
            accelZ = accel?.getOrNull(2),
            epochMillis = System.currentTimeMillis(),
            nanoTime = System.nanoTime(),
            uptimeNanos = SystemClock.elapsedRealtimeNanos(),
            touchX = touchX,
            touchY = touchY,
            pressDurationMs = pressDurationMs,
            rollCounter = rollCounter,
            batteryLevel = readBatteryPercentage(),
            secureSaltHex = saltHex,
            triggeredByShake = triggeredByShake
        )
        return EntropySeedGenerator.generateRoll(sample = sample, diceCount = diceCount)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                if (event.values.size >= 3) {
                    latestGyro = floatArrayOf(event.values[0], event.values[1], event.values[2])
                }
            }

            Sensor.TYPE_ACCELEROMETER -> {
                if (event.values.size >= 3) {
                    val ax = event.values[0]
                    val ay = event.values[1]
                    val az = event.values[2]
                    latestAccel = floatArrayOf(ax, ay, az)
                    detectShake(ax, ay, az)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Non requis pour l'entropie
    }

    private fun detectShake(ax: Float, ay: Float, az: Float) {
        val gX = ax / SensorManager.GRAVITY_EARTH
        val gY = ay / SensorManager.GRAVITY_EARTH
        val gZ = az / SensorManager.GRAVITY_EARTH
        val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

        if (gForce > SHAKE_THRESHOLD_G) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeTimestampMs >= SHAKE_COOLDOWN_MS) {
                lastShakeTimestampMs = now
                _shakeEvents.tryEmit(Unit)
            }
        }
    }

    private fun readBatteryPercentage(): Int? = try {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100) / scale else null
    } catch (_: Exception) {
        null
    }

    companion object {
        private const val SHAKE_THRESHOLD_G = 2.25f
        private const val SHAKE_COOLDOWN_MS = 750L
    }
}
