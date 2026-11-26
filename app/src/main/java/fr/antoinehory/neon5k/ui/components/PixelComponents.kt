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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.antoinehory.neon5k.domain.model.EntropySnapshot
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlin.math.floor

/**
 * Forme chanfreinée style terminal cyberpunk / pixel art.
 */
val CyberCutShape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp)

/**
 * Identifiants des icônes Pixel Art `8×8` dessinées sur `Canvas` pour les boutons et en-têtes.
 */
enum class PixelIconId {
    PLAY,
    GAMEPAD,
    BOLT,
    PALETTE,
    TROPHY,
    BOOK,
    INFO,
    DICE,
    SPARKLE,
    CHAOS,
    BACK,
    HEART,
    STAR,
    GRID,
    EXPORT
}

/**
 * Sprites constants `8×8` pour les icônes Pixel Art des boutons (`1` = pixel allumé, `0` = transparent).
 */
private object PixelButtonIconSprites {
    const val GRID = 8

    private fun parse8x8(rows: List<String>): IntArray {
        val result = IntArray(64)
        for (y in 0 until 8) {
            val row = rows.getOrElse(y) { "........" }
            for (x in 0 until 8) {
                result[y * 8 + x] = if (row.getOrElse(x) { '.' } == '#') 1 else 0
            }
        }
        return result
    }

    val PLAY = parse8x8(
        listOf(
            ".##.....",
            ".####...",
            ".#####..",
            ".######.",
            ".######.",
            ".#####..",
            ".####...",
            ".##....."
        )
    )

    val GAMEPAD = parse8x8(
        listOf(
            "........",
            ".######.",
            "########",
            "#.#..#.#",
            "########",
            "###..###",
            "##....##",
            "........"
        )
    )

    val BOLT = parse8x8(
        listOf(
            "....###.",
            "...###..",
            "..###...",
            ".######.",
            "...###..",
            "..###...",
            ".###....",
            "##......"
        )
    )

    val PALETTE = parse8x8(
        listOf(
            "..####..",
            ".##..##.",
            "##.#..##",
            "##...###",
            "##.#....",
            ".###.##.",
            "..#####.",
            "........"
        )
    )

    val TROPHY = parse8x8(
        listOf(
            "########",
            "#.#..#.#",
            "#.####.#",
            "..####..",
            "...##...",
            "...##...",
            "..####..",
            ".######."
        )
    )

    val BOOK = parse8x8(
        listOf(
            ".######.",
            "##....#.",
            "#.###.#.",
            "#.....#.",
            "#.###.#.",
            "#.....#.",
            "##.####.",
            ".######."
        )
    )

    val INFO = parse8x8(
        listOf(
            "..####..",
            ".#.##.#.",
            "#......#",
            "#..##..#",
            "#..##..#",
            "#..##..#",
            ".#.###..",
            "..####.."
        )
    )

    val DICE = parse8x8(
        listOf(
            ".######.",
            "########",
            "##.##.##",
            "########",
            "###..###",
            "##.##.##",
            "########",
            ".######."
        )
    )

    val SPARKLE = parse8x8(
        listOf(
            "...##...",
            "...##...",
            ".######.",
            "########",
            ".######.",
            "...##...",
            "...##...",
            "........"
        )
    )

    val CHAOS = parse8x8(
        listOf(
            "#..##..#",
            ".#.&&.#.".replace('&', '#'),
            "..####..",
            "########",
            "..####..",
            ".#.##.#.",
            "#..##..#",
            "........"
        )
    )

    val BACK = parse8x8(
        listOf(
            "...##...",
            "..##....",
            ".##.....",
            "########",
            "########",
            ".##.....",
            "..##....",
            "...##..."
        )
    )

    val HEART = parse8x8(
        listOf(
            ".##..##.",
            "########",
            "########",
            "########",
            ".######.",
            "..####..",
            "...##...",
            "........"
        )
    )

    val STAR = parse8x8(
        listOf(
            "...##...",
            "..####..",
            "########",
            ".######.",
            "..####..",
            ".##..##.",
            "##....##",
            "........"
        )
    )

    val GRID_ICON = parse8x8(
        listOf(
            "###..###",
            "###..###",
            "###..###",
            "........",
            "........",
            "###..###",
            "###..###",
            "###..###"
        )
    )

