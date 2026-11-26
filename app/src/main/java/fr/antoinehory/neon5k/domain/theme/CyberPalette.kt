package fr.antoinehory.neon5k.domain.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Thèmes prédéfinis Cyberpunk / Pixel Art de Neon 5K.
 */
enum class CyberThemePreset(val labelFr: String) {
    NEON_NIGHT("Neon Night"),
    TERMINAL_PHOSPHOR("Terminal Phosphor"),
    CRT_AMBER("CRT Amber"),
    POCKET_MONO("Pocket Mono"),
    OLED_MINIMAL("OLED Minimal"),
    CUSTOM("Custom Studio")
}

/**
 * Styles visuels de rendu des dés sur la grille `16×16` du `PixelDiceCanvas`.
 */
enum class DiceVisualStyle(val labelFr: String) {
    PIXEL_SOLID("Pixel Solid"),
    NEON_WIREFRAME("Neon Wireframe"),
    DOT_MATRIX("Dot Matrix")
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

        fun fromPreset(preset: CyberThemePreset): CyberPalette = when (preset) {
            CyberThemePreset.NEON_NIGHT -> NEON_NIGHT
            CyberThemePreset.TERMINAL_PHOSPHOR -> TERMINAL_PHOSPHOR
            CyberThemePreset.CRT_AMBER -> CRT_AMBER
            CyberThemePreset.POCKET_MONO -> POCKET_MONO
            CyberThemePreset.OLED_MINIMAL -> OLED_MINIMAL
            CyberThemePreset.CUSTOM -> NEON_NIGHT.copy(preset = CyberThemePreset.CUSTOM)
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
