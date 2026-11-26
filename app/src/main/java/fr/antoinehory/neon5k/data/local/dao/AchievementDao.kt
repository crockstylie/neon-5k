package fr.antoinehory.neon5k.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.antoinehory.neon5k.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room pour le suivi réactif des 20 succès à comptage continu post-déverrouillage (Lot 4).
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun observeAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(entities: List<AchievementEntity>)
}
