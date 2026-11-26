package fr.antoinehory.neon5k.domain.achievement

import fr.antoinehory.neon5k.domain.rules.CombinationKind
import fr.antoinehory.neon5k.domain.rules.ScoringCombination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

/**
 * Suite de tests unitaires JVM pour [AchievementEvaluator] et générateur des 20 icônes PNG `512×512`
 * destinées à la Google Play Console (`docs/play_console_achievements_icons/`).
 */
class AchievementEvaluatorTest {

    @Test
    fun `20 achievements total exactly 1000 XP and have valid 16x16 matrices`() {
        val all = AchievementId.entries
        assertEquals(20, all.size)
        assertEquals(1000, all.sumOf { it.playGamesXp })
        for (achievement in all) {
            assertEquals(16, achievement.iconMatrix16x16.size)
            for (row in achievement.iconMatrix16x16) {
                assertEquals(16, row.length)
            }
        }
    }

    @Test
    fun `infinite counter continues incrementing after unlock and promotes mastery ranks`() {
        var map = AchievementEvaluator.initialProgressMap()
        val tripleAceCombo = ScoringCombination(
            kind = CombinationKind.THREE_OF_A_KIND,
            diceFaces = listOf(1, 1, 1),
            points = 1000,
            labelFr = "Brelan d'As"
        )
        val rollEvent = TelemetryGameEvent.HumanRollExecuted(
            rolledFaces = listOf(1, 1, 1, 2, 4),
            combinationsOnRoll = listOf(tripleAceCombo),
            triggeredByShake = false,
            pressDurationMs = 120L,
            scoredOnSingleRemainingDie = false
        )

        // 1re occurrence -> Déverrouillage initial (BRONZE, lifetimeCount = 1)
        val firstOutcome = AchievementEvaluator.evaluate(map, rollEvent, distinctTripleFacesMask = 0b10)
        map = firstOutcome.updatedProgressMap
        val p1 = map.getValue(AchievementId.TRIPLE_ACE_PROTOCOL)
        assertTrue(p1.isUnlocked)
        assertEquals(1, p1.lifetimeCount)
        assertEquals(MasteryRank.BRONZE, p1.masteryRank)
        assertTrue(firstOutcome.notifications.any { it.achievementId == AchievementId.TRIPLE_ACE_PROTOCOL && it.isFirstUnlock })

        // 4 occurrences supplémentaires -> Total à vie = 5 -> Promotion au rang ARGENT !
        var silverNotified = false
        repeat(4) {
            val outcome = AchievementEvaluator.evaluate(map, rollEvent, distinctTripleFacesMask = 0b10)
            map = outcome.updatedProgressMap
            if (outcome.notifications.any { it.achievementId == AchievementId.TRIPLE_ACE_PROTOCOL && it.newRank == MasteryRank.SILVER }) {
                silverNotified = true
            }
        }
        val p5 = map.getValue(AchievementId.TRIPLE_ACE_PROTOCOL)
        assertTrue(p5.isUnlocked)
        assertEquals(5, p5.lifetimeCount)
        assertEquals(MasteryRank.SILVER, p5.masteryRank)
        assertTrue(silverNotified)
    }

    @Test
    fun `brelan collector tracks distinct faces 1 to 6 while accumulating total triples`() {
        var map = AchievementEvaluator.initialProgressMap()

        // Obtenir successivement les brelans des faces 1 à 5 (5/6 faces -> pas encore déverrouillé)
        var mask = 0
        for (face in 1..5) {
            mask = mask or (1 shl face)
            val combo = ScoringCombination(
                kind = CombinationKind.THREE_OF_A_KIND,
                diceFaces = listOf(face, face, face),
                points = if (face == 1) 1000 else face * 100,
                labelFr = "Brelan de $face"
            )
            val outcome = AchievementEvaluator.evaluate(
                currentMap = map,
                event = TelemetryGameEvent.HumanRollExecuted(
                    rolledFaces = listOf(face, face, face, 2, 3),
                    combinationsOnRoll = listOf(combo),
                    triggeredByShake = false,
                    pressDurationMs = 100L,
                    scoredOnSingleRemainingDie = false
                ),
                distinctTripleFacesMask = mask
            )
            map = outcome.updatedProgressMap
        }

        val beforeSix = map.getValue(AchievementId.BRELAN_COLLECTOR)
        assertFalse(beforeSix.isUnlocked)
        assertEquals(5, beforeSix.progressSteps)
        assertEquals(5, beforeSix.lifetimeCount)

        // Obtenir le brelan de 6 -> 6/6 faces -> BRELAN_COLLECTOR déverrouillé !
        mask = mask or (1 shl 6)
        val combo6 = ScoringCombination(
            kind = CombinationKind.THREE_OF_A_KIND,
            diceFaces = listOf(6, 6, 6),
            points = 600,
            labelFr = "Brelan de 6"
        )
        val finalOutcome = AchievementEvaluator.evaluate(
            currentMap = map,
            event = TelemetryGameEvent.HumanRollExecuted(
                rolledFaces = listOf(6, 6, 6, 2, 3),
                combinationsOnRoll = listOf(combo6),
                triggeredByShake = false,
                pressDurationMs = 100L,
                scoredOnSingleRemainingDie = false
            ),
            distinctTripleFacesMask = mask
        )
        val afterSix = finalOutcome.updatedProgressMap.getValue(AchievementId.BRELAN_COLLECTOR)
        assertTrue(afterSix.isUnlocked)
        assertEquals(6, afterSix.progressSteps)
        assertEquals(6, afterSix.lifetimeCount)
    }

