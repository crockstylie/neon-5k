package fr.antoinehory.neon5k.domain.achievement

import fr.antoinehory.neon5k.domain.rules.CombinationKind
import fr.antoinehory.neon5k.domain.rules.ScoringCombination

/**
 * Événement métier pur évalué par [AchievementEvaluator] après un lancer, une banque, un coup nul ou une fin de partie.
 */
sealed interface TelemetryGameEvent {
    /**
     * Un joueur humain vient de lancer `rolledFaces` (dés libres).
     */
    data class HumanRollExecuted(
        val rolledFaces: List<Int>,
        val combinationsOnRoll: List<ScoringCombination>,
        val triggeredByShake: Boolean,
        val pressDurationMs: Long,
        val scoredOnSingleRemainingDie: Boolean
    ) : TelemetryGameEvent

    /**
     * Un joueur humain vient de banquer son tour.
     */
    data class HumanTurnBanked(
        val turnPoints: Int,
        val wasOpeningTurn: Boolean,
        val hotDiceCountInTurn: Int,
        val usedPiggyback: Boolean,
        val bumpedOpponent: Boolean
    ) : TelemetryGameEvent

    /**
     * Un joueur humain vient de subir un Coup nul (Glitch / Dépassement).
     */
    data class HumanTurnBusted(
        val lostPotPoints: Int,
        val triggeredThreeStrikesPenalty: Boolean
    ) : TelemetryGameEvent

    /**
     * Une partie contenant au moins un joueur humain vient de se terminer.
     */
    data class MatchCompleted(
        val humanWon: Boolean,
        val wonWithExactTarget: Boolean,
        val defeatedBotSafe: Boolean,
        val defeatedBotBalanced: Boolean,
        val defeatedBotKamikaze: Boolean,
        val hasHumanPlayer: Boolean = true
    ) : TelemetryGameEvent
}

/**
 * Résultat pur de l'évaluation d'un événement sur la liste des 20 succès.
 */
data class AchievementEvaluationOutcome(
    val updatedProgressMap: Map<AchievementId, AchievementProgress>,
    val notifications: List<AchievementUnlockNotification>
)

/**
 * Évaluateur pur Kotlin des 20 succès à comptage continu post-déverrouillage (ADR-011).
 *
 * Règle fondamentale :
 * - Même lorsqu'un succès est déjà `isUnlocked == true`, chaque nouvelle occurrence incrémente `lifetimeCount`
 *   et peut déclencher une promotion de rang de maîtrise (*Bronze → Argent → Or → Cyber-Platine*).
 */
object AchievementEvaluator {

    /**
     * Initialise la map par défaut des 20 succès à 0.
     */
    fun initialProgressMap(): Map<AchievementId, AchievementProgress> =
        AchievementId.entries.associateWith { id -> AchievementProgress(id = id) }

