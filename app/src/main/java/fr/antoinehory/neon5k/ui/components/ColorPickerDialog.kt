package fr.antoinehory.neon5k.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

private enum class ColorInputMode {
    HSV,
    RGB
}

/**
 * Sélecteur de couleur hybride HSV (Teinte / Saturation / Luminosité) & RGB (Rouge / Vert / Bleu 0..255)
 * + bouton Random + saisie directe `#RRGGBB` avec contrôle du ratio de contraste WCAG 2.1.
 */
@Composable
fun ColorPickerDialog(
    title: String,
    initialArgb: Long,
    referenceBackgroundArgb: Long,
    onDismiss: () -> Unit,
    onColorConfirmed: (Long) -> Unit
) {
    val colors = LocalNeon5KColors.current

    var mode by remember { mutableStateOf(ColorInputMode.HSV) }
    val initialHsv = remember(initialArgb) { argbToHsv(initialArgb) }
    var hue by remember(initialArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialArgb) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialArgb) { mutableFloatStateOf(initialHsv[2]) }

    var red by remember(initialArgb) { mutableIntStateOf(((initialArgb shr 16) and 0xFFL).toInt()) }
    var green by remember(initialArgb) { mutableIntStateOf(((initialArgb shr 8) and 0xFFL).toInt()) }
    var blue by remember(initialArgb) { mutableIntStateOf((initialArgb and 0xFFL).toInt()) }

    var hexInput by remember(initialArgb) { mutableStateOf(argbToHexRgb(initialArgb)) }

    fun syncFromArgb(newArgb: Long, updateHex: Boolean = true) {
        val hsv = argbToHsv(newArgb)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        red = ((newArgb shr 16) and 0xFFL).toInt()
        green = ((newArgb shr 8) and 0xFFL).toInt()
        blue = (newArgb and 0xFFL).toInt()
        if (updateHex) {
            hexInput = argbToHexRgb(newArgb)
        }
    }

    fun syncFromHsv(h: Float, s: Float, v: Float) {
        hue = h
        saturation = s
        value = v
        val argb = hsvToArgb(h, s, v)
        red = ((argb shr 16) and 0xFFL).toInt()
        green = ((argb shr 8) and 0xFFL).toInt()
        blue = (argb and 0xFFL).toInt()
        hexInput = argbToHexRgb(argb)
    }

    fun syncFromRgb(r: Int, g: Int, b: Int) {
        red = r.coerceIn(0, 255)
        green = g.coerceIn(0, 255)
        blue = b.coerceIn(0, 255)
        val argb = rgbToArgb(red, green, blue)
        val hsv = argbToHsv(argb)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexInput = argbToHexRgb(argb)
    }

    val currentArgb = if (mode == ColorInputMode.HSV) {
        hsvToArgb(hue, saturation, value)
    } else {
        rgbToArgb(red, green, blue)
    }
    val previewColor = Color(currentArgb)
    val contrastRatio = CyberPalette.wcagContrastRatio(currentArgb, referenceBackgroundArgb)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CyberCutShape,
            color = colors.background,
            border = BorderStroke(1.5.dp, colors.primaryNeon)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STUDIO COULEUR // $title",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryNeon
                )

                // Aperçu de la couleur et ratio de contraste WCAG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(previewColor, CyberCutShape)
                            .border(1.dp, colors.textPrimary.copy(alpha = 0.4f), CyberCutShape)
                    )
                    Column(modifier = Modifier.weight(1.4f)) {
                        Text(
                            text = "#$hexInput  (R:$red G:$green B:$blue)",
                            style = MaterialTheme.typography.labelLarge,
                            color = colors.textPrimary
                        )
                        val wcagOk = contrastRatio >= 4.5
                        Text(
                            text = String.format(
                                Locale.US,
                                "Contraste vs Fond : %.2f:1 %s",
                                contrastRatio,
                                if (wcagOk) "(WCAG AA ✓)" else "(< 4.5:1)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (wcagOk) colors.primaryNeon else colors.alertNeon
                        )
                    }
                }

                // Sélecteur de mode HSV / RGB + Bouton Random
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "HSV",
                        icon = PixelIconId.PALETTE,
                        filled = mode == ColorInputMode.HSV,
                        accentColor = if (mode == ColorInputMode.HSV) colors.primaryNeon else colors.textMuted,
                        onClick = { mode = ColorInputMode.HSV },
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "RGB",
                        icon = PixelIconId.PALETTE,
                        filled = mode == ColorInputMode.RGB,
                        accentColor = if (mode == ColorInputMode.RGB) colors.secondaryNeon else colors.textMuted,
                        onClick = { mode = ColorInputMode.RGB },
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "RANDOM",
                        icon = PixelIconId.DICE,
                        filled = false,
                        accentColor = colors.alertNeon,
                        onClick = {
                            val randomArgb = 0xFF000000L or Random.nextLong(0x1000000L)
                            syncFromArgb(randomArgb)
                        },
                        modifier = Modifier.weight(1.15f)
                    )
                }

                if (mode == ColorInputMode.HSV) {
                    ColorChannelSliderRow(
                        label = "TEINTE (H : ${hue.toInt()}°)",
                        value = hue,
                        valueRange = 0f..360f,
                        accentColor = colors.primaryNeon,
                        onValueChange = { syncFromHsv(it, saturation, value) }
                    )

                    ColorChannelSliderRow(
                        label = "SATURATION (S : ${(saturation * 100).toInt()} %)",
                        value = saturation,
                        valueRange = 0f..1f,
                        accentColor = colors.secondaryNeon,
                        onValueChange = { syncFromHsv(hue, it, value) }
                    )

                    ColorChannelSliderRow(
                        label = "LUMINOSITÉ (V : ${(value * 100).toInt()} %)",
                        value = value,
                        valueRange = 0f..1f,
                        accentColor = colors.alertNeon,
                        onValueChange = { syncFromHsv(hue, saturation, it) }
                    )
                } else {
                    ColorChannelSliderRow(
                        label = "ROUGE (R : $red / 255)",
                        value = red.toFloat(),
                        valueRange = 0f..255f,
                        accentColor = Color(0xFFFF4D6D),
                        onValueChange = { syncFromRgb(it.roundToInt(), green, blue) }
                    )

                    ColorChannelSliderRow(
                        label = "VERT (G : $green / 255)",
                        value = green.toFloat(),
                        valueRange = 0f..255f,
                        accentColor = Color(0xFF39FF14),
                        onValueChange = { syncFromRgb(red, it.roundToInt(), blue) }
                    )

                    ColorChannelSliderRow(
                        label = "BLEU (B : $blue / 255)",
                        value = blue.toFloat(),
                        valueRange = 0f..255f,
                        accentColor = Color(0xFF00E5FF),
                        onValueChange = { syncFromRgb(red, green, it.roundToInt()) }
                    )
                }

                // Saisie directe #RRGGBB
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase(Locale.US)
                            .filter { it in '0'..'9' || it in 'A'..'F' }
                            .take(6)
                        hexInput = cleaned
                        if (cleaned.length == 6) {
                            val parsedArgb = 0xFF000000L or cleaned.toLong(16)
                            syncFromArgb(parsedArgb, updateHex = false)
                        }
                    },
                    singleLine = true,
                    label = { Text("Code Hexadécimal (#RRGGBB)", style = MaterialTheme.typography.labelSmall) },
                    prefix = { Text("#") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primaryNeon,
                        unfocusedBorderColor = colors.primaryNeon.copy(alpha = 0.35f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "ANNULER",
                        onClick = onDismiss,
                        filled = false,
                        accentColor = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "APPLIQUER",
                        onClick = { onColorConfirmed(currentArgb) },
                        accentColor = colors.primaryNeon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorChannelSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accentColor: Color,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalNeon5KColors.current
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = colors.surface
            )
        )
    }
}

private fun argbToHexRgb(argb: Long): String =
    String.format(Locale.US, "%06X", argb and 0xFFFFFFL)

private fun argbToHsv(argb: Long): FloatArray {
    val r = ((argb shr 16) and 0xFFL).toInt()
    val g = ((argb shr 8) and 0xFFL).toInt()
    val b = (argb and 0xFFL).toInt()
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    return hsv
}

private fun hsvToArgb(h: Float, s: Float, v: Float): Long {
    val intColor = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
    return intColor.toLong() and 0xFFFFFFFFL
}

private fun rgbToArgb(r: Int, g: Int, b: Int): Long =
    0xFF000000L or ((r.toLong() and 0xFFL) shl 16) or ((g.toLong() and 0xFFL) shl 8) or (b.toLong() and 0xFFL)
