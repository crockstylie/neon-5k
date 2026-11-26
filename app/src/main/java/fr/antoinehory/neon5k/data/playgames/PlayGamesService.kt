package fr.antoinehory.neon5k.data.playgames

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import fr.antoinehory.neon5k.R
import fr.antoinehory.neon5k.domain.achievement.AchievementId
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État de connexion à Google Play Games Services v2 (PGS v2).
 */
data class PlayGamesStatus(
    val isConfiguredInManifest: Boolean = false,
    val isAuthenticated: Boolean = false,
    val statusNoteFr: String = "Suivi local actif (20 succès à compteurs infinis)"
)

/**
 * Service d'intégration Google Play Jeux v2 (Lot 4).
 * - Fonctionne à 100 % hors-ligne grâce à la persistance Room locale (`AchievementEntity`).
 * - Si `game_services_project_id` est renseigné avec l'ID Play Console (`!= 000000000000`) et qu'une `Activity`
 *   est présente, synchronise silencieusement les déblocages (`unlock`) et étapes (`setSteps`) vers Google Play Jeux.
 */
class GooglePlayGamesService(
    private val appContext: Context
) {
    private val projectId: String = runCatching {
        appContext.getString(R.string.game_services_project_id)
    }.getOrDefault("000000000000")

    private val isProjectConfigured: Boolean =
        projectId.isNotBlank() && projectId != "000000000000"

    private val _status = MutableStateFlow(
        PlayGamesStatus(
            isConfiguredInManifest = isProjectConfigured,
            isAuthenticated = false,
            statusNoteFr = if (isProjectConfigured) {
                "Google Play Jeux v2 prêt"
            } else {
                "Mode local actif (ID Play Console à renseigner dans games-ids.xml avant publication)"
            }
        )
    )
    val status: StateFlow<PlayGamesStatus> = _status.asStateFlow()

    @Volatile
    private var currentActivity: Activity? = null

    init {
        if (isProjectConfigured) {
            runCatching {
                PlayGamesSdk.initialize(appContext)
            }
        }
    }

    /**
     * Attache l'activité courante et vérifie l'authentification silencieuse PGS v2.
     */
    fun attachActivity(activity: Activity) {
        currentActivity = activity
        if (!isProjectConfigured) return
        runCatching {
            val signInClient = PlayGames.getGamesSignInClient(activity)
            signInClient.isAuthenticated.addOnCompleteListener { task ->
                val isAuth = task.isSuccessful && (task.result?.isAuthenticated == true)
                _status.value = PlayGamesStatus(
                    isConfiguredInManifest = true,
                    isAuthenticated = isAuth,
                    statusNoteFr = if (isAuth) {
                        "Connecté à Google Play Jeux v2 (Sync cloud active)"
                    } else {
                        "Google Play Jeux non connecté (Progression sauvegardée localement)"
                    }
                )
            }
        }
    }

    fun detachActivity(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    /**
     * Déclenche une connexion manuelle à Google Play Jeux ou ouvre l'overlay natif des succès Play Jeux.
     */
    fun showNativePlayGamesAchievementsOrSignIn(onFallbackMessage: (String) -> Unit) {
        val activity = currentActivity
        if (!isProjectConfigured || activity == null) {
            onFallbackMessage(
                "Pour activer l'overlay Google Play Jeux, renseignez votre game_services_project_id dans res/values/games-ids.xml."
            )
            return
        }
        runCatching {
            val signInClient = PlayGames.getGamesSignInClient(activity)
            signInClient.isAuthenticated.addOnCompleteListener { authTask ->
                val isAuth = authTask.isSuccessful && (authTask.result?.isAuthenticated == true)
                if (isAuth) {
                    openNativeAchievementsOverlay(activity, onFallbackMessage)
                } else {
                    signInClient.signIn().addOnCompleteListener { signInTask ->
                        val signedIn = signInTask.isSuccessful && (signInTask.result?.isAuthenticated == true)
                        _status.value = _status.value.copy(
                            isAuthenticated = signedIn,
                            statusNoteFr = if (signedIn) {
                                "Connecté à Google Play Jeux v2"
                            } else {
                                "Connexion Play Jeux refusée ou indisponible (Suivi local actif)"
                            }
                        )
                        if (signedIn) {
                            openNativeAchievementsOverlay(activity, onFallbackMessage)
                        } else {
                            onFallbackMessage("Connexion Google Play Jeux indisponible sur cet appareil.")
                        }
                    }
                }
            }
        }.onFailure {
            onFallbackMessage("Service Google Play Jeux indisponible.")
        }
    }

    private fun openNativeAchievementsOverlay(
        activity: Activity,
        onFallbackMessage: (String) -> Unit
    ) {
        runCatching {
            PlayGames.getAchievementsClient(activity)
                .achievementsIntent
                .addOnSuccessListener { intent ->
                    activity.startActivityForResult(intent, RC_ACHIEVEMENTS_UI)
                }
                .addOnFailureListener {
                    onFallbackMessage("Impossible d'ouvrir la fenêtre Google Play Jeux.")
                }
        }
    }

    /**
     * Synchronise les succès débloqués ou incrémentiels vers Google Play Jeux si le joueur est authentifié.
     */
    fun syncAchievementsToPlayGames(progressList: Collection<AchievementProgress>) {
        val activity = currentActivity ?: return
        if (!isProjectConfigured) return

        runCatching {
            val client = PlayGames.getAchievementsClient(activity)
            for (progress in progressList) {
                val pgsId = resolvePlayGamesAchievementId(progress.id)
                if (pgsId.isBlank() || pgsId.startsWith("CgkI_placeholder_")) continue

                if (progress.id.targetSteps > 1) {
                    val steps = progress.progressSteps.coerceIn(1, progress.id.targetSteps)
                    if (steps > 0) {
                        client.setSteps(pgsId, steps)
                    }
                } else if (progress.isUnlocked) {
                    client.unlock(pgsId)
                }
            }
        }
    }

    private fun resolvePlayGamesAchievementId(id: AchievementId): String {
        val resId = when (id) {
            AchievementId.FIRST_BOOT -> R.string.achievement_first_boot
            AchievementId.HIGH_VOLTAGE_OPENING -> R.string.achievement_high_voltage_opening
            AchievementId.TRIPLE_ACE_PROTOCOL -> R.string.achievement_triple_ace_protocol
            AchievementId.BRELAN_COLLECTOR -> R.string.achievement_brelan_collector
            AchievementId.QUAD_CORE -> R.string.achievement_quad_core
            AchievementId.FIVE_OF_A_KIND -> R.string.achievement_five_of_a_kind
            AchievementId.STRAIGHT_RUNNER -> R.string.achievement_straight_runner
            AchievementId.TWIN_MATRIX -> R.string.achievement_twin_matrix
            AchievementId.FULL_HOUSE_CYBER -> R.string.achievement_full_house_cyber
            AchievementId.HOT_DICE -> R.string.achievement_hot_dice
            AchievementId.FULL_OVERCLOCK -> R.string.achievement_full_overclock
            AchievementId.ONE_DIE_MIRACLE -> R.string.achievement_one_die_miracle
            AchievementId.SYSTEM_CRASH -> R.string.achievement_system_crash
            AchievementId.THREE_STRIKES_OUT -> R.string.achievement_three_strikes_out
            AchievementId.PIXEL_PERFECT_5000 -> R.string.achievement_pixel_perfect_5000
            AchievementId.OVERKILL_TURN -> R.string.achievement_overkill_turn
            AchievementId.HIJACKER -> R.string.achievement_hijacker
            AchievementId.HEADSHOT -> R.string.achievement_headshot
            AchievementId.BEAT_THE_MACHINE -> R.string.achievement_beat_the_machine
            AchievementId.ENTROPY_MASTER -> R.string.achievement_entropy_master
        }
        return runCatching { appContext.getString(resId) }.getOrDefault("")
    }

    companion object {
        private const val RC_ACHIEVEMENTS_UI = 9003
    }
}