    /**
     * Applique un [TelemetryGameEvent] à l'état courant des succès et retourne les nouvelles progressions
     * ainsi que les notifications de déblocage ou de montée de rang.
     *
     * @param currentMap État actuel des 20 succès.
     * @param event Événement métier survenu.
     * @param distinctTripleFacesMask Masque binaire (bits 1..6) des faces de brelans déjà obtenues en carrière (pour `BRELAN_COLLECTOR`).
     * @param nowEpochMs Horodatage courant pour marquer `unlockedAtEpochMs`.
     */
    fun evaluate(
        currentMap: Map<AchievementId, AchievementProgress>,
        event: TelemetryGameEvent,
        distinctTripleFacesMask: Int = 0,
        nowEpochMs: Long = System.currentTimeMillis()
    ): AchievementEvaluationOutcome {
        val working = initialProgressMap().toMutableMap().apply {
            putAll(currentMap)
        }
        val notifications = mutableListOf<AchievementUnlockNotification>()

        fun incrementAchievement(
            id: AchievementId,
            deltaCount: Int = 1,
            overrideProgressSteps: Int? = null
        ) {
            if (deltaCount <= 0 && overrideProgressSteps == null) return
            val existing = working[id] ?: AchievementProgress(id = id)
            val oldRank = existing.masteryRank
            val wasUnlocked = existing.isUnlocked

            val newLifetime = existing.lifetimeCount + deltaCount
            val newSteps = overrideProgressSteps
                ?: (existing.progressSteps + deltaCount).coerceAtMost(id.targetSteps)
            val nowUnlocked = wasUnlocked || newSteps >= id.targetSteps || newLifetime >= id.targetSteps

            val updated = existing.copy(
                isUnlocked = nowUnlocked,
                progressSteps = if (nowUnlocked) id.targetSteps else newSteps,
                lifetimeCount = newLifetime,
                unlockedAtEpochMs = existing.unlockedAtEpochMs ?: if (nowUnlocked) nowEpochMs else null
            )
            working[id] = updated

            val newRank = updated.masteryRank
            if (!wasUnlocked && nowUnlocked) {
                notifications.add(
                    AchievementUnlockNotification(
                        achievementId = id,
                        newRank = newRank,
                        lifetimeCount = newLifetime,
                        isFirstUnlock = true
                    )
                )
            } else if (wasUnlocked && newRank.ordinal > oldRank.ordinal) {
                notifications.add(
                    AchievementUnlockNotification(
                        achievementId = id,
                        newRank = newRank,
                        lifetimeCount = newLifetime,
                        isFirstUnlock = false
                    )
                )
            }
        }

        when (event) {
            is TelemetryGameEvent.HumanRollExecuted -> {
                var triplesInRoll = 0
                for (combo in event.combinationsOnRoll) {
                    when (combo.kind) {
                        CombinationKind.THREE_OF_A_KIND -> {
                            triplesInRoll++
                            if (combo.diceFaces.firstOrNull() == 1) {
                                incrementAchievement(AchievementId.TRIPLE_ACE_PROTOCOL)
                            }
                        }
                        CombinationKind.FOUR_OF_A_KIND -> {
                            triplesInRoll++
                            if (combo.diceFaces.firstOrNull() == 1) {
                                incrementAchievement(AchievementId.TRIPLE_ACE_PROTOCOL)
                            }
                            incrementAchievement(AchievementId.QUAD_CORE)
                        }
                        CombinationKind.FIVE_OF_A_KIND, CombinationKind.SIX_OF_A_KIND -> {
                            triplesInRoll++
                            if (combo.diceFaces.firstOrNull() == 1) {
                                incrementAchievement(AchievementId.TRIPLE_ACE_PROTOCOL)
                            }
                            incrementAchievement(AchievementId.FIVE_OF_A_KIND)
                        }
                        CombinationKind.STRAIGHT -> {
                            incrementAchievement(AchievementId.STRAIGHT_RUNNER)
                        }
                        CombinationKind.THREE_PAIRS -> {
                            incrementAchievement(AchievementId.TWIN_MATRIX)
                        }
                        CombinationKind.FULL_HOUSE -> {
                            triplesInRoll++
                            if (combo.diceFaces.groupingBy { it }.eachCount()[1] == 3) {
                                incrementAchievement(AchievementId.TRIPLE_ACE_PROTOCOL)
                            }
                            incrementAchievement(AchievementId.FULL_HOUSE_CYBER)
                        }
                        CombinationKind.SINGLE_ONE, CombinationKind.SINGLE_FIVE -> Unit
                    }
                }

                if (triplesInRoll > 0 || distinctTripleFacesMask > 0) {
                    val distinctFacesCount = Integer.bitCount(distinctTripleFacesMask and 0b1111110)
                    incrementAchievement(
                        id = AchievementId.BRELAN_COLLECTOR,
                        deltaCount = triplesInRoll,
                        overrideProgressSteps = distinctFacesCount.coerceIn(0, 6)
                    )
                }

                if (event.scoredOnSingleRemainingDie) {
                    incrementAchievement(AchievementId.ONE_DIE_MIRACLE)
                }
            }

            is TelemetryGameEvent.HumanTurnBanked -> {
                if (event.wasOpeningTurn) {
                    incrementAchievement(AchievementId.FIRST_BOOT)
                    if (event.turnPoints >= 1500) {
                        incrementAchievement(AchievementId.HIGH_VOLTAGE_OPENING)
                    }
                }
                if (event.hotDiceCountInTurn >= 1) {
                    incrementAchievement(AchievementId.HOT_DICE, deltaCount = event.hotDiceCountInTurn)
                }
                if (event.hotDiceCountInTurn >= 2) {
                    incrementAchievement(AchievementId.FULL_OVERCLOCK)
                }
                if (event.turnPoints >= 2000) {
                    incrementAchievement(AchievementId.OVERKILL_TURN)
                }
                if (event.usedPiggyback && event.turnPoints > 0) {
                    incrementAchievement(AchievementId.HIJACKER)
                }
                if (event.bumpedOpponent) {
                    incrementAchievement(AchievementId.HEADSHOT)
                }
            }

            is TelemetryGameEvent.HumanTurnBusted -> {
                if (event.lostPotPoints >= 1000) {
                    incrementAchievement(AchievementId.SYSTEM_CRASH)
                }
                if (event.triggeredThreeStrikesPenalty) {
                    incrementAchievement(AchievementId.THREE_STRIKES_OUT)
                }
            }

            is TelemetryGameEvent.MatchCompleted -> {
                if (event.hasHumanPlayer) {
                    incrementAchievement(AchievementId.ENTROPY_MASTER, deltaCount = 1)
                    if (event.humanWon && event.wonWithExactTarget) {
                        incrementAchievement(AchievementId.PIXEL_PERFECT_5000)
                    }
                    if (event.humanWon && event.defeatedBotKamikaze) {
                        incrementAchievement(AchievementId.BEAT_THE_MACHINE)
                    }
                }
            }
        }

        return AchievementEvaluationOutcome(
            updatedProgressMap = working,
            notifications = notifications
        )
    }
}
