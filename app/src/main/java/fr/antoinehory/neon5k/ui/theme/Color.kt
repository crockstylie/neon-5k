package fr.antoinehory.neon5k.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle

val NeonNightBg = Color(0xFF090A10)
val NeonNightSurface = Color(0xFF121521)
val NeonCyan = Color(0xFF00F0FF)
val NeonMagenta = Color(0xFFFF2A85)
val NeonAlertYellow = Color(0xFFFFE600)
val NeonText = Color(0xFFEAEFF5)

/**
 * Couleurs et paramètres visuels actifs exposés à l'ensemble des Composables de Neon 5K.
 */
@Immutable
data class Neon5KColors(
    val background: Color,
    val surface: Color,
    val primaryNeon: Color,
    val secondaryNeon: Color,
    val alertNeon: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val diceStyle: DiceVisualStyle = DiceVisualStyle.PIXEL_SOLID,
    val showScanlines: Boolean = true
) {
    companion object {
        fun fromPalette(
            palette: CyberPalette = CyberPalette.NEON_NIGHT,
            diceStyle: DiceVisualStyle = DiceVisualStyle.PIXEL_SOLID,
            showScanlines: Boolean = true
        ): Neon5KColors {
            val textColor = Color(palette.textArgb)
            return Neon5KColors(
                background = Color(palette.backgroundArgb),
                surface = Color(palette.surfaceArgb),
                primaryNeon = Color(palette.primaryArgb),
                secondaryNeon = Color(palette.secondaryArgb),
                alertNeon = Color(palette.alertArgb),
                textPrimary = textColor,
                textMuted = textColor.copy(alpha = 0.68f),
                diceStyle = diceStyle,
                showScanlines = showScanlines
            )
        }
    }
}

val LocalNeon5KColors = staticCompositionLocalOf {
    Neon5KColors.fromPalette(CyberPalette.NEON_NIGHT)
}
