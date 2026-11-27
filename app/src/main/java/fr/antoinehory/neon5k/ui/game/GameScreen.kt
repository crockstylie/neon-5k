package fr.antoinehory.neon5k.ui.game

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import fr.antoinehory.neon5k.data.preferences.UserPreferences
import fr.antoinehory.neon5k.domain.ai.BotProfile
import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.RollHistoryEntry
import fr.antoinehory.neon5k.domain.model.TurnPhase
import fr.antoinehory.neon5k.domain.rules.FiveThousandEngine
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import fr.antoinehory.neon5k.ui.components.AchievementToastBanner
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.EntropyBadgeDialog
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.components.PixelIconCanvas
import fr.antoinehory.neon5k.ui.components.PixelIconId
import fr.antoinehory.neon5k.ui.rules.QuickRulesPaytableDialog
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Plateau de jeu principal de Neon 5K.
 *
 * Améliorations UX / UI :
 * - Flèche retour Pixel Art (`PixelIconId.BACK`) harmonisée sur le bouton `MENU`.
 * - Carrousel de joueurs (`LazyRow`) avec centrage automatique animé sur le joueur actif (> 2 joueurs).
 * - Séparation nette et monocouche du titre du tour et du badge dynamique `+XXX PTS`.
 * - Panneau central d'historique des lancers et de la pile des Paliers & Encoches (`✕`) + modale complète.
 * - Hauteur du dock de boutons d'action strictement verrouillée (le bouton `TOUT PRENDRE` est intégré
 *   à l'en-tête du bloc de dés) afin que la ligne de dés ne saute jamais verticalement.
 * - Mode Paysage (`maxWidth > maxHeight`) en 2 colonnes côte à côte sans aucun défilement vertical requis.
 */
