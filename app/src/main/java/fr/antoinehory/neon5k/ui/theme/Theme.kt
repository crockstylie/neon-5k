package fr.antoinehory.neon5k.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle

@Composable
fun Neon5KTheme(
    palette: CyberPalette = CyberPalette.NEON_NIGHT,
    diceStyle: DiceVisualStyle = DiceVisualStyle.PIXEL_SOLID,
    showScanlines: Boolean = true,
    content: @Composable () -> Unit
) {
    val neonColors = Neon5KColors.fromPalette(
        palette = palette,
        diceStyle = diceStyle,
        showScanlines = showScanlines
    )

    val isLightBg = neonColors.background.luminance() > 0.5f

    val colorScheme = if (isLightBg) {
        lightColorScheme(
            primary = neonColors.primaryNeon,
            onPrimary = neonColors.background,
            secondary = neonColors.secondaryNeon,
            onSecondary = neonColors.background,
            tertiary = neonColors.alertNeon,
            background = neonColors.background,
            onBackground = neonColors.textPrimary,
            surface = neonColors.surface,
            onSurface = neonColors.textPrimary,
            error = neonColors.alertNeon
        )
    } else {
        darkColorScheme(
            primary = neonColors.primaryNeon,
            onPrimary = Color(0xFF06070B),
            secondary = neonColors.secondaryNeon,
            onSecondary = Color(0xFF06070B),
            tertiary = neonColors.alertNeon,
            background = neonColors.background,
            onBackground = neonColors.textPrimary,
            surface = neonColors.surface,
            onSurface = neonColors.textPrimary,
            error = neonColors.alertNeon
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = isLightBg
            controller.isAppearanceLightNavigationBars = isLightBg
        }
    }

    CompositionLocalProvider(LocalNeon5KColors provides neonColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
