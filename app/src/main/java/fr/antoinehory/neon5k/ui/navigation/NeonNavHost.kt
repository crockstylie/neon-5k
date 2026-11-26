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
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.ui.components.ScanlineOverlay
import fr.antoinehory.neon5k.ui.game.GameScreen
import fr.antoinehory.neon5k.ui.game.GameViewModel
import fr.antoinehory.neon5k.ui.home.HomeScreen
import fr.antoinehory.neon5k.ui.setup.SetupScreen
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Destinations de navigation de Neon 5K (Lot 1 : Home, Setup, Game).
 */
enum class NeonDestination {
    HOME,
    SETUP,
    GAME
}

/**
 * Conteneur de navigation principal de Neon 5K avec surcouche CRT Scanlines optionnelle.
 */
@Composable
fun NeonNavHost(
    modifier: Modifier = Modifier,
    gameViewModel: GameViewModel = viewModel()
) {
    val colors = LocalNeon5KColors.current
    val uiState by gameViewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(NeonDestination.HOME) }

    BackHandler(enabled = destination != NeonDestination.HOME) {
        destination = NeonDestination.HOME
    }

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
                            rules = GameRulesConfig.CLASSIC_5_DICE,
                            players = listOf(
                                PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
                                PlayerState(id = 1, name = "JOUEUR 2", type = PlayerType.HUMAN)
                            )
                        )
                        destination = NeonDestination.GAME
                    },
                    onOpenSetup = { destination = NeonDestination.SETUP }
                )
            }

            NeonDestination.SETUP -> {
                SetupScreen(
                    initialRules = uiState.lastConfiguredRules,
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
                    onEvent = gameViewModel::onEvent,
                    onResumed = gameViewModel::onGameScreenResumed,
                    onPaused = gameViewModel::onGameScreenPaused,
                    onNavigateHome = { destination = NeonDestination.HOME },
                    onNavigateSetup = { destination = NeonDestination.SETUP }
                )
            }
        }

        ScanlineOverlay()
    }
}
