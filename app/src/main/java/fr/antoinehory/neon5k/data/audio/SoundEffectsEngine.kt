package fr.antoinehory.neon5k.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Événements sonores rétro 8-bit disponibles dans Neon 5K (Lot 4.5).
 */
enum class SoundCue {
    ROLL,
    SELECT_DIE,
    HOT_DICE,
    BANK,
    BUST,
    VICTORY
}

/**
 * Synthétiseur audio 8-bit PCM léger sans aucun fichier média embarqué (`AudioTrack`, ADR-015).
 *
 * - Génère de minuscules formes d'onde chiptune en Kotlin pur (ondes carrées/triangle).
 * - Exécute toute lecture sur un thread dédié unique afin de ne jamais bloquer le Main Thread.
 */
class SoundEffectsEngine {

    private val audioExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "neon5k-8bit-audio").apply { isDaemon = true }
    }

    @Volatile
    private var released = false

    private val cachedWaveforms: Map<SoundCue, ShortArray> by lazy {
        mapOf(
            SoundCue.ROLL to synthesizeRoll(),
            SoundCue.SELECT_DIE to synthesizeSelect(),
            SoundCue.HOT_DICE to synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.50), 55, 0.28),
            SoundCue.BANK to synthesizeArpeggio(listOf(392.00, 587.33), 85, 0.28),
            SoundCue.BUST to synthesizeBust(),
            SoundCue.VICTORY to synthesizeArpeggio(listOf(523.25, 659.25, 783.99, 1046.50, 783.99, 1046.50), 75, 0.30)
        )
    }

    fun play(cue: SoundCue) {
        if (released) return
        audioExecutor.execute {
            if (released) return@execute
            runCatching {
                val samples = cachedWaveforms[cue] ?: return@runCatching
                playPcmSamples(samples)
            }
        }
    }

    fun release() {
        released = true
        runCatching { audioExecutor.shutdownNow() }
    }

    private fun playPcmSamples(samples: ShortArray) {
        val bufferSizeBytes = samples.size * 2
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(bufferSizeBytes)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(minBuf.coerceAtLeast(bufferSizeBytes))
            .build()

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = ((samples.size * 1000L) / SAMPLE_RATE) + 25L
            Thread.sleep(durationMs)
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }

    private fun synthesizeSelect(): ShortArray {
        val count = (SAMPLE_RATE * 0.045).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / count
            val freq = 820.0 + 360.0 * progress
            val env = (1.0 - progress) * 0.25
            squareSample(freq, t, env)
        }
    }

    private fun synthesizeRoll(): ShortArray {
        val stepSamples = (SAMPLE_RATE * 0.032).toInt()
        val freqs = doubleArrayOf(340.0, 480.0, 410.0, 560.0)
        val out = ShortArray(stepSamples * freqs.size)
        freqs.forEachIndexed { stepIdx, freq ->
            for (i in 0 until stepSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val env = (1.0 - i.toDouble() / stepSamples) * 0.22
                out[stepIdx * stepSamples + i] = squareSample(freq, t, env)
            }
        }
        return out
    }

    private fun synthesizeBust(): ShortArray {
        val count = (SAMPLE_RATE * 0.26).toInt()
        return ShortArray(count) { i ->
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / count
            val freq = 340.0 - 220.0 * progress
            val env = (1.0 - progress * 0.85) * 0.28
            squareSample(freq, t, env)
        }
    }

    private fun synthesizeArpeggio(
        notesHz: List<Double>,
        noteDurationMs: Int,
        gain: Double
    ): ShortArray {
        val noteSamples = (SAMPLE_RATE * (noteDurationMs / 1000.0)).toInt()
        val out = ShortArray(noteSamples * notesHz.size)
        notesHz.forEachIndexed { idx, freq ->
            for (i in 0 until noteSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val localProgress = i.toDouble() / noteSamples
                val env = (1.0 - localProgress * 0.55) * gain
                out[idx * noteSamples + i] = squareSample(freq, t, env)
            }
        }
        return out
    }

    private fun squareSample(freqHz: Double, timeSec: Double, amplitude: Double): Short {
        val s = if (sin(2.0 * PI * freqHz * timeSec) >= 0.0) 1.0 else -1.0
        return (s * amplitude * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    companion object {
        private const val SAMPLE_RATE = 22_050
    }
}
