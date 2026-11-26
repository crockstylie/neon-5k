package fr.antoinehory.neon5k

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import fr.antoinehory.neon5k.ui.navigation.NeonNavHost
import fr.antoinehory.neon5k.ui.theme.Neon5KTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Neon5KTheme {
                NeonNavHost()
            }
        }
    }
}
