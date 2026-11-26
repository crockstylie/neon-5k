package fr.antoinehory.neon5k.domain.entropy

import fr.antoinehory.neon5k.domain.model.EntropyRawSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de tests unitaires JVM pour [EntropySeedGenerator].
 * Vérifie le déterminisme SHA-256, le fallback sans capteurs et l'uniformité statistique des faces `1..6`.
 */
class EntropySeedGeneratorTest {

    @Test
    fun `identical EntropyRawSample produces identical hash, seed and dice faces`() {
        val sample = EntropyRawSample(
            gyroX = 0.142f,
            gyroY = -0.085f,
            gyroZ = 1.204f,
            accelX = 0.12f,
            accelY = 9.78f,
            accelZ = 0.45f,
            epochMillis = 1764180000123L,
            nanoTime = 9876543210123L,
            uptimeNanos = 5432109876543L,
            touchX = 412f,
            touchY = 1640f,
            pressDurationMs = 74L,
            rollCounter = 3,
            batteryLevel = 84,
            secureSaltHex = "A1B2C3D4",
            triggeredByShake = false
        )

        val outcome1 = EntropySeedGenerator.generateRoll(sample, diceCount = 5)
        val outcome2 = EntropySeedGenerator.generateRoll(sample, diceCount = 5)

        assertEquals(outcome1.snapshot.fullHashHex, outcome2.snapshot.fullHashHex)
        assertEquals(outcome1.snapshot.seed, outcome2.snapshot.seed)
        assertEquals(outcome1.faces, outcome2.faces)
        assertEquals(64, outcome1.snapshot.fullHashHex.length)
        assertEquals(7, outcome1.snapshot.shortHashHex.length)
        assertTrue(outcome1.snapshot.shortHashHex.startsWith("#"))
        assertTrue(outcome1.snapshot.hasGyroscope)
        assertTrue(outcome1.snapshot.hasAccelerometer)
    }

    @Test
    fun `1 nanosecond difference changes SHA-256 hash and seed completely`() {
        val base = EntropyRawSample(
            gyroX = null,
            gyroY = null,
            gyroZ = null,
            accelX = null,
            accelY = null,
            accelZ = null,
            epochMillis = 1764180000000L,
            nanoTime = 1000000000L,
            uptimeNanos = 1000000000L,
            secureSaltHex = "00112233"
        )
        val shifted = base.copy(nanoTime = 1000000001L)

        val out1 = EntropySeedGenerator.generateRoll(base, diceCount = 6)
        val out2 = EntropySeedGenerator.generateRoll(shifted, diceCount = 6)

        assertNotEquals(out1.snapshot.fullHashHex, out2.snapshot.fullHashHex)
        assertNotEquals(out1.snapshot.seed, out2.snapshot.seed)
        assertFalse(out1.snapshot.hasGyroscope)
        assertFalse(out1.snapshot.hasAccelerometer)
    }

    @Test
    fun `generated dice faces are strictly in 1 to 6 and uniformly distributed over 1200 rolls`() {
        val counts = IntArray(7)
        val totalRolls = 1200 // 1200 * 5 = 6000 dés tirés (~1000 attendus par face)

        for (i in 1..totalRolls) {
            val sample = EntropyRawSample(
                gyroX = i * 0.01f,
                gyroY = -i * 0.02f,
                gyroZ = i * 0.03f,
                epochMillis = 1764180000000L + i,
                nanoTime = 500_000_000L + i * 7919L,
                uptimeNanos = 300_000_000L + i * 104729L,
                rollCounter = i,
                secureSaltHex = String.format("%08X", i * 2654435761L)
            )
            val outcome = EntropySeedGenerator.generateRoll(sample, diceCount = 5)
            assertEquals(5, outcome.faces.size)
            for (face in outcome.faces) {
                assertTrue("Face out of range: $face", face in 1..6)
                counts[face]++
            }
        }

        // Chaque face (attendue ~1000 fois sur 6000 dés) doit apparaître entre 850 et 1150 fois
        for (face in 1..6) {
            assertTrue(
                "Face $face count (${counts[face]}) should be close to 1000",
                counts[face] in 850..1150
            )
        }
    }
}
