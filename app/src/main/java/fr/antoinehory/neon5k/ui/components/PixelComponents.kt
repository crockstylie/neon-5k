package fr.antoinehory.neon5k.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.antoinehory.neon5k.domain.model.EntropySnapshot
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Forme chanfreinée style terminal cyberpunk / pixel art.
 */
val CyberCutShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)

/**
 * Bouton principal à bordure néon et coins chanfreinés.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalNeon5KColors.current.primaryNeon,
    filled: Boolean = true
) {
    val colors = LocalNeon5KColors.current
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = CyberCutShape,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (enabled) accentColor else colors.textMuted.copy(alpha = 0.3f)
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor.copy(alpha = 0.20f),
                contentColor = accentColor,
                disabledContainerColor = colors.surface.copy(alpha = 0.4f),
                disabledContentColor = colors.textMuted.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            modifier = modifier
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = CyberCutShape,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) accentColor.copy(alpha = 0.75f) else colors.textMuted.copy(alpha = 0.25f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = accentColor,
                disabledContentColor = colors.textMuted.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = modifier
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Panneau encadré dans l'esthétique terminal cyberpunk.
 */
@Composable
fun CyberPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = LocalNeon5KColors.current.primaryNeon.copy(alpha = 0.35f),
    content: @Composable () -> Unit
) {
    val colors = LocalNeon5KColors.current
    Box(
        modifier = modifier
            .background(color = colors.surface, shape = CyberCutShape)
            .border(width = 1.dp, color = borderColor, shape = CyberCutShape)
            .padding(12.dp)
    ) {
        content()
    }
}

/**
 * Surcouche CRT Scanlines subtile et non-interactive dessinée par-dessus l'écran si `showScanlines == true`.
 */
@Composable
fun ScanlineOverlay(
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    if (!colors.showScanlines) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val stepPx = 6.dp.toPx().coerceAtLeast(4f)
        val lineHeight = 1.dp.toPx().coerceAtLeast(1f)
        val scanlineColor = Color.Black.copy(alpha = 0.14f)
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = scanlineColor,
                topLeft = Offset(0f, y),
                size = Size(size.width, lineHeight)
            )
            y += stepPx
        }
    }
}

/**
 * Modale « Trace d'Entropie » affichant les mesures matérielles (gyroscope, accéléromètre, horloge nano, tactile, sel)
 * et le hash `SHA-256` ayant généré le dernier lancer de dés.
 */
@Composable
fun EntropyBadgeDialog(
    snapshot: EntropySnapshot,
    onDismiss: () -> Unit
) {
    val colors = LocalNeon5KColors.current
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TRACE D'ENTROPIE ${snapshot.shortHashHex}",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryNeon
                    )
                    Text(
                        text = "LANCER #${snapshot.rollCounter}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryNeon
                    )
                }

                Text(
                    text = "Ce lancer a été forgé par hachage SHA-256 à partir des signaux physiques et temporels capturés à l'instant exact du tirage :",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )

                CyberPanel(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = colors.primaryNeon.copy(alpha = 0.25f)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EntropyLine(label = "GYROSCOPE", value = snapshot.gyroSummary)
                        EntropyLine(label = "ACCÉLÉROMÈTRE", value = snapshot.accelSummary)
                        EntropyLine(label = "HORLOGE NANO", value = snapshot.timingSummary)
                        EntropyLine(label = "DÉCLENCHEUR", value = snapshot.touchSummary)
                        EntropyLine(label = "SEL CSPRNG", value = "0x${snapshot.saltHex}")
                        EntropyLine(label = "SEED 64-BIT", value = snapshot.seed.toString())
                    }
                }

                Text(
                    text = "SHA-256 : ${snapshot.fullHashHex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryNeon.copy(alpha = 0.85f)
                )

                PixelButton(
                    text = "FERMER LE TERMINAL",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = colors.primaryNeon
                )
            }
        }
    }
}

@Composable
private fun EntropyLine(
    label: String,
    value: String
) {
    val colors = LocalNeon5KColors.current
    Column {
        Text(
            text = "[$label]",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryNeon
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
    }
}
