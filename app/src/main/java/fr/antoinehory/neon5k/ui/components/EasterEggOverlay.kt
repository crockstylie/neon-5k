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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlinx.coroutines.delay
import kotlin.random.Random

private val StarWarsYellow = Color(0xFFFFE81F)
private val DeepSpaceBlack = Color(0xFF020308)
private val IntroCyan = Color(0xFF4BD5EE)

// Délai de sécurité anti-fermeture accidentelle lors de clics rapides à l'ouverture d'un Easter Egg
private const val EASTER_EGG_DISMISS_COOLDOWN_MS = 900L

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

private data class StarPoint(
    val xNorm: Float,
    val yNorm: Float,
    val sizeDp: Float,
    val colorType: Int,
    val twinkleGroup: Int
)

private val RANDOM_STARFIELD: List<StarPoint> = buildList {
    val rng = Random(42_1337_5000L)
    repeat(96) {
        add(
            StarPoint(
                xNorm = rng.nextFloat(),
                yNorm = rng.nextFloat(),
                sizeDp = when (rng.nextInt(10)) {
                    0 -> 3.0f
                    1, 2 -> 2.2f
                    else -> 1.5f
                },
                colorType = rng.nextInt(14),
                twinkleGroup = rng.nextInt(3)
            )
        )
    }
}

/**
 * Easter Egg #1 (42 taps sur la zone des dés de l'écran d'accueil) :
 * 1. Séquence d'ouverture Star Wars fidèle : *"A long time ago in a dice tavern far, far away...."*
 *    s'affiche au centre de l'écran noir étoilé en cyan.
 * 2. Le grand **42** jaune composé de vrais dés Pixel Art (`PixelDiceCanvas`) et son générique
 *    défilent depuis tout en bas de l'écran en perspective 3D ; lorsque le texte jaune s'approche
 *    de la phrase bleue centrale, celle-ci s'efface en fondu doux (`fade out`).
 */
