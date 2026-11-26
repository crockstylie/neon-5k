package fr.antoinehory.neon5k.ui.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Écran d'accueil minimaliste cyberpunk de Neon 5K (Lot 1).
 */
@Composable
fun HomeScreen(
    hasActiveMatch: Boolean,
    onResumeMatch: () -> Unit,
    onStartQuickMatch: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val bannerDice = remember {
        listOf(
            DieState(index = 0, face = 1, canScoreOnCurrentRoll = true),
            DieState(index = 1, face = 5, isSelected = true, canScoreOnCurrentRoll = true),
            DieState(index = 2, face = 5, isSelected = true, canScoreOnCurrentRoll = true),
            DieState(index = 3, face = 5, isSelected = true, canScoreOnCurrentRoll = true),
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
                text = "CYBERPUNK PIXEL DICE ENGINE • ENTROPIE SHA-256",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
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
                            rollTrigger = 0,
                            onClick = {},
                            dieSize = 48.dp
                        )
                    }
                }
                Text(
                    text = "As = 100 • Cinq = 50 • Brelan = N×100 • Suite = 1500",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (hasActiveMatch) {
                PixelButton(
                    text = "REPRENDRE LA PARTIE EN COURS",
                    onClick = onResumeMatch,
                    accentColor = colors.alertNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PixelButton(
                text = "NOUVELLE PARTIE & VARIANTES",
                onClick = onOpenSetup,
                accentColor = colors.primaryNeon,
                modifier = Modifier.fillMaxWidth()
            )

            PixelButton(
                text = "PARTIE RAPIDE (CLASSIQUE 5 DÉS)",
                onClick = onStartQuickMatch,
                accentColor = colors.secondaryNeon,
                filled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.secondaryNeon.copy(alpha = 0.25f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "SYSTÈME D'ALÉATOIRE HYBRIDE ACTIF",
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