    @Test
    fun `generate 20 Play Console 512x512 PNG achievement icons from 16x16 Pixel Art matrices`() {
        val currentDir = File(System.getProperty("user.dir") ?: ".")
        val projectRoot = if (currentDir.name == "app") currentDir.parentFile ?: currentDir else currentDir
        val outputDir = File(projectRoot, "docs/play_console_achievements_icons")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val bgDark = 0xFF090A10.toInt()
        val surfaceGrid = 0xFF141929.toInt()
        val primaryCyan = 0xFF00F0FF.toInt()
        val secondaryMagenta = 0xFFFF2A85.toInt()
        val alertYellow = 0xFFFFE600.toInt()

        AchievementId.entries.forEachIndexed { idx, achievement ->
            val pixels = IntArray(512 * 512) { bgDark }

            fun fillRect(x0: Int, y0: Int, w: Int, h: Int, color: Int) {
                val xEnd = (x0 + w).coerceAtMost(512)
                val yEnd = (y0 + h).coerceAtMost(512)
                for (y in y0.coerceAtLeast(0) until yEnd) {
                    val rowOffset = y * 512
                    for (x in x0.coerceAtLeast(0) until xEnd) {
                        pixels[rowOffset + x] = color
                    }
                }
            }

            // 1. Micro-grille pixel art (pas de 32 px)
            for (xy in 0 until 512 step 32) {
                fillRect(xy, 0, 2, 512, surfaceGrid)
                fillRect(0, xy, 512, 2, surfaceGrid)
            }

            // 2. Cadre néon extérieur (avec marge de sécurité Play Console)
            fillRect(24, 24, 464, 8, secondaryMagenta)
            fillRect(24, 480, 464, 8, secondaryMagenta)
            fillRect(24, 24, 8, 464, secondaryMagenta)
            fillRect(480, 24, 8, 464, secondaryMagenta)

            // 3. Rendu de la matrice 16x16 centrée (1 pixel logique = 24x24 px, grille 384x384 centrée à offset 64)
            val cellSize = 24
            val offset = 64
            val matrix = achievement.iconMatrix16x16
            for (row in 0 until 16) {
                val line = matrix[row]
                for (col in 0 until 16) {
                    val pixelColor = when (line[col]) {
                        '1' -> primaryCyan
                        '2' -> secondaryMagenta
                        '3' -> alertYellow
                        else -> null
                    }
                    if (pixelColor != null) {
                        fillRect(
                            offset + col * cellSize,
                            offset + row * cellSize,
                            cellSize - 2,
                            cellSize - 2,
                            pixelColor
                        )
                    }
                }
            }

            val num = String.format("%02d", idx + 1)
            val outFile = File(outputDir, "achievement_${num}_${achievement.name.lowercase()}.png")
            writeRgbaPng(width = 512, height = 512, argbPixels = pixels, outFile = outFile)
            assertTrue("Generated PNG must exist: ${outFile.absolutePath}", outFile.exists() && outFile.length() > 100)
        }
    }

    /**
     * Encodeur PNG 32-bit RGBA pur Kotlin/Java standard (`Deflater` + `CRC32`), sans dépendance `java.awt`.
     */
    private fun writeRgbaPng(width: Int, height: Int, argbPixels: IntArray, outFile: File) {
        val rawScanlines = ByteArray((width * 4 + 1) * height)
        var pos = 0
        for (y in 0 until height) {
            rawScanlines[pos++] = 0 // Filtre PNG : None (0)
            val rowOffset = y * width
            for (x in 0 until width) {
                val argb = argbPixels[rowOffset + x]
                rawScanlines[pos++] = ((argb shr 16) and 0xFF).toByte() // R
                rawScanlines[pos++] = ((argb shr 8) and 0xFF).toByte()  // G
                rawScanlines[pos++] = (argb and 0xFF).toByte()          // B
                rawScanlines[pos++] = ((argb ushr 24) and 0xFF).toByte() // A
            }
        }

        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(rawScanlines)
        deflater.finish()
        val idatStream = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            idatStream.write(buffer, 0, count)
        }
        deflater.end()

        FileOutputStream(outFile).use { fos ->
            val dos = DataOutputStream(fos)
            // Signature PNG 8 octets
            dos.write(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10))

            fun writeChunk(type: String, data: ByteArray) {
                val typeBytes = type.toByteArray(Charsets.US_ASCII)
                dos.writeInt(data.size)
                dos.write(typeBytes)
                dos.write(data)
                val crc = CRC32()
                crc.update(typeBytes)
                crc.update(data)
                dos.writeInt(crc.value.toInt())
            }

            // Chunk IHDR (13 octets)
            val ihdr = ByteArrayOutputStream(13)
            DataOutputStream(ihdr).apply {
                writeInt(width)
                writeInt(height)
                writeByte(8) // Bit depth = 8
                writeByte(6) // Color type = 6 (Truecolor with alpha RGBA)
                writeByte(0) // Compression = 0
                writeByte(0) // Filter = 0
                writeByte(0) // Interlace = 0
            }
            writeChunk("IHDR", ihdr.toByteArray())
            writeChunk("IDAT", idatStream.toByteArray())
            writeChunk("IEND", ByteArray(0))
            dos.flush()
        }
    }
}
