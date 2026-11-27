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

    @Test
    fun `generate Play Store 512x512 App Icon, 1024x500 Feature Graphic Banner and Play Games Achievements Bulk Import ZIP`() {
        val currentDir = File(System.getProperty("user.dir") ?: ".")
        val projectRoot = if (currentDir.name == "app") currentDir.parentFile ?: currentDir else currentDir
        val storeDir = File(projectRoot, "docs/play_store_assets")
        if (!storeDir.exists()) storeDir.mkdirs()

        val bgDark = 0xFF090A10.toInt()
        val surfaceDark = 0xFF121521.toInt()
        val gridLine = 0xFF141828.toInt()
        val cyan = 0xFF00F0FF.toInt()
        val cyanHighlight = 0xFF42F5FF.toInt()
        val magenta = 0xFFFF2A85.toInt()
        val magentaShadow = 0xFF9E1855.toInt()
        val magentaCore = 0xFFFFE6F2.toInt()
        val yellow = 0xFFFFE600.toInt()

        // =========================================================================
        // 1. ICÔNE PLAY STORE 512 × 512 px (play_store_icon_512x512.png)
        // Fidèle à ic_launcher_background.xml + ic_launcher_foreground.xml
        // =========================================================================
        val iconW = 512
        val iconH = 512
        val iconPixels = IntArray(iconW * iconH) { bgDark }

        fun fillIcon(x0: Int, y0: Int, w: Int, h: Int, color: Int) {
            val xEnd = (x0 + w).coerceAtMost(iconW)
            val yEnd = (y0 + h).coerceAtMost(iconH)
            for (y in y0.coerceAtLeast(0) until yEnd) {
                val rowOffset = y * iconW
                for (x in x0.coerceAtLeast(0) until xEnd) {
                    iconPixels[rowOffset + x] = color
                }
            }
        }

        // Micro-grille de fond
        for (xy in 0 until 512 step 32) {
            fillIcon(xy, 0, 2, 512, gridLine)
            fillIcon(0, xy, 512, 2, gridLine)
        }
        // Croisillons de visée aux 4 coins
        fillIcon(36, 36, 36, 6, gridLine); fillIcon(36, 36, 6, 36, gridLine)
        fillIcon(440, 36, 36, 6, gridLine); fillIcon(470, 36, 6, 36, gridLine)
        fillIcon(36, 470, 36, 6, gridLine); fillIcon(36, 440, 6, 36, gridLine)
        fillIcon(440, 470, 36, 6, gridLine); fillIcon(470, 440, 6, 36, gridLine)

        // Dé central Face 5 sur grille logique 14×14 pixels (1 pixel logique = 20×20 px -> dé 280×280 px)
        val p = 20
        val ox = 116
        val oy = 116
        val sx = ox + p // Ombre magenta décalée (+1,+1 pixel logique)
        val sy = oy + p

        // Ombre portée crénelée magenta
        fillIcon(sx + p, sy, 12 * p, 14 * p, magentaShadow)
        fillIcon(sx, sy + p, 14 * p, 12 * p, magentaShadow)

        // Corps sombre crénelé du dé
        fillIcon(ox + p, oy, 12 * p, 14 * p, surfaceDark)
        fillIcon(ox, oy + p, 14 * p, 12 * p, surfaceDark)

        // Bordure extérieure néon cyan crénelée
        fillIcon(ox + p, oy, 12 * p, p, cyan)
        fillIcon(ox + p, oy + 13 * p, 12 * p, p, cyan)
        fillIcon(ox, oy + p, p, 12 * p, cyan)
        fillIcon(ox + 13 * p, oy + p, p, 12 * p, cyan)

        // Reflet interne haut/gauche
        fillIcon(ox + p, oy + p, 12 * p, 6, cyanHighlight)
        fillIcon(ox + p, oy + p, 6, 12 * p, cyanHighlight)

        // 4 points d'angle de la face 5 (2×2 pixels logiques = 40×40 px)
        fillIcon(ox + 3 * p, oy + 3 * p, 2 * p, 2 * p, cyan)
        fillIcon(ox + 9 * p, oy + 3 * p, 2 * p, 2 * p, cyan)
        fillIcon(ox + 3 * p, oy + 9 * p, 2 * p, 2 * p, cyan)
        fillIcon(ox + 9 * p, oy + 9 * p, 2 * p, 2 * p, cyan)

        // Point central magenta (2.5×2.5 pixels logiques = 50×50 px) + cœur lumineux (20×20 px)
        fillIcon(ox + 6 * p - 5, oy + 6 * p - 5, 50, 50, magenta)
        fillIcon(ox + 6 * p + 10, oy + 6 * p + 10, 20, 20, magentaCore)

        val iconFile = File(storeDir, "play_store_icon_512x512.png")
        writeRgbaPng(width = iconW, height = iconH, argbPixels = iconPixels, outFile = iconFile)
        assertTrue(iconFile.exists() && iconFile.length() > 100)

        // =========================================================================
        // 2. BANNIÈRE FEATURE GRAPHIC 1024 × 500 px (play_store_feature_graphic_1024x500.png)
        // Titre Pixel Art "NEON 5K" + Rangée de 5 dés Pixel Art Cyberpunk
        // =========================================================================
        val banW = 1024
        val banH = 500
        val banPixels = IntArray(banW * banH) { bgDark }

        fun fillBan(x0: Int, y0: Int, w: Int, h: Int, color: Int) {
            val xEnd = (x0 + w).coerceAtMost(banW)
            val yEnd = (y0 + h).coerceAtMost(banH)
            for (y in y0.coerceAtLeast(0) until yEnd) {
                val rowOffset = y * banW
                for (x in x0.coerceAtLeast(0) until xEnd) {
                    banPixels[rowOffset + x] = color
                }
            }
        }

        // Grille cyber de fond
        for (x in 0 until banW step 32) fillBan(x, 0, 2, banH, gridLine)
        for (y in 0 until banH step 32) fillBan(0, y, banW, 2, gridLine)

        // Cadre néon double haut et bas
        fillBan(0, 0, banW, 6, magenta)
        fillBan(0, 10, banW, 2, cyan)
        fillBan(0, banH - 12, banW, 2, cyan)
        fillBan(0, banH - 6, banW, 6, magenta)

        // Police Pixel Art 5×7 pour "NEON 5K" et sous-titre
        val glyphs5x7 = mapOf(
            'N' to listOf("10001", "11001", "10101", "10011", "10001", "10001", "10001"),
            'E' to listOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
            'O' to listOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
            '5' to listOf("11111", "10000", "11110", "00001", "00001", "10001", "01110"),
            'K' to listOf("10001", "10010", "10100", "11000", "10100", "10010", "10001"),
            'C' to listOf("01110", "10001", "10000", "10000", "10000", "10001", "01110"),
            'Y' to listOf("10001", "10001", "01010", "00100", "00100", "00100", "00100"),
            'B' to listOf("11110", "10001", "10001", "11110", "10001", "10001", "11110"),
            'R' to listOf("11110", "10001", "10001", "11110", "10100", "10010", "10001"),
            'D' to listOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
            'I' to listOf("11111", "00100", "00100", "00100", "00100", "00100", "11111"),
            '0' to listOf("01110", "10001", "10011", "10101", "11001", "10001", "01110"),
            '-' to listOf("00000", "00000", "00000", "11111", "00000", "00000", "00000")
        )

        fun drawPixelText(text: String, startX: Int, startY: Int, scale: Int, mainColor: Int, shadowColor: Int?) {
            var curX = startX
            for (ch in text) {
                if (ch == ' ') {
                    curX += 4 * scale
                    continue
                }
                val glyph = glyphs5x7[ch] ?: continue
                for (r in 0 until 7) {
                    for (c in 0 until 5) {
                        if (glyph[r][c] == '1') {
                            if (shadowColor != null) {
                                fillBan(curX + c * scale + scale / 2, startY + r * scale + scale / 2, scale - 1, scale - 1, shadowColor)
                            }
                            fillBan(curX + c * scale, startY + r * scale, scale - 1, scale - 1, mainColor)
                        }
                    }
                }
                curX += 6 * scale
            }
        }

        // Titre principal "NEON 5K" centré en haut (échelle 15 px par bloc -> hauteur 105 px)
        // "NEON" en Cyan avec ombre Magenta, "5K" en Jaune avec ombre Magenta
        drawPixelText("NEON", startX = 212, startY = 58, scale = 15, mainColor = cyan, shadowColor = magentaShadow)
        drawPixelText("5K", startX = 632, startY = 58, scale = 15, mainColor = yellow, shadowColor = magenta)

        // Sous-titre "CYBER DICE 5000" (échelle 5 px par bloc)
        drawPixelText("CYBER DICE 5000", startX = 296, startY = 188, scale = 5, mainColor = magenta, shadowColor = null)

        // Rangée de 5 dés Pixel Art en bas (faces 1, 5, 1, 5, 5 -> Main pleine 5000 !)
        // Chaque dé = 14×14 pixels logiques à scale = 9 px -> 126×126 px, espacés de 32 px
        val bannerFaces = listOf(1, 5, 1, 5, 6)
        val dieScale = 9
        val dieWidth = 14 * dieScale // 126 px
        val gap = 30
        val totalRowW = bannerFaces.size * dieWidth + (bannerFaces.size - 1) * gap // 750 px
        val rowStartX = (banW - totalRowW) / 2 // 137
        val rowStartY = 268

        bannerFaces.forEachIndexed { idx, face ->
            val dx = rowStartX + idx * (dieWidth + gap)
            val dy = rowStartY
            val borderCol = when (idx % 3) {
                0 -> cyan
                1 -> magenta
                else -> yellow
            }

            // Ombre décalée
            fillBan(dx + dieScale * 2, dy + dieScale, 12 * dieScale, 14 * dieScale, magentaShadow)
            fillBan(dx + dieScale, dy + dieScale * 2, 14 * dieScale, 12 * dieScale, magentaShadow)

            // Corps du dé
            fillBan(dx + dieScale, dy, 12 * dieScale, 14 * dieScale, surfaceDark)
            fillBan(dx, dy + dieScale, 14 * dieScale, 12 * dieScale, surfaceDark)

            // Bordure néon
            fillBan(dx + dieScale, dy, 12 * dieScale, dieScale, borderCol)
            fillBan(dx + dieScale, dy + 13 * dieScale, 12 * dieScale, dieScale, borderCol)
            fillBan(dx, dy + dieScale, dieScale, 12 * dieScale, borderCol)
            fillBan(dx + 13 * dieScale, dy + dieScale, dieScale, 12 * dieScale, borderCol)

            fun pip(pxLog: Int, pyLog: Int, col: Int = borderCol) {
                fillBan(dx + pxLog * dieScale, dy + pyLog * dieScale, 2 * dieScale, 2 * dieScale, col)
            }

            when (face) {
                1 -> {
                    fillBan(dx + 6 * dieScale - 2, dy + 6 * dieScale - 2, 2 * dieScale + 4, 2 * dieScale + 4, yellow)
                }
                5 -> {
                    pip(3, 3, cyan); pip(9, 3, cyan)
                    pip(6, 6, magenta)
                    pip(3, 9, cyan); pip(9, 9, cyan)
                }
                6 -> {
                    pip(3, 3, yellow); pip(9, 3, yellow)
                    pip(3, 6, yellow); pip(9, 6, yellow)
                    pip(3, 9, yellow); pip(9, 9, yellow)
                }
            }
        }

        val bannerFile = File(storeDir, "play_store_feature_graphic_1024x500.png")
        writeRgbaPng(width = banW, height = banH, argbPixels = banPixels, outFile = bannerFile)
        assertTrue(bannerFile.exists() && bannerFile.length() > 100)

        // =========================================================================
        // 3. ARCHIVE ZIP OFFICIELLE GOOGLE PLAY CONSOLE — IMPORT EN MASSE DES 20 SUCCÈS
        // (AchievementsMetadata.csv + 20 icônes PNG 512×512)
        // =========================================================================
        val iconsDir = File(projectRoot, "docs/play_console_achievements_icons")
        val zipFile = File(storeDir, "play_games_achievements_import_fr_en.zip")

        // Format officiel Play Console Bulk Import CSV (sans en-tête) :
        // Name,Description,Incremental,Steps,Initial State,Points,Sort Order,Icon filename[,Locale,Localized Name,Localized Description...]
        val esTranslations = mapOf(
            AchievementId.FIRST_BOOT to ("First Boot" to "Supera el umbral de apertura por primera vez."),
            AchievementId.HIGH_VOLTAGE_OPENING to ("High Voltage Opening" to "Abre tu marcador con al menos 1 500 puntos en un solo turno."),
            AchievementId.TRIPLE_ACE_PROTOCOL to ("Triple Ace Protocol" to "Consigue un trío de Ases (1 000 pts) en una sola tirada."),
            AchievementId.BRELAN_COLLECTOR to ("Coleccionista de Tríos" to "Consigue al menos un trío de cada una de las 6 caras (1 a 6)."),
            AchievementId.QUAD_CORE to ("Quad Core" to "Consigue un Póker (4 dados iguales) en una sola tirada."),
            AchievementId.FIVE_OF_A_KIND to ("God Roll (Repóker)" to "Consigue un Repóker (5 dados iguales) en una sola tirada."),
            AchievementId.STRAIGHT_RUNNER to ("Straight Runner" to "Consigue una Escalera (1-2-3-4-5 o 2-3-4-5-6) en una sola tirada."),
            AchievementId.TWIN_MATRIX to ("Twin Matrix" to "Consigue 3 Pares distintos en una tirada (variante 6 dados)."),
            AchievementId.FULL_HOUSE_CYBER to ("Full House Cyber" to "Consigue un Full (Trío + Pareja) con el bono de Full activo."),
            AchievementId.HOT_DICE to ("Hot Dice!" to "Consigue Mano Plena puntuando con todos los dados."),
            AchievementId.FULL_OVERCLOCK to ("Full Overclock" to "Encadena 2 Manos Plenas en el mismo turno y guarda los puntos."),
            AchievementId.ONE_DIE_MIRACLE to ("Milagro a 1 Dado" to "Puntúa un 1 o un 5 al relanzar solo 1 dado restante."),
            AchievementId.SYSTEM_CRASH to ("System Crash" to "Pierde un bote de al menos 1 000 puntos por un fallo (Glitch)."),
            AchievementId.THREE_STRIKES_OUT to ("Three Strikes Out (✕✕✕)" to "Sufre la penalización o retroceso tras 3 fallos seguidos (✕✕✕)."),
            AchievementId.PIXEL_PERFECT_5000 to ("Pixel Perfect 5000" to "Gana una partida alcanzando exactamente la puntuación objetivo."),
            AchievementId.OVERKILL_TURN to ("Overkill Turn" to "Guarda al menos 2 000 puntos en un solo turno."),
            AchievementId.HIJACKER to ("Hijacker (Robo)" to "Retoma el bote de un rival y guarda los puntos del turno."),
            AchievementId.HEADSHOT to ("Headshot (Derribo)" to "Iguala exactamente la puntuación de un rival para penalizarlo."),
            AchievementId.BEAT_THE_MACHINE to ("Beat the Machine" to "Gana una partida contra el bot Overclock Kamikaze."),
            AchievementId.ENTROPY_MASTER to ("Entropy Master" to "Completa 50 partidas impulsadas por la entropía SHA-256.")
        )

        val ptTranslations = mapOf(
            AchievementId.FIRST_BOOT to ("First Boot" to "Ultrapasse o limite de abertura pela primeira vez."),
            AchievementId.HIGH_VOLTAGE_OPENING to ("High Voltage Opening" to "Abra a sua pontuação com pelo menos 1 500 pontos num único turno."),
            AchievementId.TRIPLE_ACE_PROTOCOL to ("Triple Ace Protocol" to "Obtenha uma trinca de Ases (1 000 pts) num único lançamento."),
            AchievementId.BRELAN_COLLECTOR to ("Colecionador de Trincas" to "Faça pelo menos uma trinca de cada uma das 6 faces (1 a 6)."),
            AchievementId.QUAD_CORE to ("Quad Core" to "Obtenha uma Quadra (4 dados iguais) num único lançamento."),
            AchievementId.FIVE_OF_A_KIND to ("God Roll (Quina)" to "Obtenha uma Quina (5 dados iguais) num único lançamento."),
            AchievementId.STRAIGHT_RUNNER to ("Straight Runner" to "Faça uma Sequência (1-2-3-4-5 ou 2-3-4-5-6) num único lançamento."),
            AchievementId.TWIN_MATRIX to ("Twin Matrix" to "Obtenha 3 Pares distintos num lançamento (variante 6 dados)."),
            AchievementId.FULL_HOUSE_CYBER to ("Full House Cyber" to "Faça um Full House (Trinca + Par) com o bónus ativo."),
            AchievementId.HOT_DICE to ("Hot Dice!" to "Consiga Mão Cheia pontuando com todos os dados."),
            AchievementId.FULL_OVERCLOCK to ("Full Overclock" to "Encadeie 2 Mãos Cheias no mesmo turno e guarde os pontos."),
            AchievementId.ONE_DIE_MIRACLE to ("Milagre de 1 Dado" to "Pontue um 1 ou um 5 ao relançar apenas 1 dado restante."),
            AchievementId.SYSTEM_CRASH to ("System Crash" to "Perca um acumulado de pelo menos 1 000 pontos num Glitch."),
            AchievementId.THREE_STRIKES_OUT to ("Three Strikes Out (✕✕✕)" to "Sofra a penalidade ou recuo após 3 jogadas nulas (✕✕✕)."),
            AchievementId.PIXEL_PERFECT_5000 to ("Pixel Perfect 5000" to "Vença uma partida atingindo exatamente a pontuação alvo."),
            AchievementId.OVERKILL_TURN to ("Overkill Turn" to "Guarde pelo menos 2 000 pontos num único turno."),
            AchievementId.HIJACKER to ("Hijacker (Retoma)" to "Retome o acumulado de um adversário e guarde os pontos."),
            AchievementId.HEADSHOT to ("Headshot (Queda)" to "Iguale exatamente a pontuação de um adversário."),
            AchievementId.BEAT_THE_MACHINE to ("Beat the Machine" to "Vença uma partida contra o bot Overclock Kamikaze."),
            AchievementId.ENTROPY_MASTER to ("Entropy Master" to "Conclua 50 partidas completas com entropia SHA-256.")
        )

        fun csvEscape(value: String): String =
            if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value

        val csvLines = AchievementId.entries.mapIndexed { idx, ach ->
            val num = String.format("%02d", idx + 1)
            val iconFileName = "achievement_${num}_${ach.name.lowercase()}.png"
            val isIncremental = ach.targetSteps > 1
            val stepsField = if (isIncremental) ach.targetSteps.toString() else ""
            val (esTitle, esDesc) = esTranslations.getValue(ach)
            val (ptTitle, ptDesc) = ptTranslations.getValue(ach)
            // Défaut en en-US + traductions fr-FR, es-ES et pt-PT intégrées sur la même ligne
            listOf(
                csvEscape(ach.titleEn),
                csvEscape(ach.descriptionEn),
                if (isIncremental) "True" else "False",
                stepsField,
                "Revealed",
                ach.playGamesXp.toString(),
                (idx + 1).toString(),
                iconFileName,
                "fr-FR",
                csvEscape(ach.titleFr),
                csvEscape(ach.descriptionFr),
                "es-ES",
                csvEscape(esTitle),
                csvEscape(esDesc),
                "pt-PT",
                csvEscape(ptTitle),
                csvEscape(ptDesc)
            ).joinToString(",")
        }
        val csvContent = csvLines.joinToString("\n")
        val csvFile = File(storeDir, "AchievementsMetadata.csv")
        csvFile.writeText(csvContent, Charsets.UTF_8)

        java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. AchievementsMetadata.csv à la racine du ZIP
            zos.putNextEntry(java.util.zip.ZipEntry("AchievementsMetadata.csv"))
            zos.write(csvContent.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Les 20 icônes PNG 512x512 à la racine du ZIP
            AchievementId.entries.forEachIndexed { idx, ach ->
                val num = String.format("%02d", idx + 1)
                val iconFileName = "achievement_${num}_${ach.name.lowercase()}.png"
                val pngFile = File(iconsDir, iconFileName)
                if (pngFile.exists()) {
                    zos.putNextEntry(java.util.zip.ZipEntry(iconFileName))
                    zos.write(pngFile.readBytes())
                    zos.closeEntry()
                }
            }
        }
        assertTrue(zipFile.exists() && zipFile.length() > 1000)
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
