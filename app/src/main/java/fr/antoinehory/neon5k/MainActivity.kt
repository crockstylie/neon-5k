package fr.antoinehory.neon5k

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.antoinehory.neon5k.ui.game.GameViewModel
import fr.antoinehory.neon5k.ui.navigation.NeonNavHost
import fr.antoinehory.neon5k.ui.theme.Neon5KTheme

class MainActivity : ComponentActivity() {

    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            Neon5KTheme {
                NeonNavHost(gameViewModel = gameViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        gameViewModel.attachActivity(this)
    }

    override fun onStop() {
        gameViewModel.detachActivity(this)
        super.onStop()
    }
}
