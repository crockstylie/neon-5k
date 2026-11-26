package fr.antoinehory.neon5k.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.domain.achievement.AchievementId
import fr.antoinehory.neon5k.domain.achievement.AchievementUnlockNotification
import fr.antoinehory.neon5k.domain.achievement.MasteryRank
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlin.math.floor

/**
 * Rendu Compose `Canvas` 100 % Pixel Art (`16×16`) d'une icône de succès [AchievementId] (Lot 4).
 * - Si [isUnlocked] est vrai : dessine les pixels aux couleurs néon du thème actif (`1` = primaire, `2` = secondaire, `3` = alerte)
 *   avec une bordure colorée selon le rang de maîtrise ([masteryRank]).
 * - Si [isUnlocked] est faux : dessine l'icône en wireframe grisé atténué.
 */
@Composable
fun PixelAchievementIconCanvas(
    achievementId: AchievementId,
    isUnlocked: Boolean,
    masteryRank: MasteryRank,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp
) {
    val colors = LocalNeon5KColors.current

    val rankBorderColor = when (masteryRank) {
        MasteryRank.LOCKED -> colors.textMuted.copy(alpha = 0.25f)
        MasteryRank.BRONZE -> colors.primaryNeon.copy(alpha = 0.7f)
        MasteryRank.SILVER -> colors.secondaryNeon
        MasteryRank.GOLD -> colors.alertNeon
        MasteryRank.CYBER_PLATINUM -> colors.primaryNeon
    }

    Box(
        modifier = modifier
            .size(iconSize)
            .background(colors.background, CyberCutShape)
            .border(
                width = if (masteryRank == MasteryRank.CYBER_PLATINUM || masteryRank == MasteryRank.GOLD) 2.dp else 1.dp,
                color = rankBorderColor,
                shape = CyberCutShape
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(iconSize - 8.dp)) {
            val grid = 16
            val pixelSize = floor(size.minDimension / grid).coerceAtLeast(1f)
            val totalGridPx = pixelSize * grid
            val startX = (size.width - totalGridPx) / 2f
            val startY = (size.height - totalGridPx) / 2f

            val c1: Color = if (isUnlocked) colors.primaryNeon else colors.textMuted.copy(alpha = 0.28f)
            val c2: Color = if (isUnlocked) colors.secondaryNeon else colors.textMuted.copy(alpha = 0.20f)
            val c3: Color = if (isUnlocked) colors.alertNeon else colors.textMuted.copy(alpha = 0.35f)

            val matrix = achievementId.iconMatrix16x16
            for (row in 0 until minOf(grid, matrix.size)) {
                val line = matrix[row]
                for (col in 0 until minOf(grid, line.length)) {
                    val ch = line[col]
                    val pixelColor = when (ch) {
                        '1' -> c1
                        '2' -> c2
                        '3' -> c3
                        else -> null
                    }
                    if (pixelColor != null) {
                        drawRect(
                            color = pixelColor,
                            topLeft = Offset(startX + col * pixelSize, startY + row * pixelSize),
                            size = Size(pixelSize, pixelSize)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bandeau cyberpunk non-bloquant affiché en haut de `GameScreen` lorsqu'un succès est débloqué
 * ou monte en rang de maîtrise (*Bronze → Argent → Or → Cyber-Platine*).
 */
@Composable
fun AchievementToastBanner(
    notification: AchievementUnlockNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val accent = if (notification.isFirstUnlock) colors.alertNeon else colors.secondaryNeon

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, CyberCutShape)
            .border(width = 1.5.dp, color = accent, shape = CyberCutShape)
            .clickable { onDismiss() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PixelAchievementIconCanvas(
                achievementId = notification.achievementId,
                isUnlocked = true,
                masteryRank = notification.newRank,
                iconSize = 44.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                val header = if (notification.isFirstUnlock) {
                    "★ SUCCÈS DÉVERROUILLÉ (+${notification.achievementId.playGamesXp} XP)"
                } else {
                    "▲ RANG DE MAÎTRISE : ${notification.newRank.labelFr} (×${notification.lifetimeCount})"
                }
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Text(
                    text = notification.achievementId.titleFr,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryNeon
                )
                Text(
                    text = notification.achievementId.descriptionFr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
