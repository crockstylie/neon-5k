package fr.antoinehory.neon5k.data.playgames

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.games.PlayGames
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
    val configuredProjectId: String = "",
    val lastSyncedAchievementsCount: Int = 0,
    val statusNoteFr: String = "Suivi local actif (20 succès à compteurs infinis)"
)

/**
 * Service d'intégration Google Play Jeux v2 (Lot 4).
 * - Fonctionne à 100 % hors-ligne grâce à la persistance Room locale (`AchievementEntity`).
 * - L'initialisation globale du SDK (`PlayGamesSdk.initialize`) est assurée dans [fr.antoinehory.neon5k.Neon5KApplication].
 * - Si `game_services_project_id` est renseigné avec l'ID Play Console (`!= 000000000000`) et qu'une `Activity`
 *   est présente, synchronise silencieusement les déblocages (`unlock`) et étapes (`setSteps`) vers Google Play Jeux.
 */
class GooglePlayGamesService(
    private val appContext: Context
) {
    private val projectId: String = resolveProjectId(appContext)

    private val isProjectConfigured: Boolean =
        projectId.isNotBlank() && projectId != PLACEHOLDER_PROJECT_ID

    private val _status = MutableStateFlow(
        PlayGamesStatus(
            isConfiguredInManifest = isProjectConfigured,
            isAuthenticated = false,
            configuredProjectId = if (isProjectConfigured) projectId else "",
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
                val diagSuffix = if (!task.isSuccessful) {
                    " [${formatApiError(task.exception)}]"
                } else {
                    ""
                }
                _status.value = _status.value.copy(
                    isConfiguredInManifest = true,
                    isAuthenticated = isAuth,
                    configuredProjectId = projectId,
                    statusNoteFr = if (isAuth) {
                        "Connecté à Google Play Jeux v2 (Sync cloud active)"
                    } else {
                        "Google Play Jeux non connecté$diagSuffix (Progression locale active)"
                    }
                )
            }
        }.onFailure { err ->
            _status.value = _status.value.copy(
                isAuthenticated = false,
                statusNoteFr = "Google Play Jeux indisponible (${err.localizedMessage ?: "erreur init"})"
            )
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
                    _status.value = _status.value.copy(
                        isAuthenticated = true,
                        statusNoteFr = "Connecté à Google Play Jeux v2 (Sync cloud active)"
                    )
                    openNativeAchievementsOverlay(activity, onFallbackMessage)
                } else {
                    signInClient.signIn().addOnCompleteListener { signInTask ->
                        val signedIn = signInTask.isSuccessful && (signInTask.result?.isAuthenticated == true)
                        val diagSuffix = if (!signedIn && !signInTask.isSuccessful) {
                            " [${formatApiError(signInTask.exception)}]"
                        } else {
                            ""
                        }
                        _status.value = _status.value.copy(
                            isAuthenticated = signedIn,
                            statusNoteFr = if (signedIn) {
                                "Connecté à Google Play Jeux v2 (Sync cloud active)"
                            } else {
                                "Connexion Play Jeux refusée ou indisponible$diagSuffix (Suivi local actif)"
                            }
                        )
                        if (signedIn) {
                            openNativeAchievementsOverlay(activity, onFallbackMessage)
                        } else {
                            onFallbackMessage(
                                "Connexion Google Play Jeux indisponible$diagSuffix."
                            )
                        }
                    }
                }
            }
        }.onFailure { err ->
            onFallbackMessage("Service Google Play Jeux indisponible (${err.localizedMessage ?: "erreur"}).")
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
                .addOnFailureListener { err ->
                    val diag = formatApiError(err)
                    _status.value = _status.value.copy(
                        statusNoteFr = "Erreur overlay Play Jeux [$diag]"
                    )
                    onFallbackMessage("Impossible d'ouvrir les succès Google Play Jeux ($diag).")
                }
        }
    }

    /**
     * Synchronise les succès débloqués ou incrémentiels vers Google Play Jeux si le joueur est authentifié.
     * Retourne le nombre de succès transmis.
     */
    fun syncAchievementsToPlayGames(progressList: Collection<AchievementProgress>): Int {
        val activity = currentActivity ?: return 0
        if (!isProjectConfigured || !_status.value.isAuthenticated) return 0

        var synced = 0
        runCatching {
            val client = PlayGames.getAchievementsClient(activity)
            for (progress in progressList) {
                val pgsId = resolvePlayGamesAchievementId(progress.id)
                if (pgsId.isBlank() || pgsId.startsWith("CgkI_placeholder_")) continue

                if (progress.id.targetSteps > 1) {
                    val steps = progress.progressSteps.coerceIn(0, progress.id.targetSteps)
                    if (steps > 0) {
                        client.setSteps(pgsId, steps)
                        synced++
                    }
                } else if (progress.isUnlocked) {
                    client.unlock(pgsId)
                    synced++
                }
            }
        }
        _status.value = _status.value.copy(lastSyncedAchievementsCount = synced)
        return synced
    }

    private fun formatApiError(exception: Exception?): String {
        val apiEx = exception as? ApiException
        if (apiEx != null) {
            val code = apiEx.statusCode
            val label = when (code) {
                CommonStatusCodes.SIGN_IN_REQUIRED -> "SIGN_IN_REQUIRED"
                CommonStatusCodes.DEVELOPER_ERROR -> "DEVELOPER_ERROR / SHA-1 ou Config Play Console"
                CommonStatusCodes.NETWORK_ERROR -> "NETWORK_ERROR"
                else -> CommonStatusCodes.getStatusCodeString(code)
            }
            return "code $code ($label)"
        }
        return exception?.localizedMessage?.take(60) ?: "non authentifié"
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
        val fromResources = runCatching { appContext.getString(resId).trim() }.getOrDefault("")
        if (fromResources.isNotBlank() && !fromResources.startsWith(PLACEHOLDER_ACHIEVEMENT_PREFIX)) {
            return fromResources
        }
        return OFFICIAL_ACHIEVEMENT_IDS[id].orEmpty()
    }

    companion object {
        private const val RC_ACHIEVEMENTS_UI = 9003
        const val PLACEHOLDER_PROJECT_ID = "000000000000"
        private const val PLACEHOLDER_ACHIEVEMENT_PREFIX = "CgkI_placeholder_"
        const val OFFICIAL_PROJECT_ID = "59639547526"

        val OFFICIAL_ACHIEVEMENT_IDS: Map<AchievementId, String> = mapOf(
            AchievementId.FIRST_BOOT to "CgkIho2tlt4BEAIQDA",
            AchievementId.HIGH_VOLTAGE_OPENING to "CgkIho2tlt4BEAIQFA",
            AchievementId.TRIPLE_ACE_PROTOCOL to "CgkIho2tlt4BEAIQDQ",
            AchievementId.BRELAN_COLLECTOR to "CgkIho2tlt4BEAIQBg",
            AchievementId.QUAD_CORE to "CgkIho2tlt4BEAIQCw",
            AchievementId.FIVE_OF_A_KIND to "CgkIho2tlt4BEAIQCQ",
            AchievementId.STRAIGHT_RUNNER to "CgkIho2tlt4BEAIQBQ",
            AchievementId.TWIN_MATRIX to "CgkIho2tlt4BEAIQBA",
            AchievementId.FULL_HOUSE_CYBER to "CgkIho2tlt4BEAIQAQ",
            AchievementId.HOT_DICE to "CgkIho2tlt4BEAIQDw",
            AchievementId.FULL_OVERCLOCK to "CgkIho2tlt4BEAIQBw",
            AchievementId.ONE_DIE_MIRACLE to "CgkIho2tlt4BEAIQEw",
            AchievementId.SYSTEM_CRASH to "CgkIho2tlt4BEAIQEg",
            AchievementId.THREE_STRIKES_OUT to "CgkIho2tlt4BEAIQEA",
            AchievementId.PIXEL_PERFECT_5000 to "CgkIho2tlt4BEAIQAg",
            AchievementId.OVERKILL_TURN to "CgkIho2tlt4BEAIQCA",
            AchievementId.HIJACKER to "CgkIho2tlt4BEAIQDg",
            AchievementId.HEADSHOT to "CgkIho2tlt4BEAIQEQ",
            AchievementId.BEAT_THE_MACHINE to "CgkIho2tlt4BEAIQAw",
            AchievementId.ENTROPY_MASTER to "CgkIho2tlt4BEAIQCg"
        )

        /**
         * Résout l'ID du projet Play Games Services en privilégiant les ressources non localisées,
         * avec repli automatique sur l'ID officiel si un pack de langue Play Store injecte un ancien placeholder.
         */
        fun resolveProjectId(context: Context): String {
            val candidateResIds = listOf(
                R.string.pgs_app_id,
                R.string.game_services_project_id,
                R.string.app_id
            )
            for (resId in candidateResIds) {
                val value = runCatching { context.getString(resId).trim() }.getOrDefault("")
                if (value.isNotBlank() && value != PLACEHOLDER_PROJECT_ID) {
                    return value
                }
            }
            return OFFICIAL_PROJECT_ID
        }
    }
}
