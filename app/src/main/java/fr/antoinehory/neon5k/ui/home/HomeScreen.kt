package fr.antoinehory.neon5k.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.R
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.Matrix1337EasterEggDialog
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.components.PixelIconId
import fr.antoinehory.neon5k.ui.components.StarWars42EasterEggDialog
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Écran d'accueil cyberpunk de Neon 5K (Lots 1 à 4+) :
 * - Boutons enrichis d'icônes Pixel Art `8×8` dédiées pour une lecture visuelle immédiate,
 * - Easter Egg #1 : 42 taps sur la zone des dés lance le générique spatial Star Wars "42" composé de dés du jeu,
 * - Easter Egg #2 : 13 taps sur le panneau d'entropie hybride ouvre le terminal secret "OVERCLOCK 1337".
 */
@Composable
fun HomeScreen(
    hasActiveMatch: Boolean,
    onResumeMatch: () -> Unit,
    onStartQuickMatch: () -> Unit,
    onOpenSetup: () -> Unit,
    onOpenCustomization: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current

    var diceZoneTapCount by remember { mutableIntStateOf(0) }
    var bannerRollTrigger by remember { mutableIntStateOf(0) }
    var showStarWars42EasterEgg by remember { mutableStateOf(false) }

    var entropyTapCount by remember { mutableIntStateOf(0) }
    var showMatrix1337EasterEgg by remember { mutableStateOf(false) }

    fun onDiceZoneTapped() {
        val next = diceZoneTapCount + 1
        bannerRollTrigger++
        if (next >= 42) {
            diceZoneTapCount = 0
            showStarWars42EasterEgg = true
        } else {
            diceZoneTapCount = next
        }
    }

    if (showStarWars42EasterEgg) {
        StarWars42EasterEggDialog(
            onDismiss = { showStarWars42EasterEgg = false }
        )
    }

    if (showMatrix1337EasterEgg) {
        Matrix1337EasterEggDialog(
            onDismiss = { showMatrix1337EasterEgg = false }
        )
    }

    val bannerDice = remember {
        listOf(
            DieState(index = 0, face = 1, canScoreOnCurrentRoll = true),
            DieState(index = 1, face = 5, isLocked = true, canScoreOnCurrentRoll = true),
            DieState(index = 2, face = 5, isSelected = true, canScoreOnCurrentRoll = true),
            DieState(index = 3, face = 5, isLocked = true, canScoreOnCurrentRoll = true),
            DieState(index = 4, face = 1, canScoreOnCurrentRoll = true)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PROTOCOL // FIVE_THOUSAND",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryNeon
            )
            Text(
                text = "NEON 5K",
                style = MaterialTheme.typography.displayLarge,
                color = colors.primaryNeon,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zone des dés d'accueil (Easter Egg 42 taps)
        CyberPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::onDiceZoneTapped
                ),
            borderColor = colors.primaryNeon.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bannerDice.forEach { die ->
                        PixelDiceCanvas(
                            die = die,
                            rollTrigger = bannerRollTrigger,
                            onClick = ::onDiceZoneTapped,
                            dieSize = 48.dp,
                            alwaysClickable = true
                        )
                    }
                }
                Text(
                    text = if (diceZoneTapCount >= 5) {
                        "As = 100 • Cinq = 50 • Brelan = N×100 • [DEEP THOUGHT : $diceZoneTapCount / 42]"
                    } else {
                        "As = 100 • Cinq = 50 • Brelan = N×100 • Suite = 1500"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (diceZoneTapCount >= 21) colors.alertNeon else colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasActiveMatch) {
                PixelButton(
                    text = stringResource(R.string.home_resume_match),
                    icon = PixelIconId.PLAY,
                    onClick = onResumeMatch,
                    accentColor = colors.alertNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PixelButton(
                text = stringResource(R.string.home_new_match),
                icon = PixelIconId.GAMEPAD,
                onClick = onOpenSetup,
                accentColor = colors.primaryNeon,
                modifier = Modifier.fillMaxWidth()
            )

            PixelButton(
                text = stringResource(R.string.home_quick_duel),
                icon = PixelIconId.BOLT,
                onClick = onStartQuickMatch,
                accentColor = colors.secondaryNeon,
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelButton(
                    text = stringResource(R.string.home_customization),
                    icon = PixelIconId.PALETTE,
                    onClick = onOpenCustomization,
                    accentColor = colors.primaryNeon,
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = stringResource(R.string.home_stats),
                    icon = PixelIconId.TROPHY,
                    onClick = onOpenStats,
                    accentColor = colors.secondaryNeon,
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixelButton(
                    text = stringResource(R.string.home_rules),
                    icon = PixelIconId.BOOK,
                    onClick = onOpenRules,
                    accentColor = colors.secondaryNeon,
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
                PixelButton(
                    text = stringResource(R.string.home_info),
                    icon = PixelIconId.INFO,
                    onClick = onOpenInfo,
                    accentColor = colors.alertNeon,
                    filled = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Panneau Entropie Hybride (Easter Egg 13 taps -> Overclock 1337)
        CyberPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    val next = entropyTapCount + 1
                    if (next >= 13) {
                        entropyTapCount = 0
                        showMatrix1337EasterEgg = true
                    } else {
                        entropyTapCount = next
                    }
                },
            borderColor = colors.secondaryNeon.copy(alpha = 0.25f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (entropyTapCount >= 4) {
                        "SYSTÈME D'ALÉATOIRE HYBRIDE ACTIF [OVERCLOCK : $entropyTapCount / 13]"
                    } else {
                        "SYSTÈME D'ALÉATOIRE HYBRIDE ACTIF"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryNeon
                )
                Text(
                    text = "Chaque lancer fusionne gyroscope, accéléromètre, horloge nanoseconde, vecteur tactile et sel cryptographique dans une graine SHA-256 vérifiable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
            }
        }
    }
}
