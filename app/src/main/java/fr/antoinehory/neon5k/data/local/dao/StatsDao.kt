package fr.antoinehory.neon5k.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room pour les statistiques locales cumulées du joueur.
 */
@Dao
interface StatsDao {

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    fun observeStats(): Flow<PlayerStatsEntity?>

    @Query("SELECT * FROM player_stats WHERE id = 1 LIMIT 1")
    suspend fun getStats(): PlayerStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStats(entity: PlayerStatsEntity)
}
