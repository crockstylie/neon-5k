package fr.antoinehory.neon5k.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlinx.coroutines.launch

private val StarWarsYellow = Color(0xFFFFE81F)
private val DeepSpaceBlack = Color(0xFF020308)
private val IntroCyan = Color(0xFF4BD5EE)

// Matrice 5x7 du chiffre '4' composée de dés (valeur > 0 = face du dé à afficher, 0 = espace vide)
private val DIGIT_4_DICE_GRID: List<IntArray> = listOf(
    intArrayOf(4, 0, 0, 2, 0),
    intArrayOf(4, 0, 0, 2, 0),
    intArrayOf(4, 0, 0, 2, 0),
    intArrayOf(4, 4, 4, 4, 4),
    intArrayOf(0, 0, 0, 2, 0),
    intArrayOf(0, 0, 0, 2, 0),
    intArrayOf(0, 0, 0, 2, 0)
)

// Matrice 5x7 du chiffre '2' composée de dés
private val DIGIT_2_DICE_GRID: List<IntArray> = listOf(
    intArrayOf(2, 2, 2, 2, 2),
    intArrayOf(0, 0, 0, 0, 2),
    intArrayOf(0, 0, 0, 0, 2),
    intArrayOf(2, 2, 2, 2, 2),
    intArrayOf(2, 0, 0, 0, 0),
    intArrayOf(2, 0, 0, 0, 0),
    intArrayOf(2, 2, 2, 2, 2)
)

/**
 * Easter Egg #1 (42 taps sur la zone des dés de l'écran d'accueil) :
 * Affiche un grand **42** jaune composé de vrais dés Pixel Art du jeu (`PixelDiceCanvas`),
 * suivi de *"Answer to the Ultimate Question of Life, the Universe, and Everything"* (toujours en anglais original)
 * qui s'éloigne en perspective inclinée dans l'espace comme le générique d'ouverture de Star Wars.
 */
@Composable
fun StarWars42EasterEggDialog(
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val crawlProgress = remember { Animatable(0f) }
    var rollTick by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        crawlProgress.snapTo(0f)
        crawlProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 26000, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starsTwinkle")
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBlack)
        ) {
            // Champ d'étoiles spatiales en arrière-plan
            StarfieldCanvas(
                twinkleFactor = starAlpha,
                modifier = Modifier.fillMaxSize()
            )

            // En-tête galactique "A long time ago in a galaxy far, far away...."
            Text(
                text = "A long time ago in a dice tavern far, far away....",
                style = MaterialTheme.typography.bodyMedium,
                color = IntroCyan.copy(alpha = (1f - crawlProgress.value * 2.2f).coerceIn(0.15f, 1f)),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp, start = 20.dp, end = 20.dp)
            )

            // Conteneur en perspective 3D Star Wars (incliné vers l'horizon et fuyant au loin)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val p = crawlProgress.value
                // Déplacement du bas vers l'horizon lointain + réduction progressive d'échelle
                val translateY = (1f - p * 1.65f) * 520f
                val distanceScale = (1.05f - p * 0.72f).coerceAtLeast(0.22f)
                val distanceAlpha = (1.15f - p * 0.85f).coerceIn(0.15f, 1f)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.15f)
                            rotationX = 56f
                            cameraDistance = 15f * density
                            translationY = translateY
                            scaleX = distanceScale
                            scaleY = distanceScale
                            alpha = distanceAlpha
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Grand "42" jaune composé de vrais dés Pixel Art du jeu !
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiceDigitMatrix(
                            grid = DIGIT_4_DICE_GRID,
                            rollTrigger = rollTick,
                            onTapDie = { rollTick++ }
                        )
                        DiceDigitMatrix(
                            grid = DIGIT_2_DICE_GRID,
                            rollTrigger = rollTick,
                            onTapDie = { rollTick++ }
                        )
                    }

                    Text(
                        text = "EPISODE XLII\nDEEP THOUGHT // NEON 5K",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        ),
                        color = StarWarsYellow,
                        textAlign = TextAlign.Center
                    )

                    // Citation obligatoire en anglais original quelle que soit la langue de l'application
                    Text(
                        text = "Answer to the Ultimate Question of Life, the Universe, and Everything",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 32.sp
                        ),
                        color = StarWarsYellow,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Text(
                        text = "For seven and a half million years, the supercomputer Deep Thought rolled five cyberpunk dice across the SHA-256 entropy continuum.\n\n" +
                            "Gyroscopes spun. Nanosecond clocks ticked. Four Aces and a Five gleamed in pure golden neon.\n\n" +
                            "Don't Panic, bring your towel, and never forget: forty-two taps always reveal the truth of the universe.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 26.sp
                        ),
                        color = StarWarsYellow.copy(alpha = 0.92f),
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Dégradé supérieur d'extinction à l'horizon spatial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepSpaceBlack, Color.Transparent)
                        )
                    )
            )

            // Barre de contrôle inférieure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DeepSpaceBlack.copy(alpha = 0.85f))
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PixelButton(
                    text = "REPLAY 42",
                    icon = PixelIconId.DICE,
                    filled = false,
                    accentColor = StarWarsYellow,
                    onClick = {
                        rollTick++
                        scope.launch {
                            crawlProgress.snapTo(0f)
                            crawlProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 26000, easing = LinearEasing)
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = "DON'T PANIC (EXIT)",
                    icon = PixelIconId.BACK,
                    filled = true,
                    accentColor = StarWarsYellow,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1.3f)
                )
            }
        }
    }
}

