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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlin.math.abs
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
 *    défilent depuis tout en bas de l'écran en perspective 3D fixe à vitesse constante le long du plan incliné ;
 *    lorsque le texte jaune s'approche de la phrase bleue centrale, celle-ci s'efface en fondu doux (`fade out`).
 * 3. Toucher l'écran met le défilement en pause ; glisser vers le haut ou le bas fait défiler manuellement
 *    le texte vers le fond (avant) ou vers le bas (retour).
 */
@Composable
fun StarWars42EasterEggDialog(
    onDismiss: () -> Unit
) {
    val introAlpha = remember { Animatable(0f) }
    var crawlProgress by remember { mutableFloatStateOf(0f) }
    var crawlStarted by remember { mutableStateOf(false) }
    var isUserTouching by remember { mutableStateOf(false) }
    var sequenceKey by remember { mutableIntStateOf(0) }
    var rollTick by remember { mutableIntStateOf(1) }
    var isDismissArmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(EASTER_EGG_DISMISS_COOLDOWN_MS)
        isDismissArmed = true
    }

    LaunchedEffect(sequenceKey) {
        crawlProgress = 0f
        crawlStarted = false
        introAlpha.snapTo(0f)
        // Phase 1 : Apparition au centre de "A long time ago in a dice tavern far, far away...."
        introAlpha.animateTo(1f, animationSpec = tween(durationMillis = 650, easing = LinearEasing))

        // Pause de lecture de l'intro (~1100 ms), gelée tant que le joueur garde le doigt appuyé
        var introHoldElapsedMs = 0L
        while (introHoldElapsedMs < 1100L && !crawlStarted) {
            delay(25L)
            if (!isUserTouching) {
                introHoldElapsedMs += 25L
            }
        }
        crawlStarted = true

        // Phase 2 : Défilement temps réel à vitesse constante (pause instantanée au toucher, reprise au relâchement)
        var lastFrameNanos = withFrameNanos { it }
        val totalCrawlDurationSec = 34f
        while (true) {
            val frameNanos = withFrameNanos { it }
            val dtSec = ((frameNanos - lastFrameNanos).coerceIn(0L, 100_000_000L)) / 1_000_000_000f
            lastFrameNanos = frameNanos
            if (!isUserTouching && crawlProgress < 1f) {
                crawlProgress = (crawlProgress + dtSec / totalCrawlDurationSec).coerceAtMost(1f)
            }
        }
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
            val currentMaxHeightPx by rememberUpdatedState(maxHeightPx)

            // Zone spatiale interactive : toucher = pause immédiate ; glisser haut/bas = avancer/reculer le générique
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            isUserTouching = true
                            var totalDragY = 0f
                            var isDragging = false
                            try {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activeChanges = event.changes.filter { it.pressed }
                                    if (activeChanges.isEmpty()) break
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: activeChanges.first()
                                    val dy = change.positionChange().y
                                    if (dy != 0f) {
                                        totalDragY += dy
                                        if (!isDragging && abs(totalDragY) > touchSlop) {
                                            isDragging = true
                                            crawlStarted = true
                                        }
                                        if (isDragging) {
                                            change.consume()
                                            val travelPx = currentMaxHeightPx + crawlContentHeightPx.coerceAtLeast(1)
                                            // Glisser vers le haut (dy < 0) avance le texte vers le fond ; vers le bas (dy > 0) le ramène vers l'avant
                                            val deltaP = (-dy * 1.65f) / travelPx.coerceAtLeast(1f)
                                            crawlProgress = (crawlProgress + deltaP).coerceIn(0f, 1f)
                                        }
                                    }
                                }
                            } finally {
                                isUserTouching = false
                            }
                        }
                    }
            ) {
                // Champ d'étoiles pseudo-aléatoires sans alignement diagonal
                StarfieldCanvas(
                    twinkleFactor = starAlpha,
                    modifier = Modifier.fillMaxSize()
                )

                val p = crawlProgress
                // La phrase bleue reste visible tant que le texte jaune remonte du bas,
                // puis fade out doucement entre p = 0.10 et p = 0.26 avant d'atteindre le centre.
                val approachFade = (1f - ((p - 0.10f) / 0.16f).coerceIn(0f, 1f))
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

                // Plan 3D fixe incliné vers l'horizon : la perspective naturelle évite toute accélération
                // en fin de générique (les lignes proches du bas restent grandes et lisibles à vitesse constante).
                val tiltAngleX = if (isPortrait) 54f else 48f
                val camDistFactor = if (isPortrait) 12.5f else 14.5f
                val startY = maxHeightPx
                val endY = -crawlContentHeightPx.toFloat().coerceAtLeast(maxHeightPx * 0.5f)
                val translateY = startY + (endY - startY) * p
                val crawlVisibilityAlpha = if (!crawlStarted && p <= 0.0005f) {
                    0f
                } else if (p > 0.94f) {
                    (1f - (p - 0.94f) / 0.06f).coerceIn(0f, 1f)
                } else {
                    1f
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0.5f, 0.45f)
                            rotationX = tiltAngleX
                            cameraDistance = camDistFactor * density
                        }
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .padding(horizontal = if (isPortrait) 24.dp else 64.dp)
                            .onSizeChanged { crawlContentHeightPx = it.height }
                            .graphicsLayer {
                                translationY = translateY
                                alpha = crawlVisibilityAlpha
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

                // Dégradé supérieur d'extinction à l'horizon spatial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                0.0f to DeepSpaceBlack,
                                0.45f to DeepSpaceBlack.copy(alpha = 0.85f),
                                1.0f to Color.Transparent
                            )
                        )
                )
            }

            // Barre de contrôle inférieure (protégée par les insets safeDrawing en mode bord à bord)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(DeepSpaceBlack.copy(alpha = 0.88f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isUserTouching) {
                        "⏸ PAUSE • GLISSER ↕ POUR NAVIGUER"
                    } else {
                        "TOUCHER : PAUSE • GLISSER ↕ : DÉFILER"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = StarWarsYellow.copy(alpha = if (isUserTouching) 0.95f else 0.55f),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
