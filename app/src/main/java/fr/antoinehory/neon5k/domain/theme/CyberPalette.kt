package fr.antoinehory.neon5k.domain.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Catégories de palettes prédéfinies dans le Studio de Thème.
 */
enum class ThemePresetCategory(val titleFr: String) {
    CYBER_RETRO("PALETTES CYBERPUNK & RÉTRO"),
    GRAYSCALE("NIVEAUX DE GRIS (3 NIVEAUX DE CONTRASTE)"),
    SEPIA("SÉPIA (3 NIVEAUX DE CONTRASTE)"),
    DALTONISM("ACCESSIBILITÉ // DALTONISME"),
    CUSTOM("PERSONNALISÉ")
}

/**
 * Thèmes prédéfinis Cyberpunk, Niveaux de gris, Sépia et Daltonisme de Neon 5K.
 */
enum class CyberThemePreset(
    val labelFr: String,
    val category: ThemePresetCategory
) {
    // Cyberpunk & Rétro
    NEON_NIGHT("Neon Night", ThemePresetCategory.CYBER_RETRO),
    TERMINAL_PHOSPHOR("Terminal Phosphor", ThemePresetCategory.CYBER_RETRO),
    CRT_AMBER("CRT Amber", ThemePresetCategory.CYBER_RETRO),
    POCKET_MONO("Pocket Mono", ThemePresetCategory.CYBER_RETRO),
    OLED_MINIMAL("OLED Minimal", ThemePresetCategory.CYBER_RETRO),

    // Niveaux de gris (3 niveaux de contraste)
    GRAYSCALE_SOFT("Gris Doux (Bas)", ThemePresetCategory.GRAYSCALE),
    GRAYSCALE_BALANCED("Gris Équilibré (Moyen)", ThemePresetCategory.GRAYSCALE),
    GRAYSCALE_HIGH("Gris Pur (Max 21:1)", ThemePresetCategory.GRAYSCALE),

    // Sépia (3 niveaux de contraste)
    SEPIA_SOFT("Sépia Parchemin (Bas)", ThemePresetCategory.SEPIA),
    SEPIA_CLASSIC("Sépia Rétro (Moyen)", ThemePresetCategory.SEPIA),
    SEPIA_HIGH("Sépia Intense (Haut)", ThemePresetCategory.SEPIA),

    // Accessibilité Daltonisme
    DALTONISM_PROTA_DEUTERA("Protanopie / Deutéranopie (Rouge-Vert)", ThemePresetCategory.DALTONISM),
    DALTONISM_TRITANOPIA("Tritanopie (Bleu-Jaune)", ThemePresetCategory.DALTONISM),
    DALTONISM_ACHROMATOPSIA("Achromatopsie (Monochromatie)", ThemePresetCategory.DALTONISM),

    // Custom Studio
    CUSTOM("Custom Studio", ThemePresetCategory.CUSTOM)
}

/**
 * Styles visuels de rendu des dés sur la grille `16×16` du `PixelDiceCanvas`.
 */
enum class DiceVisualStyle(val labelFr: String) {
    PIXEL_SOLID("Pixel Solid (Plein 3D)"),
    NEON_WIREFRAME("Neon Wireframe (Filaire)"),
    DOT_MATRIX("Dot Matrix (LED)")
}

/**
 * Palette de couleurs ARGB 32-bit pure Kotlin (sans dépendance `androidx.compose.ui.graphics.Color`).
 */