@Composable
private fun DiceDigitMatrix(
    grid: List<IntArray>,
    rollTrigger: Int,
    onTapDie: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        grid.forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEachIndexed { colIndex, face ->
                    if (face > 0) {
                        PixelDiceCanvas(
                            die = DieState(
                                index = rowIndex * 10 + colIndex,
                                face = face,
                                canScoreOnCurrentRoll = true
                            ),
                            rollTrigger = rollTrigger,
                            onClick = onTapDie,
                            dieSize = 20.dp,
                            styleOverride = DiceVisualStyle.PIXEL_SOLID,
                            alwaysClickable = true,
                            customTint = StarWarsYellow
                        )
                    } else {
                        Spacer(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StarfieldCanvas(
    twinkleFactor: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Coordonnées déterministes de 70 étoiles
        for (i in 0 until 70) {
            val sx = ((i * 73 + 19) % 100) / 100f * w
            val sy = ((i * 41 + 37) % 100) / 100f * h
            val starSize = if (i % 5 == 0) 3.dp.toPx() else 1.8.dp.toPx()
            val alpha = if (i % 3 == 0) twinkleFactor else (1.35f - twinkleFactor).coerceIn(0.3f, 0.95f)
            val color = when {
                i % 11 == 0 -> StarWarsYellow.copy(alpha = alpha)
                i % 7 == 0 -> IntroCyan.copy(alpha = alpha)
                else -> Color.White.copy(alpha = alpha)
            }
            drawRect(
                color = color,
                topLeft = Offset(sx, sy),
                size = Size(starSize, starSize)
            )
        }
    }
}

/**
 * Easter Egg #2 (13 taps sur le panneau "SYSTÈME D'ALÉATOIRE HYBRIDE ACTIF" de l'écran d'accueil) :
 * Terminal "OVERCLOCK 1337 // WHITE RABBIT ENTROPY" avec pluie matricielle de dés Phosphor.
 */
@Composable
fun Matrix1337EasterEggDialog(
    onDismiss: () -> Unit
) {
    val phosphorGreen = Color(0xFF39FF14)
    val matrixBg = Color(0xFF030A04)
    var rollTick by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CyberCutShape,
            color = matrixBg,
            border = BorderStroke(2.dp, phosphorGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SECRET #2 // OVERCLOCK 1337",
                    style = MaterialTheme.typography.titleMedium,
                    color = phosphorGreen
                )
                Text(
                    text = "Wake up, Runner...\nThe Matrix has you.\nFollow the White Dice.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = phosphorGreen,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 3, 3, 6, 1).forEachIndexed { idx, face ->
                        PixelDiceCanvas(
                            die = DieState(index = idx, face = face, canScoreOnCurrentRoll = true),
                            rollTrigger = rollTick,
                            onClick = { rollTick++ },
                            dieSize = 42.dp,
                            styleOverride = DiceVisualStyle.DOT_MATRIX,
                            alwaysClickable = true,
                            customTint = phosphorGreen
                        )
                    }
                }
                Text(
                    text = "SHA-256 : 1337C0DE42F00D... • ENTROPY OVERDRIVE ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = phosphorGreen.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                PixelButton(
                    text = "JACK OUT (1337)",
                    icon = PixelIconId.BOLT,
                    accentColor = phosphorGreen,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Easter Egg #3 (7 taps sur le badge de version en bas de InfoScreen) :
 * Clin d'oeil galactique à `OCRAurebesh` et au Sabacc ("NEVER TELL ME THE ODDS!").
 */
@Composable
fun HoloAurebeshEasterEggDialog(
    onDismiss: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    var rollTick by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = CyberCutShape,
            color = colors.background,
            border = BorderStroke(2.dp, StarWarsYellow)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SECRET #3 // HOLO-NET AUREBESH NODE",
                    style = MaterialTheme.typography.titleMedium,
                    color = StarWarsYellow,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "TRANSMISSION FROM MASTER CROCK",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(6, 6).forEachIndexed { idx, face ->
                        PixelDiceCanvas(
                            die = DieState(index = idx, face = face, canScoreOnCurrentRoll = true),
                            rollTrigger = rollTick,
                            onClick = { rollTick++ },
                            dieSize = 52.dp,
                            styleOverride = DiceVisualStyle.PIXEL_SOLID,
                            alwaysClickable = true,
                            customTint = StarWarsYellow
                        )
                    }
                }
                Text(
                    text = "\"Never tell me the odds!\"\n\nProbabilité de 5 As en un seul lancer : 1 sur 7 776 (0,01286 %).\nQue la Force (et l'entropie SHA-256) soient avec toi.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                PixelButton(
                    text = "FERMER L'HOLOCRON",
                    icon = PixelIconId.STAR,
                    accentColor = StarWarsYellow,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