@Composable
fun StarWars42EasterEggDialog(
    onDismiss: () -> Unit
) {
    val introAlpha = remember { Animatable(0f) }
    val crawlProgress = remember { Animatable(0f) }
    var sequenceKey by remember { mutableIntStateOf(0) }
    var rollTick by remember { mutableIntStateOf(1) }
    var isDismissArmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(EASTER_EGG_DISMISS_COOLDOWN_MS)
        isDismissArmed = true
    }

    LaunchedEffect(sequenceKey) {
        crawlProgress.snapTo(0f)
        introAlpha.snapTo(0f)
        // Phase 1 : Apparition au centre de "A long time ago in a dice tavern far, far away...."
        introAlpha.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = LinearEasing))
        delay(1100L)
        // Phase 2 : Le crawl part de tout en bas ; la phrase bleue s'efface gentiment quand le jaune s'approche
        crawlProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 26000, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starsTwinkle")
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    Dialog(
        onDismissRequest = { if (isDismissArmed) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(DeepSpaceBlack)
        ) {
            val isPortrait = maxHeight > maxWidth
            val maxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            var crawlContentHeightPx by remember { mutableIntStateOf(0) }

            // Champ d'étoiles pseudo-aléatoires sans alignement diagonal
            StarfieldCanvas(
                twinkleFactor = starAlpha,
                modifier = Modifier.fillMaxSize()
            )

            val p = crawlProgress.value
            // La phrase bleue reste visible tant que le texte jaune remonte du bas,
            // puis fade out doucement entre p = 0.12 (y ≈ 85%) et p = 0.28 (y ≈ 64%) avant d'atteindre le centre.
            val approachFade = (1f - ((p - 0.12f) / 0.16f).coerceIn(0f, 1f))
            val effectiveIntroAlpha = introAlpha.value * approachFade

            if (effectiveIntroAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A long time ago in a dice tavern\nfar, far away....",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 32.sp
                        ),
                        color = IntroCyan.copy(alpha = effectiveIntroAlpha),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Phase 2 : Conteneur en perspective 3D Star Wars ancré tout en haut du bloc (TransformOrigin y=0)
            // afin que le haut du "42" démarre exactement sous le bord inférieur de l'écran (maxHeightPx).
            if (p > 0.0005f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isPortrait) 8.dp else 32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val startY = maxHeightPx
                    val endY = -maxOf(maxHeightPx * 0.28f, crawlContentHeightPx * 0.35f)
                    val translateY = startY + (endY - startY) * p
                    val distanceScale = (1.02f - p * 0.80f).coerceAtLeast(0.18f)
                    val distanceAlpha = if (p < 0.78f) {
                        1f
                    } else {
                        (1f - (p - 0.78f) / 0.22f).coerceIn(0f, 1f)
                    }
                    val tiltAngleX = if (isPortrait) 64f else 56f
                    val camDistFactor = if (isPortrait) 9.5f else 12.5f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { crawlContentHeightPx = it.height }
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0.0f)
                                rotationX = tiltAngleX
                                cameraDistance = camDistFactor * density
                                translationY = translateY
                                scaleX = distanceScale
                                scaleY = distanceScale
                                alpha = distanceAlpha
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Grand "42" jaune composé de vrais dés Pixel Art du jeu
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
            }

            // Dégradé supérieur d'extinction à l'horizon spatial
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DeepSpaceBlack, Color.Transparent)
                        )
                    )
            )

            // Barre de contrôle inférieure (protégée par les insets safeDrawing en mode bord à bord)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DeepSpaceBlack.copy(alpha = 0.85f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
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
                        sequenceKey++
                    },
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = "DON'T PANIC (EXIT)",
                    icon = PixelIconId.BACK,
                    filled = true,
                    accentColor = StarWarsYellow,
                    onClick = { if (isDismissArmed) onDismiss() },
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
        for (star in RANDOM_STARFIELD) {
            val sx = star.xNorm * w
            val sy = star.yNorm * h
            val starPx = star.sizeDp.dp.toPx()
            val alpha = when (star.twinkleGroup) {
                0 -> twinkleFactor
                1 -> (1.38f - twinkleFactor).coerceIn(0.28f, 0.95f)
                else -> 0.72f
            }
            val color = when (star.colorType) {
                0 -> StarWarsYellow.copy(alpha = alpha)
                1 -> IntroCyan.copy(alpha = alpha)
                else -> Color.White.copy(alpha = alpha)
            }
            drawRect(
                color = color,
                topLeft = Offset(sx, sy),
                size = Size(starPx, starPx)
            )
        }
    }
}

/**
 * Easter Egg #2 (13 taps sur le panneau "SYSTÈME D'ALÉATOIRE HYBRIDE ACTIF" de l'écran d'accueil) :
 * Terminal "OVERCLOCK 1337 // WHITE RABBIT ENTROPY" avec pluie matricielle de dés Phosphor.
 * Protégé par un délai anti-fermeture accidentelle ([EASTER_EGG_DISMISS_COOLDOWN_MS]).
 */
@Composable
fun Matrix1337EasterEggDialog(
    onDismiss: () -> Unit
) {
    val phosphorGreen = Color(0xFF39FF14)
    val matrixBg = Color(0xFF030A04)
    var rollTick by remember { mutableIntStateOf(1) }
    var isDismissArmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(EASTER_EGG_DISMISS_COOLDOWN_MS)
        isDismissArmed = true
    }

    Dialog(onDismissRequest = { if (isDismissArmed) onDismiss() }) {
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
                    onClick = { if (isDismissArmed) onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Easter Egg #3 (7 taps sur le badge de version en bas de InfoScreen) :
 * Clin d'oeil galactique à `OCRAurebesh` et au Sabacc ("NEVER TELL ME THE ODDS!").
 * Protégé par un délai anti-fermeture accidentelle ([EASTER_EGG_DISMISS_COOLDOWN_MS]).
 */
@Composable
fun HoloAurebeshEasterEggDialog(
    onDismiss: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    var rollTick by remember { mutableIntStateOf(1) }
    var isDismissArmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(EASTER_EGG_DISMISS_COOLDOWN_MS)
        isDismissArmed = true
    }

    Dialog(onDismissRequest = { if (isDismissArmed) onDismiss() }) {
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
                    onClick = { if (isDismissArmed) onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
