package fr.antoinehory.neon5k.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlin.math.floor

/**
 * Matrices constantes immuables `16×16` pour le rendu Pixel Art des dés (`0`..`6`) sans allocation dans `Canvas`.
 * Codes de cellule :
 * - `0` : Vide (transparent)
 * - `1` : Bordure extérieure / chanfrein pixel-art du dé
 * - `2` : Corps intérieur du dé
 * - `3` : Point (pip) de la face (`1..6`) ou motif central `?` (`0`)
 */
object PixelDiceSprites {
    const val GRID_SIZE = 16

    // Boîtier 16x16 avec coins coupés pixel-art (chanfrein 1px aux angles)
    private val BASE_SHELL: IntArray = IntArray(256) { index ->
        val x = index % GRID_SIZE
        val y = index / GRID_SIZE
        when {
            // Coins extérieurs coupés -> transparent
            (x == 0 || x == 15) && (y == 0 || y == 15) -> 0
            // Bordure de 1px + pixels diagonaux des coins
            x == 0 || x == 15 || y == 0 || y == 15 -> 1
            (x == 1 || x == 14) && (y == 1 || y == 14) -> 1
            else -> 2
        }
    }

    // Coordonnées (x, y) du coin supérieur gauche de chaque point (pip 2x2) sur la grille 16x16
    private val PIP_TOP_LEFT = 3 to 3
    private val PIP_TOP_RIGHT = 11 to 3
    private val PIP_MID_LEFT = 3 to 7
    private val PIP_CENTER = 7 to 7
    private val PIP_MID_RIGHT = 11 to 7
    private val PIP_BOTTOM_LEFT = 3 to 11
    private val PIP_BOTTOM_RIGHT = 11 to 11

    private fun buildSpriteWithPips(pips: List<Pair<Int, Int>>): IntArray {
        val matrix = BASE_SHELL.copyOf()
        for ((px, py) in pips) {
            for (dy in 0..1) {
                for (dx in 0..1) {
                    matrix[(py + dy) * GRID_SIZE + (px + dx)] = 3
                }
            }
        }
        return matrix
    }

    // Face 0 (avant le premier lancer) : croix centrale cyber discrète
    private val SPRITE_0: IntArray = BASE_SHELL.copyOf().apply {
        this[7 * GRID_SIZE + 7] = 3
        this[7 * GRID_SIZE + 8] = 3
        this[8 * GRID_SIZE + 7] = 3
        this[8 * GRID_SIZE + 8] = 3
    }

    private val SPRITE_1 = buildSpriteWithPips(listOf(PIP_CENTER))
    private val SPRITE_2 = buildSpriteWithPips(listOf(PIP_TOP_LEFT, PIP_BOTTOM_RIGHT))
    private val SPRITE_3 = buildSpriteWithPips(listOf(PIP_TOP_LEFT, PIP_CENTER, PIP_BOTTOM_RIGHT))
    private val SPRITE_4 = buildSpriteWithPips(listOf(PIP_TOP_LEFT, PIP_TOP_RIGHT, PIP_BOTTOM_LEFT, PIP_BOTTOM_RIGHT))
    private val SPRITE_5 = buildSpriteWithPips(
        listOf(PIP_TOP_LEFT, PIP_TOP_RIGHT, PIP_CENTER, PIP_BOTTOM_LEFT, PIP_BOTTOM_RIGHT)
    )
    private val SPRITE_6 = buildSpriteWithPips(
        listOf(PIP_TOP_LEFT, PIP_TOP_RIGHT, PIP_MID_LEFT, PIP_MID_RIGHT, PIP_BOTTOM_LEFT, PIP_BOTTOM_RIGHT)
    )

    private val ALL_SPRITES: Array<IntArray> = arrayOf(
        SPRITE_0,
        SPRITE_1,
        SPRITE_2,
        SPRITE_3,
        SPRITE_4,
        SPRITE_5,
        SPRITE_6
    )

    fun spriteForFace(face: Int): IntArray = ALL_SPRITES[face.coerceIn(0, 6)]
}

/**
 * Rendu Pixel Art natif Compose `Canvas` d'un dé sur une grille `16×16` alignée à l'entier inférieur.
 *
 * Trois styles visuels distincts :
 * - [DiceVisualStyle.PIXEL_SOLID] : bloc rétro plein avec remplissage coloré saturé, biseau 3D clair/sombre et points contrastés.
 * - [DiceVisualStyle.NEON_WIREFRAME] : hologramme filaire transparent à double cadre CAO, mire interne et points vectoriels creux.
 * - [DiceVisualStyle.DOT_MATRIX] : afficheur matriciel à diodes LED circulaires (diodes éteintes en fond + diodes allumées pour le cadre et les points).
 */
