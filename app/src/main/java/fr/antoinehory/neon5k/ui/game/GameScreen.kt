package fr.antoinehory.neon5k.ui.game

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.EntropyBadgeDialog
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Plateau de jeu principal de Neon 5K (Lot 1).
 * - Gère le cycle de vie des capteurs via `LifecycleEventEffect(ON_RESUME / ON_PAUSE)`.
 * - Maintient l'écran allumé (`Modifier.keepScreenOn()`) pendant la partie.
 * - Capture les coordonnées `(x, y)` et la durée d'appui tactile sur le bouton LANCER pour enrichir la graine `SHA-256`.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameScreen(
    uiState: GameUiState,
    onEvent: (GameUiEvent) -> Unit,
    onResumed: () -> Unit,
    onPaused: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onResumed()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        onPaused()
    }

    val colors = LocalNeon5KColors.current
    val match = uiState.match
    val turn = match.turn
    val activePlayer = match.activePlayer

    if (uiState.showEntropyDialog && turn.lastEntropySnapshot != null) {
        EntropyBadgeDialog(
            snapshot = turn.lastEntropySnapshot,
            onDismiss = { onEvent(GameUiEvent.ToggleEntropyDialog) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .keepScreenOn()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Barre supérieure : Retour accueil, Preset/Cible et Badge d'Entropie SHA-256
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PixelButton(
                    text = "← MENU",
                    onClick = onNavigateHome,
                    filled = false,
                    accentColor = colors.textPrimary
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CIBLE ${match.rules.targetScore} PTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryNeon
                    )
                    Text(
                        text = "OUVERTURE ${match.rules.openingThreshold} • ${match.rules.diceCount} DÉS",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }

                val snapshot = turn.lastEntropySnapshot
                Box(
                    modifier = Modifier
                        .background(colors.surface, CyberCutShape)
                        .border(
                            width = 1.dp,
                            color = if (snapshot != null) colors.secondaryNeon else colors.textMuted.copy(alpha = 0.3f),
                            shape = CyberCutShape
                        )
                        .clickable(enabled = snapshot != null) {
                            onEvent(GameUiEvent.ToggleEntropyDialog)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = snapshot?.shortHashHex ?: "#SEED--",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (snapshot != null) colors.secondaryNeon else colors.textMuted
                    )
                }
            }

            // 2. Bandeau des scores joueurs (Pass & Play / Solo)
            ScoreboardRow(
                players = match.players,
                activePlayerIndex = turn.activePlayerIndex,
                winnerPlayerId = match.winnerPlayerId,
                openingThreshold = match.rules.openingThreshold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Zone centrale : Pot du tour + Tapis de dés Pixel Art 16x16
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = when (turn.phase) {
                TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> colors.alertNeon
                TurnPhase.HOT_DICE_MANDATORY_REROLL -> colors.secondaryNeon
                else -> colors.primaryNeon.copy(alpha = 0.55f)
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOUR #${turn.turnNumber} // ${activePlayer.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryNeon
                        )
                        Text(
                            text = if (activePlayer.isOpened) {
                                "STATUT : OUVERT (${activePlayer.score} / ${match.rules.targetScore})"
                            } else {
                                "STATUT : FERMÉ (Seuil ${match.rules.openingThreshold} requis)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "POT DU TOUR",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryNeon
                        )
                        Text(
                            text = "+${turn.totalTurnPointsWithSelection} PTS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (turn.isSelectionValid || turn.bankedInTurn > 0) {
                                colors.primaryNeon
                            } else {
                                colors.textMuted
                            }
                        )
                    }
                }

                // Tapis des dés (5 ou 6 dés)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    turn.dice.forEach { die ->
                        PixelDiceCanvas(
                            die = die,
                            rollTrigger = match.totalRollsInMatch,
                            onClick = { onEvent(GameUiEvent.ToggleDie(die.index)) },
                            dieSize = 66.dp
                        )
                    }
                }

                // Légende rapide des états de dés
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendDot(color = colors.primaryNeon, label = "Scorant")
                    LegendDot(color = colors.secondaryNeon, label = "Sélectionné")
                    LegendDot(color = colors.alertNeon, label = "Verrouillé")
                }

                // Message d'état du tour ou de combinaison sélectionnée
                Text(
                    text = turn.statusMessageFr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (turn.phase) {
                        TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> colors.alertNeon
                        else -> colors.textPrimary
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorBannerFr != null) {
                    Text(
                        text = "⚠ ${uiState.errorBannerFr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.alertNeon,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Zone de commandes (Lancer avec capture tactile d'entropie / Tout prendre / Relancer / Banquer / Repiquer)
        ActionControlsSection(
            match = match,
            onEvent = onEvent,
            onNavigateSetup = onNavigateSetup
        )
    }
}

@Composable
private fun ScoreboardRow(
    players: List<PlayerState>,
    activePlayerIndex: Int,
    winnerPlayerId: Int?,
    openingThreshold: Int
) {
    val colors = LocalNeon5KColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEachIndexed { index, player ->
            val isActive = index == activePlayerIndex && winnerPlayerId == null
            val isWinner = player.id == winnerPlayerId
            val borderColor = when {
                isWinner -> colors.alertNeon
                isActive -> colors.primaryNeon
                else -> colors.textMuted.copy(alpha = 0.25f)
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (isActive) colors.primaryNeon.copy(alpha = 0.12f) else colors.surface,
                        shape = CyberCutShape
                    )
                    .border(width = if (isActive || isWinner) 1.5.dp else 1.dp, color = borderColor, shape = CyberCutShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isWinner) "★ ${player.name}" else player.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) colors.primaryNeon else colors.textPrimary
                    )
                    Text(
                        text = "${player.score} PTS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isWinner) colors.alertNeon else colors.textPrimary
                    )
                    val tag = when {
                        player.isOpened || openingThreshold == 0 -> "OUVERT"
                        else -> "FERMÉ (<$openingThreshold)"
                    }
                    val bustTag = if (player.consecutiveBusts > 0) " • NULS:${player.consecutiveBusts}" else ""
                    Text(
                        text = "$tag$bustTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    val colors = LocalNeon5KColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .background(color)
                .padding(4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
    }
}

@Composable
private fun ActionControlsSection(
    match: MatchState,
    onEvent: (GameUiEvent) -> Unit,
    onNavigateSetup: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val turn = match.turn
    val activePlayer = match.activePlayer

    if (match.isFinished) {
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.alertNeon
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "VICTOIRE // ${match.winnerPlayer?.name ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.alertNeon
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "REVANCHE",
                        onClick = { onEvent(GameUiEvent.RestartSameMatch) },
                        accentColor = colors.primaryNeon,
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "CONFIGURER",
                        onClick = onNavigateSetup,
                        accentColor = colors.secondaryNeon,
                        filled = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (turn.phase) {
            TurnPhase.AWAITING_INITIAL_ROLL -> {
                // Offre de repiquage si disponible
                val offer = turn.piggybackOffer
                if (offer != null && activePlayer.isOpened) {
                    PixelButton(
                        text = "REPIQUER ${offer.inheritedPoints} PTS (${offer.remainingDiceCount} DÉS DE ${offer.fromPlayerName})",
                        onClick = { onEvent(GameUiEvent.AcceptPiggybackAndRoll()) },
                        accentColor = colors.alertNeon,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                EntropyTouchRollButton(
                    label = "LANCER LES ${match.rules.diceCount} DÉS (OU SECOUER)",
                    accentColor = colors.primaryNeon,
                    onRollWithTouchEntropy = { x, y, durationMs ->
                        onEvent(
                            GameUiEvent.RollDice(
                                touchX = x,
                                touchY = y,
                                pressDurationMs = durationMs
                            )
                        )
                    }
                )
            }

            TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                EntropyTouchRollButton(
                    label = "MAIN PLEINE ! RELANCER LES ${match.rules.diceCount} DÉS",
                    accentColor = colors.secondaryNeon,
                    onRollWithTouchEntropy = { x, y, durationMs ->
                        onEvent(
                            GameUiEvent.RollDice(
                                touchX = x,
                                touchY = y,
                                pressDurationMs = durationMs
                            )
                        )
                    }
                )
            }

            TurnPhase.SELECTING_DICE -> {
                val hasUnselectedScoring = turn.dice.any { !it.isLocked && it.canScoreOnCurrentRoll && !it.isSelected }
                if (hasUnselectedScoring) {
                    PixelButton(
                        text = "TOUT SÉLECTIONNER (DÉS SCORANTS)",
                        onClick = { onEvent(GameUiEvent.SelectAllScoring) },
                        accentColor = colors.primaryNeon,
                        filled = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val isHotDice = turn.selectionTriggersHotDice
                val rollLabel = when {
                    !turn.isSelectionValid -> "SÉLECTIONNEZ AU MOINS 1 DÉ SCORANT"
                    isHotDice -> "MAIN PLEINE ! RELANCER LES ${match.rules.diceCount} DÉS"
                    else -> "RELANCER (${turn.remainingDiceAfterSelection} DÉ(S) RESTANT(S))"
                }

                EntropyTouchRollButton(
                    label = rollLabel,
                    enabled = turn.isSelectionValid,
                    accentColor = if (isHotDice) colors.secondaryNeon else colors.primaryNeon,
                    onRollWithTouchEntropy = { x, y, durationMs ->
                        onEvent(
                            GameUiEvent.RollDice(
                                touchX = x,
                                touchY = y,
                                pressDurationMs = durationMs
                            )
                        )
                    }
                )

                val projectedTurnScore = turn.totalTurnPointsWithSelection
                val projectedTotal = activePlayer.score + projectedTurnScore
                val meetsOpening = activePlayer.isOpened || projectedTurnScore >= match.rules.openingThreshold
                val withinTarget = !match.rules.exactFinishRequired || projectedTotal <= match.rules.targetScore
                val allowedOnHotDice = !isHotDice || !match.rules.mandatoryRerollOnHotDice
                val canBank = turn.isSelectionValid && meetsOpening && withinTarget && allowedOnHotDice

                val bankLabel = when {
                    !turn.isSelectionValid -> "BANQUER (SÉLECTION REQUISE)"
                    isHotDice && match.rules.mandatoryRerollOnHotDice ->
                        "BANQUE BLOQUÉE (RELANCE MAIN PLEINE OBLIGATOIRE)"
                    !meetsOpening ->
                        "OUVERTURE À ${match.rules.openingThreshold} PTS (${projectedTurnScore}/${match.rules.openingThreshold})"
                    !withinTarget ->
                        "DÉPASSEMENT (${projectedTotal} > ${match.rules.targetScore})"
                    else ->
                        "BANQUER +$projectedTurnScore PTS (TOTAL : $projectedTotal)"
                }

                PixelButton(
                    text = bankLabel,
                    enabled = canBank,
                    onClick = { onEvent(GameUiEvent.BankTurn) },
                    accentColor = colors.secondaryNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                val bustButtonText = if (turn.phase == TurnPhase.OVERFLOW_BUSTED) {
                    "DÉPASSEMENT DE CIBLE — PASSER AU JOUEUR SUIVANT"
                } else {
                    "GLITCH (0 PT SUR CE LANCER) — JOUEUR SUIVANT"
                }
                PixelButton(
                    text = bustButtonText,
                    onClick = { onEvent(GameUiEvent.AcknowledgeBust) },
                    accentColor = colors.alertNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Bouton de lancer qui mesure les coordonnées exactes `(x, y)` et la durée d'appui en millisecondes
 * pour les injecter dans `SensorEntropyCollector`.
 */
@Composable
private fun EntropyTouchRollButton(
    label: String,
    accentColor: androidx.compose.ui.graphics.Color,
    onRollWithTouchEntropy: (touchX: Float, touchY: Float, pressDurationMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = LocalNeon5KColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (enabled) accentColor.copy(alpha = 0.20f) else colors.surface.copy(alpha = 0.4f),
                shape = CyberCutShape
            )
            .border(
                width = 1.5.dp,
                color = if (enabled) accentColor else colors.textMuted.copy(alpha = 0.3f),
                shape = CyberCutShape
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTimeMs = SystemClock.uptimeMillis()
                    val touchX = down.position.x
                    val touchY = down.position.y
                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        val duration = (SystemClock.uptimeMillis() - downTimeMs).coerceAtLeast(1L)
                        onRollWithTouchEntropy(touchX, touchY, duration)
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (enabled) accentColor else colors.textMuted.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}
