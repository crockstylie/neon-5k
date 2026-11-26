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

/**
 * Sélecteur de couleur HSV (Hue / Saturation / Value) + saisie directe `#RRGGBB` avec contrôle du ratio de contraste WCAG 2.1.
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

    val initialHsv = remember(initialArgb) { argbToHsv(initialArgb) }
    var hue by remember(initialArgb) { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember(initialArgb) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initialArgb) { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember(initialArgb) { mutableStateOf(argbToHexRgb(initialArgb)) }

    val currentArgb = hsvToArgb(hue, saturation, value)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = "#$hexInput",
                            style = MaterialTheme.typography.titleMedium,
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

                // Sliders HSV
                HsvSliderRow(
                    label = "TEINTE (H : ${hue.toInt()}°)",
                    value = hue,
                    valueRange = 0f..360f,
                    accentColor = colors.primaryNeon,
                    onValueChange = {
                        hue = it
                        hexInput = argbToHexRgb(hsvToArgb(hue, saturation, value))
                    }
                )

                HsvSliderRow(
                    label = "SATURATION (S : ${(saturation * 100).toInt()} %)",
                    value = saturation,
                    valueRange = 0f..1f,
                    accentColor = colors.secondaryNeon,
                    onValueChange = {
                        saturation = it
                        hexInput = argbToHexRgb(hsvToArgb(hue, saturation, value))
                    }
                )

                HsvSliderRow(
                    label = "LUMINOSITÉ (V : ${(value * 100).toInt()} %)",
                    value = value,
                    valueRange = 0f..1f,
                    accentColor = colors.alertNeon,
                    onValueChange = {
                        value = it
                        hexInput = argbToHexRgb(hsvToArgb(hue, saturation, value))
                    }
                )

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
                            val newHsv = argbToHsv(parsedArgb)
                            hue = newHsv[0]
                            saturation = newHsv[1]
                            value = newHsv[2]
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
private fun HsvSliderRow(
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