@Composable
fun PixelDiceCanvas(
    die: DieState,
    rollTrigger: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dieSize: Dp = 64.dp,
    styleOverride: DiceVisualStyle? = null,
    alwaysClickable: Boolean = false,
    customTint: Color? = null
) {
    val colors = LocalNeon5KColors.current
    val style = styleOverride ?: colors.diceStyle

    val targetScale = when {
        die.isSelected -> 1.08f
        die.isLocked -> 0.92f
        else -> 1.0f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 180),
        label = "dieScale"
    )

    val rotationAnim = remember(die.index) { Animatable(0f) }
    var lastObservedRollTrigger by remember(die.index) { mutableIntStateOf(rollTrigger) }

    LaunchedEffect(rollTrigger) {
        if (rollTrigger != lastObservedRollTrigger) {
            lastObservedRollTrigger = rollTrigger
            if (!die.isLocked && die.face > 0) {
                rotationAnim.snapTo(0f)
                rotationAnim.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = 280)
                )
                rotationAnim.snapTo(0f)
            } else {
                rotationAnim.snapTo(0f)
            }
        }
    }

    val borderColor: Color
    val solidFillColor: Color
    val solidHighlightBevel: Color
    val solidShadowBevel: Color
    val pipColor: Color
    val haloColor: Color?

    when {
        customTint != null -> {
            borderColor = customTint
            solidFillColor = customTint.copy(alpha = 0.36f)
            solidHighlightBevel = customTint.copy(alpha = 0.65f)
            solidShadowBevel = Color.Black.copy(alpha = 0.55f)
            pipColor = Color(0xFFFFF8CC)
            haloColor = customTint.copy(alpha = 0.25f)
        }

        die.isLocked -> {
            borderColor = colors.alertNeon.copy(alpha = 0.70f)
            solidFillColor = colors.alertNeon.copy(alpha = 0.26f)
            solidHighlightBevel = colors.alertNeon.copy(alpha = 0.45f)
            solidShadowBevel = colors.background.copy(alpha = 0.75f)
            pipColor = colors.alertNeon
            haloColor = null
        }

        die.isSelected -> {
            borderColor = colors.secondaryNeon
            solidFillColor = colors.secondaryNeon.copy(alpha = 0.44f)
            solidHighlightBevel = colors.textPrimary.copy(alpha = 0.60f)
            solidShadowBevel = colors.background.copy(alpha = 0.70f)
            pipColor = colors.textPrimary
            haloColor = colors.secondaryNeon.copy(alpha = 0.30f)
        }

        die.canScoreOnCurrentRoll -> {
            borderColor = colors.primaryNeon
            solidFillColor = colors.primaryNeon.copy(alpha = 0.32f)
            solidHighlightBevel = colors.primaryNeon.copy(alpha = 0.58f)
            solidShadowBevel = colors.background.copy(alpha = 0.70f)
            pipColor = colors.textPrimary
            haloColor = colors.primaryNeon.copy(alpha = 0.18f)
        }

        die.face == 0 -> {
            borderColor = colors.primaryNeon.copy(alpha = 0.40f)
            solidFillColor = colors.primaryNeon.copy(alpha = 0.14f)
            solidHighlightBevel = colors.primaryNeon.copy(alpha = 0.25f)
            solidShadowBevel = colors.background.copy(alpha = 0.65f)
            pipColor = colors.primaryNeon.copy(alpha = 0.50f)
            haloColor = null
        }

        else -> {
            // Dé mort (non scorant sur ce lancer)
            borderColor = colors.textMuted.copy(alpha = 0.32f)
            solidFillColor = colors.textMuted.copy(alpha = 0.14f)
            solidHighlightBevel = colors.textMuted.copy(alpha = 0.22f)
            solidShadowBevel = colors.background.copy(alpha = 0.75f)
            pipColor = colors.textMuted.copy(alpha = 0.50f)
            haloColor = null
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val stateLabel = when {
        die.isLocked -> "verrouillé"
        die.isSelected -> "sélectionné"
        die.canScoreOnCurrentRoll -> "scorant"
        else -> "inactif"
    }

    Box(
        modifier = modifier
            .size(dieSize)
            .semantics {
                contentDescription = "Dé ${die.index + 1}, face ${die.face}, $stateLabel"
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = alwaysClickable || (!die.isLocked && die.canScoreOnCurrentRoll),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        val sprite = remember(die.face) { PixelDiceSprites.spriteForFace(die.face) }

        Canvas(
            modifier = Modifier
                .size(dieSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotationAnim.value
                }
        ) {
            val grid = PixelDiceSprites.GRID_SIZE
            val px = floor(size.minDimension / grid.toFloat()).coerceAtLeast(1f)
            val totalGridPx = px * grid
            val startX = floor((size.width - totalGridPx) / 2f)
            val startY = floor((size.height - totalGridPx) / 2f)

            if (haloColor != null) {
                drawRect(
                    color = haloColor,
                    topLeft = Offset(startX - px, startY - px),
                    size = Size(totalGridPx + px * 2f, totalGridPx + px * 2f)
                )
            }

            when (style) {
                DiceVisualStyle.PIXEL_SOLID -> {
                    // Fond opaque sous le bloc pour isoler de la surface du panneau
                    drawRect(
                        color = colors.background,
                        topLeft = Offset(startX + px, startY + px),
                        size = Size(totalGridPx - px * 2f, totalGridPx - px * 2f)
                    )
                    val cellSize = Size(px, px)
                    for (y in 0 until grid) {
                        val rowOffset = y * grid
                        val drawY = startY + y * px
                        for (x in 0 until grid) {
                            val cellCode = sprite[rowOffset + x]
                            if (cellCode == 0) continue

                            val cellColor = when (cellCode) {
                                1 -> borderColor
                                2 -> when {
                                    // Biseau 3D arcade haut/gauche clair
                                    x == 1 || y == 1 -> solidHighlightBevel
                                    // Biseau 3D arcade bas/droite sombre
                                    x == 14 || y == 14 -> solidShadowBevel
                                    else -> solidFillColor
                                }
                                3 -> pipColor
                                else -> Color.Transparent
                            }

                            drawRect(
                                color = cellColor,
                                topLeft = Offset(startX + x * px, drawY),
                                size = cellSize
                            )
                        }
                    }
                }

                DiceVisualStyle.NEON_WIREFRAME -> {
                    // Fond 100 % transparent/creux + double cadre filaire CAO + mire interne + points vectoriels
                    val cellSize = Size(px, px)
                    val wireInnerColor = borderColor.copy(alpha = 0.24f)
                    val wireCrosshairColor = borderColor.copy(alpha = 0.38f)
                    val wirePipColor = if (die.canScoreOnCurrentRoll && !die.isSelected && !die.isLocked && customTint == null) {
                        colors.primaryNeon
                    } else {
                        pipColor
                    }

                    for (y in 0 until grid) {
                        val rowOffset = y * grid
                        val drawY = startY + y * px
                        for (x in 0 until grid) {
                            val cellCode = sprite[rowOffset + x]
                            if (cellCode == 0) continue
                            val topLeft = Offset(startX + x * px, drawY)

                            when (cellCode) {
                                1 -> {
                                    drawRect(
                                        color = borderColor,
                                        topLeft = topLeft,
                                        size = cellSize
                                    )
                                }
                                2 -> {
                                    // Cadre filaire interne à 1px d'écart + encoches de mire au centre
                                    val isInnerFrame = ((x == 2 || x == 13) && y in 2..13) ||
                                        ((y == 2 || y == 13) && x in 2..13)
                                    val isCrosshairTick = ((x == 7 || x == 8) && (y == 2 || y == 13)) ||
                                        ((y == 7 || y == 8) && (x == 2 || x == 13))
                                    if (isCrosshairTick) {
                                        drawRect(
                                            color = wireCrosshairColor,
                                            topLeft = topLeft,
                                            size = cellSize
                                        )
                                    } else if (isInnerFrame) {
                                        drawRect(
                                            color = wireInnerColor,
                                            topLeft = topLeft,
                                            size = cellSize
                                        )
                                    }
                                }
                                3 -> {
                                    // Point filaire (contour vectoriel néon + coeur semi-transparent)
                                    drawRect(
                                        color = wirePipColor.copy(alpha = 0.25f),
                                        topLeft = topLeft,
                                        size = cellSize
                                    )
                                    drawRect(
                                        color = wirePipColor,
                                        topLeft = topLeft,
                                        size = cellSize,
                                        style = Stroke(width = (px * 0.32f).coerceAtLeast(1f))
                                    )
                                }
                            }
                        }
                    }
                }

                DiceVisualStyle.DOT_MATRIX -> {
                    // Plaque matricielle sombre + diodes LED circulaires espacées (éteintes / bordure / points)
                    drawRect(
                        color = colors.background.copy(alpha = 0.90f),
                        topLeft = Offset(startX + px * 0.5f, startY + px * 0.5f),
                        size = Size(totalGridPx - px, totalGridPx - px)
                    )
                    val unlitLedColor = borderColor.copy(alpha = 0.14f)
                    val borderLedColor = borderColor.copy(alpha = 0.85f)
                    val ledRadius = (px * 0.36f).coerceAtLeast(1f)
                    val pipLedRadius = (px * 0.44f).coerceAtLeast(1.2f)

                    for (y in 0 until grid) {
                        val rowOffset = y * grid
                        val centerY = startY + (y + 0.5f) * px
                        for (x in 0 until grid) {
                            val cellCode = sprite[rowOffset + x]
                            if (cellCode == 0) continue
                            val center = Offset(startX + (x + 0.5f) * px, centerY)

                            when (cellCode) {
                                1 -> {
                                    drawCircle(
                                        color = borderLedColor,
                                        radius = ledRadius,
                                        center = center
                                    )
                                }
                                2 -> {
                                    drawCircle(
                                        color = unlitLedColor,
                                        radius = ledRadius * 0.78f,
                                        center = center
                                    )
                                }
                                3 -> {
                                    drawCircle(
                                        color = pipColor.copy(alpha = 0.30f),
                                        radius = pipLedRadius * 1.25f,
                                        center = center
                                    )
                                    drawCircle(
                                        color = pipColor,
                                        radius = pipLedRadius,
                                        center = center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
