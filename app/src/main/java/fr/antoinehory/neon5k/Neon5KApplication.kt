package fr.antoinehory.neon5k

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk
import fr.antoinehory.neon5k.data.playgames.GooglePlayGamesService

/**
 * Classe [Application] de Neon 5K.
 * Initialise Google Play Games Services v2 dès le démarrage du processus, conformément à la
 * recommandation officielle Google Play Jeux v2.
 */
class Neon5KApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val projectId = GooglePlayGamesService.resolveProjectId(this)
        if (projectId.isNotBlank() && projectId != GooglePlayGamesService.PLACEHOLDER_PROJECT_ID) {
            runCatching {
                PlayGamesSdk.initialize(this)
            }
        }
    }
}