data class CyberPalette(
    val preset: CyberThemePreset,
    val backgroundArgb: Long,
    val surfaceArgb: Long,
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val alertArgb: Long,
    val textArgb: Long
) {
    /** Ratio de contraste WCAG 2.1 entre le texte principal et le fond (`>= 4.5` recommandé). */
    val textOnBackgroundContrast: Double
        get() = wcagContrastRatio(textArgb, backgroundArgb)

    /** Vrai si le fond est lumineux (nécessite des icônes système sombres dans les barres Android). */
    val isLightBackground: Boolean
        get() = relativeLuminance(backgroundArgb) > 0.5

    companion object {
        // --- 1. Cyberpunk & Rétro ---
        val NEON_NIGHT = CyberPalette(
            preset = CyberThemePreset.NEON_NIGHT,
            backgroundArgb = 0xFF090A10L,
            surfaceArgb = 0xFF121521L,
            primaryArgb = 0xFF00F0FFL,
            secondaryArgb = 0xFFFF2A85L,
            alertArgb = 0xFFFFE600L,
            textArgb = 0xFFEAEFF5L
        )

        val TERMINAL_PHOSPHOR = CyberPalette(
            preset = CyberThemePreset.TERMINAL_PHOSPHOR,
            backgroundArgb = 0xFF050D07L,
            surfaceArgb = 0xFF0B1A0FL,
            primaryArgb = 0xFF39FF14L,
            secondaryArgb = 0xFF00D18CL,
            alertArgb = 0xFFFFB000L,
            textArgb = 0xFFD6FFE0L
        )

        val CRT_AMBER = CyberPalette(
            preset = CyberThemePreset.CRT_AMBER,
            backgroundArgb = 0xFF0E0904L,
            surfaceArgb = 0xFF1A1208L,
            primaryArgb = 0xFFFFAA00L,
            secondaryArgb = 0xFFFF5E00L,
            alertArgb = 0xFFFFE066L,
            textArgb = 0xFFFFE8C2L
        )

        val POCKET_MONO = CyberPalette(
            preset = CyberThemePreset.POCKET_MONO,
            backgroundArgb = 0xFFC4CFA1L,
            surfaceArgb = 0xFFB2BE8FL,
            primaryArgb = 0xFF1F2418L,
            secondaryArgb = 0xFF414B33L,
            alertArgb = 0xFF1F2418L,
            textArgb = 0xFF1F2418L
        )

        val OLED_MINIMAL = CyberPalette(
            preset = CyberThemePreset.OLED_MINIMAL,
            backgroundArgb = 0xFF000000L,
            surfaceArgb = 0xFF0D0D10L,
            primaryArgb = 0xFF00E5FFL,
            secondaryArgb = 0xFFFFFFFFL,
            alertArgb = 0xFFFF3B5CL,
            textArgb = 0xFFF5F5F5L
        )

        // --- 2. Niveaux de Gris (3 niveaux de contraste) ---
        val GRAYSCALE_SOFT = CyberPalette(
            preset = CyberThemePreset.GRAYSCALE_SOFT,
            backgroundArgb = 0xFF1D2024L,
            surfaceArgb = 0xFF272B31L,
            primaryArgb = 0xFFB8C0CCL,
            secondaryArgb = 0xFF949DAAL,
            alertArgb = 0xFFD8DEE8L,
            textArgb = 0xFFDCE1E8L
        )

        val GRAYSCALE_BALANCED = CyberPalette(
            preset = CyberThemePreset.GRAYSCALE_BALANCED,
            backgroundArgb = 0xFF101215L,
            surfaceArgb = 0xFF1A1D22L,
            primaryArgb = 0xFFDCE2ECL,
            secondaryArgb = 0xFFA7B1C0L,
            alertArgb = 0xFFF0F4F8L,
            textArgb = 0xFFECEFF4L
        )

        val GRAYSCALE_HIGH = CyberPalette(
            preset = CyberThemePreset.GRAYSCALE_HIGH,
            backgroundArgb = 0xFF000000L,
            surfaceArgb = 0xFF121212L,
            primaryArgb = 0xFFFFFFFFL,
            secondaryArgb = 0xFFC8C8C8L,
            alertArgb = 0xFFE6E6E6L,
            textArgb = 0xFFFFFFFFL
        )

        // --- 3. Sépia (3 niveaux de contraste) ---
        val SEPIA_SOFT = CyberPalette(
            preset = CyberThemePreset.SEPIA_SOFT,
            backgroundArgb = 0xFF211A14L,
            surfaceArgb = 0xFF2E241CL,
            primaryArgb = 0xFFD9AE7EL,
            secondaryArgb = 0xFFB88B5EL,
            alertArgb = 0xFFEAC79BL,
            textArgb = 0xFFEBD9C5L
        )

        val SEPIA_CLASSIC = CyberPalette(
            preset = CyberThemePreset.SEPIA_CLASSIC,
            backgroundArgb = 0xFF130D08L,
            surfaceArgb = 0xFF21170FL,
            primaryArgb = 0xFFEBB36CL,
            secondaryArgb = 0xFFD18042L,
            alertArgb = 0xFFF7D479L,
            textArgb = 0xFFF6E4CCL
        )

        val SEPIA_HIGH = CyberPalette(
            preset = CyberThemePreset.SEPIA_HIGH,
            backgroundArgb = 0xFF070402L,
            surfaceArgb = 0xFF150E08L,
            primaryArgb = 0xFFFFCC80L,
            secondaryArgb = 0xFFFFA726L,
            alertArgb = 0xFFFFE57FL,
            textArgb = 0xFFFFF4E3L
        )

        // --- 4. Accessibilité Daltonisme ---
        /**
         * Protanopie / Deutéranopie (déficience axe Rouge-Vert) :
         * Basé sur la palette scientifique Okabe-Ito (Bleu ciel #56B4E9 / Orange #E69F00 / Jaune #F0E442).
         */
        val DALTONISM_PROTA_DEUTERA = CyberPalette(
            preset = CyberThemePreset.DALTONISM_PROTA_DEUTERA,
            backgroundArgb = 0xFF080C14L,
            surfaceArgb = 0xFF111827L,
            primaryArgb = 0xFF56B4E9L,
            secondaryArgb = 0xFFE69F00L,
            alertArgb = 0xFFF0E442L,
            textArgb = 0xFFF3F6FAL
        )

        /**
         * Tritanopie (déficience axe Bleu-Jaune) :
         * Basé sur une opposition franche Cyan-Turquoise (#00E5CC) vs Cramoisi-Corail (#FF3B6B).
         */
        val DALTONISM_TRITANOPIA = CyberPalette(
            preset = CyberThemePreset.DALTONISM_TRITANOPIA,
            backgroundArgb = 0xFF0A0C10L,
            surfaceArgb = 0xFF141820L,
            primaryArgb = 0xFF00E5CCL,
            secondaryArgb = 0xFFFF3B6BL,
            alertArgb = 0xFFFF9EB5L,
            textArgb = 0xFFF5F8FAL
        )

        /**
         * Achromatopsie / Monochromatie totale :
         * Hiérarchie purement lumineuse à fort contraste et surfaces nettement étagées.
         */
        val DALTONISM_ACHROMATOPSIA = CyberPalette(
            preset = CyberThemePreset.DALTONISM_ACHROMATOPSIA,
            backgroundArgb = 0xFF050505L,
            surfaceArgb = 0xFF171717L,
            primaryArgb = 0xFFFFFFFFL,
            secondaryArgb = 0xFFB8B8B8L,
            alertArgb = 0xFFE2E2E2L,
            textArgb = 0xFFFAFAFAL
        )

        fun fromPreset(preset: CyberThemePreset): CyberPalette = when (preset) {
            CyberThemePreset.NEON_NIGHT -> NEON_NIGHT
            CyberThemePreset.TERMINAL_PHOSPHOR -> TERMINAL_PHOSPHOR
            CyberThemePreset.CRT_AMBER -> CRT_AMBER
            CyberThemePreset.POCKET_MONO -> POCKET_MONO
            CyberThemePreset.OLED_MINIMAL -> OLED_MINIMAL
            CyberThemePreset.GRAYSCALE_SOFT -> GRAYSCALE_SOFT
            CyberThemePreset.GRAYSCALE_BALANCED -> GRAYSCALE_BALANCED
            CyberThemePreset.GRAYSCALE_HIGH -> GRAYSCALE_HIGH
            CyberThemePreset.SEPIA_SOFT -> SEPIA_SOFT
            CyberThemePreset.SEPIA_CLASSIC -> SEPIA_CLASSIC
            CyberThemePreset.SEPIA_HIGH -> SEPIA_HIGH
            CyberThemePreset.DALTONISM_PROTA_DEUTERA -> DALTONISM_PROTA_DEUTERA
            CyberThemePreset.DALTONISM_TRITANOPIA -> DALTONISM_TRITANOPIA
            CyberThemePreset.DALTONISM_ACHROMATOPSIA -> DALTONISM_ACHROMATOPSIA
            CyberThemePreset.CUSTOM -> NEON_NIGHT.copy(preset = CyberThemePreset.CUSTOM)
        }

        /**
         * Génère une palette procédurale harmonieuse (accords complémentaires/triadiques + contraste WCAG AA/AAA garanti).
         */
        fun randomHarmonious(rng: Random = Random.Default): CyberPalette {
            val baseHue = rng.nextFloat() * 360f
            val secondaryHue = (baseHue + 135f + rng.nextFloat() * 90f) % 360f
            val alertHue = (baseHue + 55f + rng.nextFloat() * 50f) % 360f

            val bgArgb = hsvToArgbPure(
                h = baseHue,
                s = 0.35f + rng.nextFloat() * 0.25f,
                v = 0.05f + rng.nextFloat() * 0.04f
            )
            val surfaceArgb = hsvToArgbPure(
                h = baseHue,
                s = 0.30f + rng.nextFloat() * 0.25f,
                v = 0.12f + rng.nextFloat() * 0.05f
            )
            val primaryArgb = hsvToArgbPure(
                h = baseHue,
                s = 0.78f + rng.nextFloat() * 0.22f,
                v = 0.92f + rng.nextFloat() * 0.08f
            )
            val secondaryArgb = hsvToArgbPure(
                h = secondaryHue,
                s = 0.78f + rng.nextFloat() * 0.22f,
                v = 0.92f + rng.nextFloat() * 0.08f
            )
            val alertArgb = hsvToArgbPure(
                h = alertHue,
                s = 0.82f + rng.nextFloat() * 0.18f,
                v = 0.95f + rng.nextFloat() * 0.05f
            )
            val textArgb = hsvToArgbPure(
                h = baseHue,
                s = 0.06f + rng.nextFloat() * 0.08f,
                v = 0.94f + rng.nextFloat() * 0.06f
            )

            return CyberPalette(
                preset = CyberThemePreset.CUSTOM,
                backgroundArgb = bgArgb,
                surfaceArgb = surfaceArgb,
                primaryArgb = primaryArgb,
                secondaryArgb = secondaryArgb,
                alertArgb = alertArgb,
                textArgb = textArgb
            )
        }

        /**
         * Génère une palette 100 % aléatoire débridée ("Random Chaos") sans aucune contrainte chromatique.
         */
        fun randomChaos(rng: Random = Random.Default): CyberPalette {
            fun nextRgb(): Long = 0xFF000000L or rng.nextLong(0x1000000L)
            return CyberPalette(
                preset = CyberThemePreset.CUSTOM,
                backgroundArgb = nextRgb(),
                surfaceArgb = nextRgb(),
                primaryArgb = nextRgb(),
                secondaryArgb = nextRgb(),
                alertArgb = nextRgb(),
                textArgb = nextRgb()
            )
        }

        /**
         * Conversion HSV -> ARGB pure Kotlin (sans dépendance `android.graphics.Color`).
         */
        fun hsvToArgbPure(h: Float, s: Float, v: Float): Long {
            val hue = ((h % 360f) + 360f) % 360f
            val sat = s.coerceIn(0f, 1f)
            val value = v.coerceIn(0f, 1f)

            val c = value * sat
            val x = c * (1f - abs(((hue / 60f) % 2f) - 1f))
            val m = value - c

            val (rPrime, gPrime, bPrime) = when {
                hue < 60f -> Triple(c, x, 0f)
                hue < 120f -> Triple(x, c, 0f)
                hue < 180f -> Triple(0f, c, x)
                hue < 240f -> Triple(0f, x, c)
                hue < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            val r = ((rPrime + m) * 255f).toInt().coerceIn(0, 255).toLong()
            val g = ((gPrime + m) * 255f).toInt().coerceIn(0, 255).toLong()
            val b = ((bPrime + m) * 255f).toInt().coerceIn(0, 255).toLong()
            return 0xFF000000L or (r shl 16) or (g shl 8) or b
        }

        /**
         * Calcule la luminance relative sRGB selon la norme WCAG 2.1 (`0.0` noir à `1.0` blanc).
         */
        fun relativeLuminance(argb: Long): Double {
            val r = ((argb shr 16) and 0xFFL) / 255.0
            val g = ((argb shr 8) and 0xFFL) / 255.0
            val b = (argb and 0xFFL) / 255.0

            fun channelLum(c: Double): Double =
                if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

            return 0.2126 * channelLum(r) + 0.7152 * channelLum(g) + 0.0722 * channelLum(b)
        }

        /**
         * Calcule le ratio de contraste WCAG 2.1 entre deux couleurs ARGB (`1.0` à `21.0`).
         */
        fun wcagContrastRatio(argb1: Long, argb2: Long): Double {
            val l1 = relativeLuminance(argb1)
            val l2 = relativeLuminance(argb2)
            val lighter = max(l1, l2)
            val darker = min(l1, l2)
            return (lighter + 0.05) / (darker + 0.05)
        }
    }
}
