package fr.antoinehory.neon5k.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.ui.components.ScanlineOverlay
import fr.antoinehory.neon5k.ui.customization.CustomizationScreen
import fr.antoinehory.neon5k.ui.game.GameScreen
import fr.antoinehory.neon5k.ui.game.GameViewModel
import fr.antoinehory.neon5k.ui.home.HomeScreen
import fr.antoinehory.neon5k.ui.info.InfoScreen
import fr.antoinehory.neon5k.ui.rules.RulesScreen
import fr.antoinehory.neon5k.ui.setup.SetupScreen
import fr.antoinehory.neon5k.ui.stats.StatsScreen
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import fr.antoinehory.neon5k.ui.theme.Neon5KTheme

/**
 * Destinations de navigation de Neon 5K (Lots 1, 2 & 3 : Home, Setup, Game, Customization, Stats, Rules, Info).
 */
enum class NeonDestination {
    HOME,
    SETUP,
    GAME,
    CUSTOMIZATION,
    STATS,
    RULES,
    INFO
}

/**
 * Conteneur de navigation principal de Neon 5K appliquant dynamiquement le thème `Neon5KTheme`
 * issu des préférences utilisateur DataStore.
 */
@Composable
fun NeonNavHost(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel()
) {
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    val preferences by gameViewModel.preferencesState.collectAsStateWithLifecycle()
    val playerStats by gameViewModel.playerStatsState.collectAsStateWithLifecycle()
    val finishedMatches by gameViewModel.finishedMatchesState.collectAsStateWithLifecycle()
    val adaptiveLearning by gameViewModel.adaptiveLearningState.collectAsStateWithLifecycle()
    val achievements by gameViewModel.achievementsState.collectAsStateWithLifecycle()
    val playGamesStatus by gameViewModel.playGamesStatusState.collectAsStateWithLifecycle()
    val decisionLogsCount by gameViewModel.decisionLogsCountState.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(NeonDestination.HOME) }

    BackHandler(enabled = destination != NeonDestination.HOME) {
        destination = NeonDestination.HOME
    }

    Neon5KTheme(
        palette = preferences.activePalette,
        diceStyle = preferences.diceStyle,
        showScanlines = preferences.showScanlines
    ) {
        val colors = LocalNeon5KColors.current

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            when (destination) {
                NeonDestination.HOME -> {
                    HomeScreen(
                        hasActiveMatch = uiState.hasActiveMatch,
                        onResumeMatch = { destination = NeonDestination.GAME },
                        onStartQuickMatch = {
                            gameViewModel.startMatch(
                                rules = preferences.favoriteRules,
                                players = listOf(
                                    PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
                                    PlayerState(id = 1, name = "CYBER-BOT", type = PlayerType.BOT_BALANCED)
                                )
                            )
                            destination = NeonDestination.GAME
                        },
                        onOpenSetup = { destination = NeonDestination.SETUP },
                        onOpenCustomization = { destination = NeonDestination.CUSTOMIZATION },
                        onOpenStats = { destination = NeonDestination.STATS },
                        onOpenRules = { destination = NeonDestination.RULES },
                        onOpenInfo = { destination = NeonDestination.INFO }
                    )
                }

                NeonDestination.SETUP -> {
                    SetupScreen(
                        initialRules = preferences.favoriteRules,
                        onBack = { destination = NeonDestination.HOME },
                        onLaunchMatch = { rules, players ->
                            gameViewModel.startMatch(rules = rules, players = players)
                            destination = NeonDestination.GAME
                        }
                    )
                }

                NeonDestination.GAME -> {
                    GameScreen(
                        uiState = uiState,
                        preferences = preferences,
                        onEvent = gameViewModel::onEvent,
                        onResumed = gameViewModel::onGameScreenResumed,
                        onPaused = gameViewModel::onGameScreenPaused,
                        onNavigateHome = { destination = NeonDestination.HOME },
                        onNavigateSetup = { destination = NeonDestination.SETUP }
                    )
                }

                NeonDestination.CUSTOMIZATION -> {
                    CustomizationScreen(
                        preferences = preferences,
                        onSelectThemePreset = gameViewModel::setThemePreset,
                        onUpdateCustomPalette = gameViewModel::updateCustomPalette,
                        onSelectDiceStyle = gameViewModel::setDiceStyle,
                        onToggleScanlines = gameViewModel::setShowScanlines,
                        onToggleLeftHanded = gameViewModel::setLeftHandedMode,
                        onToggleHaptic = gameViewModel::setHapticEnabled,
                        onToggleSound = gameViewModel::setSoundEnabled,
                        onSelectBotSpeed = gameViewModel::setBotSpeed,
                        onToggleShakeToRoll = gameViewModel::setShakeToRollEnabled,
                        onToggleKeepScreenOn = gameViewModel::setKeepScreenOn,
                        onBack = { destination = NeonDestination.HOME }
                    )
                }

                NeonDestination.STATS -> {
                    StatsScreen(
                        stats = playerStats,
                        finishedMatches = finishedMatches,
                        adaptiveLearning = adaptiveLearning,
                        achievements = achievements,
                        playGamesStatus = playGamesStatus,
                        decisionLogsCount = decisionLogsCount,
                        exportStatusMessageFr = uiState.exportStatusMessageFr,
                        onOpenPlayGamesAchievements = gameViewModel::openPlayGamesAchievements,
                        onSyncPlayGamesNow = gameViewModel::syncPlayGamesNow,
                        onExportDataset = gameViewModel::exportAiDataset,
                        onClearDataset = gameViewModel::clearAiDataset,
                        onBack = { destination = NeonDestination.HOME }
                    )
                }

                NeonDestination.RULES -> {
                    RulesScreen(
                        activeRules = preferences.favoriteRules,
                        onBack = { destination = NeonDestination.HOME }
                    )
                }

                NeonDestination.INFO -> {
                    InfoScreen(
                        onBack = { destination = NeonDestination.HOME }
                    )
                }
            }

            ScanlineOverlay()
        }
    }
}
