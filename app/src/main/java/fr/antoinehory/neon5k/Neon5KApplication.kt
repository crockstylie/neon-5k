package fr.antoinehory.neon5k

import android.app.Application
import com.google.android.gms.games.PlayGamesSdk

/**
 * Classe [Application] de Neon 5K.
 * Initialise Google Play Games Services v2 dès le démarrage du processus, conformément à la
 * recommandation officielle Google Play Jeux v2.
 */
class Neon5KApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val projectId = runCatching { getString(R.string.game_services_project_id) }.getOrDefault("")
        if (projectId.isNotBlank() && projectId != "000000000000") {
            runCatching {
                PlayGamesSdk.initialize(this)
            }
        }
    }
}