@Composable
fun GameScreen(
    uiState: GameUiState,
    preferences: UserPreferences,
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

    val view = LocalView.current
    val match = uiState.match
    val turn = match.turn
    val activePlayer = match.activePlayer

    fun triggerHaptic() {
        if (preferences.hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    if (uiState.showEntropyDialog && turn.lastEntropySnapshot != null) {
        EntropyBadgeDialog(
            snapshot = turn.lastEntropySnapshot,
            onDismiss = { onEvent(GameUiEvent.ToggleEntropyDialog) }
        )
    }

    if (uiState.showRulesDialog) {
        QuickRulesPaytableDialog(
            rules = match.rules,
            onDismiss = { onEvent(GameUiEvent.ToggleRulesDialog) }
        )
    }

    if (uiState.showHistoryDialog) {
        MatchHistoryAndCheckpointsDialog(
            match = match,
            onDismiss = { onEvent(GameUiEvent.ToggleHistoryDialog) }
        )
    }

    val screenModifier = if (preferences.keepScreenOn) {
        modifier.fillMaxSize().keepScreenOn()
    } else {
        modifier.fillMaxSize()
    }

    BoxWithConstraints(
        modifier = screenModifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        val isLandscape = maxWidth > maxHeight

        if (isLandscape) {
            // Mode Paysage 2 colonnes sans scroll vertical obligatoire
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val infoColumn: @Composable (Modifier) -> Unit = { colMod ->
                    Column(
                        modifier = colMod.fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.latestAchievementToast != null) {
                            AchievementToastBanner(
                                notification = uiState.latestAchievementToast,
                                onDismiss = { onEvent(GameUiEvent.DismissAchievementToast) }
                            )
                        }
                        GameTopBar(
                            match = match,
                            isBotPaused = uiState.isBotPaused,
                            onNavigateHome = {
                                triggerHaptic()
                                onNavigateHome()
                            },
                            onOpenRules = {
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleRulesDialog)
                            },
                            onOpenHistory = {
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleHistoryDialog)
                            },
                            onOpenEntropy = {
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleEntropyDialog)
                            },
                            onToggleBotPause = {
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleBotPause)
                            }
                        )
                        AutoCenteredPlayerCarousel(
                            players = match.players,
                            activePlayerIndex = turn.activePlayerIndex,
                            targetScore = match.rules.targetScore,
                            threeStrikesMode = match.rules.threeStrikesMode,
                            compact = true
                        )
                        TurnStatusPanel(
                            match = match,
                            activePlayer = activePlayer,
                            isBotThinking = uiState.isBotThinking,
                            isBotPaused = uiState.isBotPaused,
                            errorBannerFr = uiState.errorBannerFr,
                            compact = true
                        )
                        TurnAndCheckpointsHistoryPanel(
                            match = match,
                            activePlayer = activePlayer,
                            maxEntriesVisible = 2,
                            onOpenFullHistory = {
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleHistoryDialog)
                            },
                            modifier = Modifier.weight(1f, fill = true)
                        )
                    }
                }

                val interactiveColumn: @Composable (Modifier) -> Unit = { colMod ->
                    Column(
                        modifier = colMod.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        DiceBoardPanel(
                            match = match,
                            activePlayer = activePlayer,
                            rollTriggerCounter = uiState.rollTriggerCounter,
                            isBotThinking = uiState.isBotThinking,
                            compact = true,
                            onToggleDie = { idx ->
                                triggerHaptic()
                                onEvent(GameUiEvent.ToggleDie(idx))
                            },
                            onSelectAll = {
                                triggerHaptic()
                                onEvent(GameUiEvent.SelectAllScoring)
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ActionControlsSection(
                            match = match,
                            activePlayer = activePlayer,
                            isBotThinking = uiState.isBotThinking,
                            isBotPaused = uiState.isBotPaused,
                            leftHandedMode = preferences.leftHandedMode,
                            compactLandscape = true,
                            onTriggerHaptic = ::triggerHaptic,
                            onEvent = onEvent,
                            onNavigateSetup = onNavigateSetup
                        )
                    }
                }

                if (preferences.leftHandedMode) {
                    interactiveColumn(Modifier.weight(0.95f))
                    infoColumn(Modifier.weight(1.05f))
                } else {
                    infoColumn(Modifier.weight(1.05f))
                    interactiveColumn(Modifier.weight(0.95f))
                }
            }
        } else {
            // Mode Portrait ergonomique à une main : Zone haute -> Historique au milieu -> Dés et Dock fixe en bas
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Zone Haute : TopBar + Carrousel auto-centré + Statut & Pot du tour
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.latestAchievementToast != null) {
                        AchievementToastBanner(
                            notification = uiState.latestAchievementToast,
                            onDismiss = { onEvent(GameUiEvent.DismissAchievementToast) }
                        )
                    }
                    GameTopBar(
                        match = match,
                        isBotPaused = uiState.isBotPaused,
                        onNavigateHome = {
                            triggerHaptic()
                            onNavigateHome()
                        },
                        onOpenRules = {
                            triggerHaptic()
                            onEvent(GameUiEvent.ToggleRulesDialog)
                        },
                        onOpenHistory = {
                            triggerHaptic()
                            onEvent(GameUiEvent.ToggleHistoryDialog)
                        },
                        onOpenEntropy = {
                            triggerHaptic()
                            onEvent(GameUiEvent.ToggleEntropyDialog)
                        },
                        onToggleBotPause = {
                            triggerHaptic()
                            onEvent(GameUiEvent.ToggleBotPause)
                        }
                    )
                    AutoCenteredPlayerCarousel(
                        players = match.players,
                        activePlayerIndex = turn.activePlayerIndex,
                        targetScore = match.rules.targetScore,
                        threeStrikesMode = match.rules.threeStrikesMode,
                        compact = false
                    )
                    TurnStatusPanel(
                        match = match,
                        activePlayer = activePlayer,
                        isBotThinking = uiState.isBotThinking,
                        isBotPaused = uiState.isBotPaused,
                        errorBannerFr = uiState.errorBannerFr,
                        compact = false
                    )
                }

                // 2. Zone Centrale : Historique en direct des lancers & Paliers d'encoches (✕)
                TurnAndCheckpointsHistoryPanel(
                    match = match,
                    activePlayer = activePlayer,
                    maxEntriesVisible = 3,
                    onOpenFullHistory = {
                        triggerHaptic()
                        onEvent(GameUiEvent.ToggleHistoryDialog)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )

                // 3. Zone Basse (verrouillée en hauteur pour que les dés ne bougent jamais)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DiceBoardPanel(
                        match = match,
                        activePlayer = activePlayer,
                        rollTriggerCounter = uiState.rollTriggerCounter,
                        isBotThinking = uiState.isBotThinking,
                        compact = false,
                        onToggleDie = { idx ->
                            triggerHaptic()
                            onEvent(GameUiEvent.ToggleDie(idx))
                        },
                        onSelectAll = {
                            triggerHaptic()
                            onEvent(GameUiEvent.SelectAllScoring)
                        }
                    )

                    ActionControlsSection(
                        match = match,
                        activePlayer = activePlayer,
                        isBotThinking = uiState.isBotThinking,
                        isBotPaused = uiState.isBotPaused,
                        leftHandedMode = preferences.leftHandedMode,
                        compactLandscape = false,
                        onTriggerHaptic = ::triggerHaptic,
                        onEvent = onEvent,
                        onNavigateSetup = onNavigateSetup
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTopBar(
    match: MatchState,
    isBotPaused: Boolean,
    onNavigateHome: () -> Unit,
    onOpenRules: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenEntropy: () -> Unit,
    onToggleBotPause: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val snapshot = match.turn.lastEntropySnapshot
    val hasBotsInMatch = match.players.any { it.type != fr.antoinehory.neon5k.domain.model.PlayerType.HUMAN }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelButton(
            text = "MENU",
            icon = PixelIconId.BACK,
            onClick = onNavigateHome,
            filled = false,
            accentColor = colors.textPrimary
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = "CIBLE ${match.rules.targetScore} PTS",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon,
                maxLines = 1
            )
            Text(
                text = "OUV. ${match.rules.openingThreshold} • ${match.rules.diceCount} DÉS",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                maxLines = 1
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton Pause rapide pour les bots
            if (hasBotsInMatch) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isBotPaused) colors.alertNeon.copy(alpha = 0.2f) else colors.surface,
                            CyberCutShape
                        )
                        .border(
                            1.dp,
                            if (isBotPaused) colors.alertNeon else colors.secondaryNeon.copy(alpha = 0.7f),
                            CyberCutShape
                        )
                        .clickable(onClick = onToggleBotPause)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isBotPaused) "▶" else "⏸",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isBotPaused) colors.alertNeon else colors.secondaryNeon
                    )
                }
            }

            // Bouton Journal / Historique des lancers & paliers
            Box(
                modifier = Modifier
                    .background(colors.surface, CyberCutShape)
                    .border(1.dp, colors.secondaryNeon.copy(alpha = 0.7f), CyberCutShape)
                    .clickable(onClick = onOpenHistory)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "LOG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryNeon
                )
            }

            // Bouton Aide Règles
            Box(
                modifier = Modifier
                    .background(colors.surface, CyberCutShape)
                    .border(1.dp, colors.primaryNeon.copy(alpha = 0.7f), CyberCutShape)
                    .clickable(onClick = onOpenRules)
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryNeon
                )
            }

            if (snapshot != null) {
                Box(
                    modifier = Modifier
                        .background(colors.surface, CyberCutShape)
                        .border(1.dp, colors.secondaryNeon.copy(alpha = 0.6f), CyberCutShape)
                        .clickable(onClick = onOpenEntropy)
                        .padding(horizontal = 7.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = snapshot.shortHashHex,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryNeon,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Carrousel de joueurs (`LazyRow`) qui centre automatiquement la carte du joueur actif
 * dès que `activePlayerIndex` change, avec mise en valeur néon explicite en mode > 2 joueurs.
 */
@Composable
private fun AutoCenteredPlayerCarousel(
    players: List<PlayerState>,
    activePlayerIndex: Int,
    targetScore: Int,
    threeStrikesMode: ThreeStrikesMode,
    compact: Boolean
) {
    val colors = LocalNeon5KColors.current
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val viewportWidthDp = maxWidth
        val isMultiCarousel = players.size > 2
        val cardWidthDp: Dp = if (isMultiCarousel) {
            (viewportWidthDp * 0.46f).coerceIn(148.dp, 195.dp)
        } else {
            ((viewportWidthDp - 8.dp) / players.size.coerceAtLeast(1))
        }

        LaunchedEffect(activePlayerIndex, players.size, viewportWidthDp) {
            if (isMultiCarousel && activePlayerIndex in players.indices) {
                val viewportPx = with(density) { viewportWidthDp.roundToPx() }
                val cardPx = with(density) { cardWidthDp.roundToPx() }
                val centerOffset = -((viewportPx - cardPx) / 2).coerceAtLeast(0)
                listState.animateScrollToItem(
                    index = activePlayerIndex,
                    scrollOffset = centerOffset
                )
            }
        }

        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = if (isMultiCarousel) 4.dp else 0.dp)
        ) {
            itemsIndexed(players, key = { _, p -> p.id }) { idx, player ->
                val isActive = idx == activePlayerIndex
                val botProfile = BotProfile.fromPlayerType(player.type)
                val borderColor = when {
                    isActive -> colors.primaryNeon
                    else -> colors.textMuted.copy(alpha = 0.28f)
                }
                val bgColor = if (isActive) {
                    colors.surface
                } else {
                    colors.surface.copy(alpha = 0.48f)
                }

                Column(
                    modifier = Modifier
                        .width(cardWidthDp)
                        .background(bgColor, CyberCutShape)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = borderColor,
                            shape = CyberCutShape
                        )
                        .padding(horizontal = 10.dp, vertical = if (compact) 6.dp else 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isActive) "▶ ${player.name.uppercase()}" else player.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isActive) colors.primaryNeon else colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isActive) {
                            Text(
                                text = if (botProfile != null) "IA" else "TOUR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.secondaryNeon,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        } else if (botProfile != null) {
                            Text(
                                text = "BOT",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "${player.score}",
                            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isActive) colors.textPrimary else colors.textPrimary.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                        Text(
                            text = "/ $targetScore",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }

                    val statusLine = buildString {
                        if (player.isOpened) append("Ouvert") else append("Non ouvert")
                        if (threeStrikesMode != ThreeStrikesMode.DISABLED) {
                            val strikesVisual = buildString {
                                repeat(player.consecutiveBusts.coerceIn(0, 3)) { append("✕") }
                                repeat((3 - player.consecutiveBusts).coerceIn(0, 3)) { append("·") }
                            }
                            append(" • [").append(strikesVisual).append("]")
                        } else if (player.consecutiveBusts > 0) {
                            append(" • Nuls: ").append(player.consecutiveBusts)
                        }
                        if (player.previousScore > 0 &&
                            (threeStrikesMode == ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES ||
                                threeStrikesMode == ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES)
                        ) {
                            val prevStrikes = player.checkpoints.dropLast(1).lastOrNull()?.strikes ?: 0
                            append(" (↙${player.previousScore}")
                            if (threeStrikesMode == ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES && prevStrikes > 0) {
                                append(":${prevStrikes}✕")
                            }
                            append(")")
                        }
                    }
                    Text(
                        text = statusLine,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (player.consecutiveBusts >= 2) colors.alertNeon else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Panneau de statut du tour avec séparation nette entre l'intitulé du tour à gauche
 * et le badge de score dynamique `+XXX PTS` à droite (garanti sur une seule ligne, sans tassement).
 */
@Composable
private fun TurnStatusPanel(
    match: MatchState,
    activePlayer: PlayerState,
    isBotThinking: Boolean,
    isBotPaused: Boolean,
    errorBannerFr: String?,
    compact: Boolean
) {
    val colors = LocalNeon5KColors.current
    val turn = match.turn

    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when (turn.phase) {
            TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> colors.alertNeon
            TurnPhase.HOT_DICE_MANDATORY_REROLL -> colors.secondaryNeon
            else -> colors.primaryNeon
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TOUR #${turn.turnNumber} // ${activePlayer.name.uppercase()}",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryNeon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Verrouillé : ${turn.bankedInTurn} pts  •  Sélection : +${turn.selectedPoints} pts",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Badge dynamique du pot sur 1 seule ligne garantie
                Box(
                    modifier = Modifier
                        .background(colors.background.copy(alpha = 0.8f), CyberCutShape)
                        .border(
                            width = 1.dp,
                            color = if (turn.totalTurnPointsWithSelection > 0) colors.secondaryNeon else colors.primaryNeon.copy(alpha = 0.4f),
                            shape = CyberCutShape
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+${turn.totalTurnPointsWithSelection} PTS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (turn.totalTurnPointsWithSelection > 0) colors.secondaryNeon else colors.textMuted,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Text(
                text = if (isBotThinking) {
                    "⚡ ${activePlayer.name} calcule sa trajectoire optimale..."
                } else if (isBotPaused && activePlayer.type != fr.antoinehory.neon5k.domain.model.PlayerType.HUMAN) {
                    "⏸ Pause active sur le tour de ${activePlayer.name} — utilisez [Pas à pas] ou [Reprendre]"
                } else {
                    turn.statusMessageFr
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (turn.phase) {
                    TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> colors.alertNeon
                    else -> colors.textPrimary
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (errorBannerFr != null) {
                Text(
                    text = "⚠ $errorBannerFr",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.alertNeon,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Panneau central affichant :
 * 1. L'échelle des **Paliers & Encoches (`✕`)** du joueur actif (indispensable pour évaluer le risque de chute en cascade !).
 * 2. Les derniers lancers et actions de la partie en direct + bouton d'ouverture de la modale complète.
 */
@Composable
private fun TurnAndCheckpointsHistoryPanel(
    match: MatchState,
    activePlayer: PlayerState,
    maxEntriesVisible: Int,
    onOpenFullHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val checkpoints = activePlayer.normalizedCheckpoints()
    val recentLogs = match.rollHistoryLog.takeLast(maxEntriesVisible).asReversed()

    CyberPanel(
        modifier = modifier.fillMaxWidth(),
        borderColor = colors.primaryNeon.copy(alpha = 0.45f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PALIERS (✕) & HISTORIQUE DES LANCERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryNeon,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(colors.background.copy(alpha = 0.7f), CyberCutShape)
                        .border(1.dp, colors.secondaryNeon.copy(alpha = 0.6f), CyberCutShape)
                        .clickable(onClick = onOpenFullHistory)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TOUT VOIR (${match.rollHistoryLog.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryNeon
                    )
                }
            }

            // Barre horizontale des Paliers & Encoches (✕) du joueur actif
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${activePlayer.name} :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryNeon
                )
                if (checkpoints.isEmpty()) {
                    val zeroStrikes = buildStrikesString(activePlayer.consecutiveBusts)
                    CheckpointChip(
                        label = "0 pt [$zeroStrikes]",
                        isCurrent = true,
                        hasDangerStrikes = activePlayer.consecutiveBusts >= 2
                    )
                } else {
                    checkpoints.forEachIndexed { idx, cp ->
                        val isTop = idx == checkpoints.lastIndex
                        val strikesStr = buildStrikesString(if (isTop) activePlayer.consecutiveBusts else cp.strikes)
                        val danger = (if (isTop) activePlayer.consecutiveBusts else cp.strikes) >= 2
                        CheckpointChip(
                            label = "${if (isTop) "▶ " else ""}${cp.score} [$strikesStr]",
                            isCurrent = isTop,
                            hasDangerStrikes = danger
                        )
                    }
                }
            }

            // Derniers lancers de la partie
            if (recentLogs.isEmpty()) {
                Text(
                    text = "Aucun lancer effectué dans cette partie. Lancez les dés pour démarrer le journal !",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    recentLogs.forEach { entry ->
                        CompactRollLogRow(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckpointChip(
    label: String,
    isCurrent: Boolean,
    hasDangerStrikes: Boolean
) {
    val colors = LocalNeon5KColors.current
    val borderColor = when {
        hasDangerStrikes -> colors.alertNeon
        isCurrent -> colors.primaryNeon
        else -> colors.textMuted.copy(alpha = 0.45f)
    }
    Box(
        modifier = Modifier
            .background(
                color = if (isCurrent) colors.primaryNeon.copy(alpha = 0.14f) else colors.background.copy(alpha = 0.6f),
                shape = CyberCutShape
            )
            .border(1.dp, borderColor, CyberCutShape)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = when {
                hasDangerStrikes -> colors.alertNeon
                isCurrent -> colors.textPrimary
                else -> colors.textMuted
            },
            maxLines = 1
        )
    }
}

@Composable
private fun CompactRollLogRow(entry: RollHistoryEntry) {
    val colors = LocalNeon5KColors.current
    val diceText = if (entry.rolledFaces.isEmpty()) "" else "[${entry.rolledFaces.joinToString(",")}] "
    val accent = when {
        entry.isBust -> colors.alertNeon
        entry.isBank -> colors.secondaryNeon
        entry.isHotDice -> colors.primaryNeon
        else -> colors.textPrimary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.55f), CyberCutShape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "T${entry.turnNumber}.L${entry.rollNumber} ${entry.playerName}: $diceText${entry.summaryFr}",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "[${buildStrikesString(entry.playerStrikesAfter)}]",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (entry.playerStrikesAfter >= 2) colors.alertNeon else colors.textMuted,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

private fun buildStrikesString(strikes: Int): String = buildString {
    val s = strikes.coerceIn(0, 3)
    repeat(s) { append("✕") }
    repeat(3 - s) { append("·") }
}

/**
 * Tapis des dés Pixel Art avec bouton « TOUT PRENDRE » intégré dans la barre d'en-tête fixe
 * (pour éviter que l'apparition/disparition d'un 3e bouton en bas ne fasse sauter la ligne de dés).
 */
@Composable
private fun DiceBoardPanel(
    match: MatchState,
    activePlayer: PlayerState,
    rollTriggerCounter: Int,
    isBotThinking: Boolean,
    compact: Boolean,
    onToggleDie: (Int) -> Unit,
    onSelectAll: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val turn = match.turn
    val isHumanTurn = activePlayer.type == fr.antoinehory.neon5k.domain.model.PlayerType.HUMAN && !isBotThinking
    val canSelectAll = isHumanTurn &&
        turn.phase == TurnPhase.SELECTING_DICE &&
        turn.dice.any { !it.isLocked && it.canScoreOnCurrentRoll && !it.isSelected }

    CyberPanel(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DÉS DU TOUR (${turn.dice.count { !it.isLocked }} LIBRES / ${match.rules.diceCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1
                )

                // Raccourci TOUT PRENDRE intégré à l'en-tête fixe (ne décale jamais la hauteur du bloc de dés)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (canSelectAll) colors.secondaryNeon.copy(alpha = 0.2f) else Color.Transparent,
                            shape = CyberCutShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (canSelectAll) colors.secondaryNeon else colors.textMuted.copy(alpha = 0.25f),
                            shape = CyberCutShape
                        )
                        .clickable(enabled = canSelectAll, onClick = onSelectAll)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (turn.selectionTriggersHotDice) "★ MAIN PLEINE" else "TOUT PRENDRE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (canSelectAll) colors.secondaryNeon else colors.textMuted.copy(alpha = 0.45f)
                    )
                }
            }

            val isSixDice = turn.dice.size == 6
            val diceRows = if (isSixDice) {
                turn.dice.chunked(3)
            } else {
                listOf(turn.dice)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (compact) 2.dp else 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
            ) {
                diceRows.forEach { rowDice ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(if (isSixDice) 16.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowDice.forEach { die ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                PixelDiceCanvas(
                                    die = die,
                                    rollTrigger = rollTriggerCounter,
                                    dieSize = when {
                                        compact && isSixDice -> 52.dp
                                        compact -> 50.dp
                                        isSixDice -> 62.dp
                                        else -> 58.dp
                                    },
                                    onClick = { onToggleDie(die.index) }
                                )
                                Text(
                                    text = when {
                                        die.isLocked -> "LOCK"
                                        die.isSelected -> "GARDE"
                                        die.canScoreOnCurrentRoll -> "SCORE"
                                        die.face == 0 -> "PRÊT"
                                        else -> "—"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        die.isLocked -> colors.alertNeon
                                        die.isSelected -> colors.secondaryNeon
                                        die.canScoreOnCurrentRoll -> colors.primaryNeon
                                        else -> colors.textMuted
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zone de contrôles tactiles à hauteur constante (`112.dp` en portrait, `62.dp` en paysage)
 * afin que la ligne de dés au-dessus ne subisse jamais de saut vertical en cours de tour.
 */
@Composable
private fun ActionControlsSection(
    match: MatchState,
    activePlayer: PlayerState,
    isBotThinking: Boolean,
    isBotPaused: Boolean,
    leftHandedMode: Boolean,
    compactLandscape: Boolean,
    onTriggerHaptic: () -> Unit,
    onEvent: (GameUiEvent) -> Unit,
    onNavigateSetup: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val turn = match.turn
    val rules = match.rules
    val fixedHeight = if (compactLandscape) 96.dp else 112.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(fixedHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (match.isFinished) {
            val winner = match.winnerPlayer
            CyberPanel(
                modifier = Modifier.fillMaxSize(),
                borderColor = colors.secondaryNeon
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏆 VICTOIRE DE ${winner?.name?.uppercase() ?: "JOUEUR"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.secondaryNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Score : ${winner?.score ?: 0} pts • ${match.totalRollsInMatch} lancers",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelButton(
                            text = "REVANCHE",
                            onClick = {
                                onTriggerHaptic()
                                onEvent(GameUiEvent.RestartMatch)
                            },
                            accentColor = colors.primaryNeon
                        )
                        PixelButton(
                            text = "CONFIG",
                            filled = false,
                            onClick = {
                                onTriggerHaptic()
                                onNavigateSetup()
                            },
                            accentColor = colors.secondaryNeon
                        )
                    }
                }
            }
            return@Box
        }

        if (activePlayer.type != fr.antoinehory.neon5k.domain.model.PlayerType.HUMAN) {
            CyberPanel(
                modifier = Modifier.fillMaxSize(),
                borderColor = if (isBotPaused) colors.alertNeon else colors.secondaryNeon
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "TOUR DE ${activePlayer.name.uppercase()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isBotPaused) colors.alertNeon else colors.secondaryNeon,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isBotPaused) "Pause active • inspectez les dés ou avancez pas à pas" else if (isBotThinking) "IA en cours de réflexion..." else "Tour IA en cours...",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelButton(
                            text = if (isBotPaused) "▶ REPRENDRE" else "⏸ PAUSE IA",
                            accentColor = if (isBotPaused) colors.primaryNeon else colors.alertNeon,
                            filled = isBotPaused,
                            onClick = {
                                onTriggerHaptic()
                                onEvent(GameUiEvent.ToggleBotPause)
                            }
                        )
                        if (isBotPaused) {
                            PixelButton(
                                text = "⏭ PAS À PAS",
                                accentColor = colors.secondaryNeon,
                                filled = false,
                                onClick = {
                                    onTriggerHaptic()
                                    onEvent(GameUiEvent.StepBotTurn)
                                }
                            )
                        }
                    }
                }
            }
            return@Box
        }

        when (turn.phase) {
            TurnPhase.AWAITING_INITIAL_ROLL -> {
                val offer = turn.piggybackOffer
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (offer != null && rules.piggybackEnabled && activePlayer.isOpened) {
                        PixelButton(
                            text = "REPIQUER +${offer.inheritedPoints} PTS (${offer.remainingDiceCount} DÉS)",
                            onClick = {
                                onTriggerHaptic()
                                onEvent(GameUiEvent.AcceptPiggybackAndRoll())
                            },
                            accentColor = colors.secondaryNeon,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    EntropyRollButton(
                        text = "LANCER (${rules.diceCount} DÉS)",
                        accentColor = colors.primaryNeon,
                        onRollWithTouchEntropy = { x, y, durationMs ->
                            onTriggerHaptic()
                            onEvent(
                                GameUiEvent.RollDice(
                                    touchX = x,
                                    touchY = y,
                                    pressDurationMs = durationMs
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            TurnPhase.HOT_DICE_MANDATORY_REROLL -> {
                EntropyRollButton(
                    text = "★ MAIN PLEINE ! RELANCER (${rules.diceCount} DÉS)",
                    accentColor = colors.secondaryNeon,
                    onRollWithTouchEntropy = { x, y, durationMs ->
                        onTriggerHaptic()
                        onEvent(
                            GameUiEvent.RollDice(
                                touchX = x,
                                touchY = y,
                                pressDurationMs = durationMs
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            TurnPhase.SELECTING_DICE -> {
                val canRollNext = turn.isSelectionValid && turn.selectedPoints > 0
                val totalPot = turn.totalTurnPointsWithSelection
                val triggersHotDice = turn.selectionTriggersHotDice

                val bankValidationError = if (canRollNext) {
                    FiveThousandEngine.validateBankConditions(
                        player = activePlayer,
                        totalTurnPoints = totalPot,
                        triggersHotDice = triggersHotDice,
                        rules = rules
                    )
                } else {
                    null
                }
                val canBank = canRollNext && bankValidationError == null

                val nextDiceCount = if (triggersHotDice) rules.diceCount else turn.remainingDiceAfterSelection
                val rollLabel = if (triggersHotDice) {
                    "MAIN PLEINE (${rules.diceCount}D)"
                } else {
                    "RELANCER ($nextDiceCount D)"
                }

                val bankLabel = when {
                    !canRollNext -> "BANQUER"
                    triggersHotDice && rules.mandatoryRerollOnHotDice -> "MAIN PLEINE !"
                    !activePlayer.isOpened && totalPot < rules.openingThreshold ->
                        "OUV. ${rules.openingThreshold} PTS"
                    activePlayer.isOpened && rules.minTurnScoreAfterOpening > 0 && totalPot < rules.minTurnScoreAfterOpening ->
                        "MIN. ${rules.minTurnScoreAfterOpening} PTS"
                    rules.requireMultipleOf100ToBank && totalPot % 100 != 0 ->
                        "ROND ×100 REQUIS"
                    rules.finalSprintLast1000InOneTurn &&
                        activePlayer.score >= rules.targetScore - 1000 &&
                        activePlayer.score + totalPot < rules.targetScore ->
                        "SPRINT ${rules.targetScore} !"
                    rules.exactFinishRequired && activePlayer.score + totalPot > rules.targetScore ->
                        "DÉPASSE ${rules.targetScore}"
                    else -> "BANQUER +$totalPot"
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    // Indication contextuelle au-dessus des 2 boutons principaux (occupe l'emplacement supérieur du dock fixe)
                    val helperNote = when {
                        !canRollNext -> "Touchez au moins un dé scorant (ou « TOUT PRENDRE ») pour activer Relancer / Banquer."
                        bankValidationError != null -> "Relance requise : $bankLabel"
                        else -> "Projection si banque : ${activePlayer.score + totalPot} / ${rules.targetScore} pts"
                    }
                    Text(
                        text = helperNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bankValidationError != null) colors.alertNeon else colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val bankBtn: @Composable (Modifier) -> Unit = { mod ->
                            PixelButton(
                                text = bankLabel,
                                enabled = canBank,
                                filled = false,
                                accentColor = colors.secondaryNeon,
                                onClick = {
                                    onTriggerHaptic()
                                    onEvent(GameUiEvent.BankTurn)
                                },
                                modifier = mod
                            )
                        }

                        val rollBtn: @Composable (Modifier) -> Unit = { mod ->
                            EntropyRollButton(
                                text = rollLabel,
                                enabled = canRollNext,
                                accentColor = colors.primaryNeon,
                                onRollWithTouchEntropy = { x, y, durationMs ->
                                    onTriggerHaptic()
                                    onEvent(
                                        GameUiEvent.RollDice(
                                            touchX = x,
                                            touchY = y,
                                            pressDurationMs = durationMs
                                        )
                                    )
                                },
                                modifier = mod
                            )
                        }

                        if (leftHandedMode) {
                            rollBtn(Modifier.weight(1f))
                            bankBtn(Modifier.weight(1f))
                        } else {
                            bankBtn(Modifier.weight(1f))
                            rollBtn(Modifier.weight(1f))
                        }
                    }
                }
            }

            TurnPhase.BUSTED, TurnPhase.OVERFLOW_BUSTED -> {
                val label = if (turn.phase == TurnPhase.OVERFLOW_BUSTED) {
                    "DÉPASSEMENT (> ${rules.targetScore}) — PASSER LE TOUR"
                } else {
                    "COUP NUL (0 PT • ENCOCHE ✕) — PASSER LE TOUR"
                }
                PixelButton(
                    text = label,
                    accentColor = colors.alertNeon,
                    onClick = {
                        onTriggerHaptic()
                        onEvent(GameUiEvent.AcknowledgeBust)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Modale complète affichant la feuille des **Paliers & Encoches (`✕`)** de tous les joueurs
 * ainsi que l'historique chronologique intégral des lancers de la partie en cours.
 */
@Composable
private fun MatchHistoryAndCheckpointsDialog(
    match: MatchState,
    onDismiss: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    Dialog(onDismissRequest = onDismiss) {
        CyberPanel(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            borderColor = colors.primaryNeon
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelIconCanvas(icon = PixelIconId.BOOK, tint = colors.primaryNeon, iconSize = 18.dp)
                        Text(
                            text = "FEUILLE DE PALIERS (✕) & LOGS",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primaryNeon
                        )
                    }
                }

                // 1. Paliers et encoches de chaque joueur
                Text(
                    text = "ÉCHELLE DES PALIERS & ENCOCHES PAR JOUEUR",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondaryNeon
                )
                match.players.forEachIndexed { idx, player ->
                    val isCurrentTurn = idx == match.turn.activePlayerIndex
                    val stack = player.normalizedCheckpoints()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.background.copy(alpha = 0.65f), CyberCutShape)
                            .border(
                                width = 1.dp,
                                color = if (isCurrentTurn) colors.primaryNeon else colors.textMuted.copy(alpha = 0.3f),
                                shape = CyberCutShape
                            )
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (isCurrentTurn) "▶ " else ""}${player.name} (${player.score} pts)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentTurn) colors.primaryNeon else colors.textPrimary
                            )
                            Text(
                                text = "Actuel : [${buildStrikesString(player.consecutiveBusts)}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (player.consecutiveBusts >= 2) colors.alertNeon else colors.secondaryNeon
                            )
                        }
                        if (stack.isEmpty()) {
                            Text(
                                text = "Aucun palier banqué (0 pt • [${buildStrikesString(player.consecutiveBusts)}])",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        } else {
                            val ladderText = stack.mapIndexed { cpIdx, cp ->
                                val strikes = if (cpIdx == stack.lastIndex) player.consecutiveBusts else cp.strikes
                                "#${cpIdx + 1}: ${cp.score} [${buildStrikesString(strikes)}]"
                            }.joinToString("  →  ")
                            Text(
                                text = ladderText,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // 2. Journal chronologique des lancers
                Text(
                    text = "JOURNAL DES LANCERS (${match.rollHistoryLog.size} ÉVÉNEMENTS)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                if (match.rollHistoryLog.isEmpty()) {
                    Text(
                        text = "Aucun lancer enregistré pour le moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        match.rollHistoryLog.asReversed().forEach { entry ->
                            CompactRollLogRow(entry = entry)
                        }
                    }
                }

                PixelButton(
                    text = "FERMER LE JOURNAL",
                    icon = PixelIconId.BACK,
                    accentColor = colors.primaryNeon,
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Bouton spécialisé capturant la position exacte `(touchX, touchY)` et la durée d'appui en millisecondes (`pressDurationMs`)
 * pour alimenter le générateur d'entropie hybride `SHA-256`.
 */
@Composable
private fun EntropyRollButton(
    text: String,
    onRollWithTouchEntropy: (touchX: Float, touchY: Float, pressDurationMs: Long) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = LocalNeon5KColors.current.primaryNeon
) {
    val colors = LocalNeon5KColors.current
    val bgColor = if (enabled) accentColor else colors.surface
    val contentColor = if (enabled) colors.background else colors.textMuted

    Box(
        modifier = modifier
            .heightIn(min = 50.dp)
            .background(color = bgColor, shape = CyberCutShape)
            .border(
                width = 2.dp,
                color = if (enabled) accentColor else colors.textMuted.copy(alpha = 0.35f),
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