    val EXPORT = parse8x8(
        listOf(
            "...##...",
            "..####..",
            ".##..##.",
            "...##...",
            "...##...",
            "#..##..#",
            "########",
            ".######."
        )
    )

    fun forId(id: PixelIconId): IntArray = when (id) {
        PixelIconId.PLAY -> PLAY
        PixelIconId.GAMEPAD -> GAMEPAD
        PixelIconId.BOLT -> BOLT
        PixelIconId.PALETTE -> PALETTE
        PixelIconId.TROPHY -> TROPHY
        PixelIconId.BOOK -> BOOK
        PixelIconId.INFO -> INFO
        PixelIconId.DICE -> DICE
        PixelIconId.SPARKLE -> SPARKLE
        PixelIconId.CHAOS -> CHAOS
        PixelIconId.BACK -> BACK
        PixelIconId.HEART -> HEART
        PixelIconId.STAR -> STAR
        PixelIconId.GRID -> GRID_ICON
        PixelIconId.EXPORT -> EXPORT
    }
}

/**
 * Icône Pixel Art `8×8` dessinée sur `Canvas` sans allocation mémoire.
 */
@Composable
fun PixelIconCanvas(
    icon: PixelIconId,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 15.dp
) {
    val sprite = remember(icon) { PixelButtonIconSprites.forId(icon) }
    Canvas(modifier = modifier.size(iconSize)) {
        val grid = PixelButtonIconSprites.GRID
        val px = floor(size.minDimension / grid.toFloat()).coerceAtLeast(1f)
        val total = px * grid
        val startX = floor((size.width - total) / 2f)
        val startY = floor((size.height - total) / 2f)
        val cellSize = Size(px, px)

        for (y in 0 until grid) {
            val rowOffset = y * grid
            val drawY = startY + y * px
            for (x in 0 until grid) {
                if (sprite[rowOffset + x] == 1) {
                    drawRect(
                        color = tint,
                        topLeft = Offset(startX + x * px, drawY),
                        size = cellSize
                    )
                }
            }
        }
    }
}

/**
 * Barre supérieure harmonisée pour toutes les sous-activités de Neon 5K :
 * - Bouton retour compact avec icône pixel-art `BACK` à gauche,
 * - Espacement garanti (`12.dp`) pour éviter tout tassement,
 * - Cartouche de titre chanfreiné sur **1 seule ligne** (`titleMedium`, `maxLines = 1`, `TextOverflow.Ellipsis`) avec icône pixel-art dédiée.
 */
@Composable
fun NeonSubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    icon: PixelIconId? = null,
    backLabel: String = "ACCUEIL"
) {
    val colors = LocalNeon5KColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelButton(
            text = backLabel,
            icon = PixelIconId.BACK,
            onClick = onBack,
            filled = false,
            accentColor = colors.textPrimary
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .background(colors.surface.copy(alpha = 0.85f), CyberCutShape)
                .border(1.dp, colors.primaryNeon.copy(alpha = 0.45f), CyberCutShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                PixelIconCanvas(
                    icon = icon,
                    tint = colors.secondaryNeon,
                    iconSize = 15.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryNeon,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * Bouton principal à bordure néon, coins chanfreinés et icône Pixel Art optionnelle.
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalNeon5KColors.current.primaryNeon,
    filled: Boolean = true,
    icon: PixelIconId? = null
) {
    val colors = LocalNeon5KColors.current
    val effectiveTint = if (enabled) accentColor else colors.textMuted.copy(alpha = 0.4f)

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
                containerColor = accentColor.copy(alpha = 0.22f),
                contentColor = accentColor,
                disabledContainerColor = colors.surface.copy(alpha = 0.4f),
                disabledContentColor = colors.textMuted.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 11.dp),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    PixelIconCanvas(
                        icon = icon,
                        tint = effectiveTint,
                        iconSize = 15.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
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
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = CyberCutShape,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) accentColor.copy(alpha = 0.80f) else colors.textMuted.copy(alpha = 0.25f)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colors.surface.copy(alpha = 0.55f),
                contentColor = accentColor,
                disabledContentColor = colors.textMuted.copy(alpha = 0.4f)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            modifier = modifier
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    PixelIconCanvas(
                        icon = icon,
                        tint = effectiveTint,
                        iconSize = 14.dp
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                }
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
