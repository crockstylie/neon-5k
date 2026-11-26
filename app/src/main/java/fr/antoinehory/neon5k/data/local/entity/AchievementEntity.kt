package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.antoinehory.neon5k.domain.achievement.AchievementId
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress

/**
 * Entité Room stockant l'état de déverrouillage et le compteur à vie infini (`lifetimeCount`)
 * d'un des 20 succès de Neon 5K (Lot 4 - ADR-011).
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val achievementId: String,
    val isUnlocked: Boolean,
    val progressSteps: Int,
    val lifetimeCount: Int,
    val unlockedAtEpochMs: Long?
)

fun AchievementProgress.toEntity(): AchievementEntity = AchievementEntity(
    achievementId = id.name,
    isUnlocked = isUnlocked,
    progressSteps = progressSteps,
    lifetimeCount = lifetimeCount,
    unlockedAtEpochMs = unlockedAtEpochMs
)

fun AchievementEntity.toDomainOrNull(): AchievementProgress? {
    val enumId = runCatching { AchievementId.valueOf(achievementId) }.getOrNull() ?: return null
    return AchievementProgress(
        id = enumId,
        isUnlocked = isUnlocked,
        progressSteps = progressSteps,
        lifetimeCount = lifetimeCount,
        unlockedAtEpochMs = unlockedAtEpochMs
    )
}
