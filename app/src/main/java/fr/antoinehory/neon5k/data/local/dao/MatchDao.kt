package fr.antoinehory.neon5k.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room pour la sauvegarde automatique et la reprise d'une partie en cours ainsi que l'historique des parties.
 */
@Dao
interface MatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMatch(entity: SavedMatchEntity)

    @Query("SELECT * FROM saved_matches WHERE isFinished = 0 ORDER BY updatedAtEpochMs DESC LIMIT 1")
    fun observeLatestActiveMatch(): Flow<SavedMatchEntity?>

    @Query("SELECT * FROM saved_matches WHERE isFinished = 0 ORDER BY updatedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestActiveMatch(): SavedMatchEntity?

    @Query("SELECT * FROM saved_matches WHERE matchUuid = :matchUuid LIMIT 1")
    suspend fun getMatchById(matchUuid: String): SavedMatchEntity?

    @Query("SELECT * FROM saved_matches WHERE isFinished = 1 ORDER BY updatedAtEpochMs DESC LIMIT :limit")
    fun observeFinishedMatches(limit: Int = 20): Flow<List<SavedMatchEntity>>

    @Query("DELETE FROM saved_matches WHERE isFinished = 0 AND matchUuid != :keepMatchUuid")
    suspend fun deleteOtherActiveMatches(keepMatchUuid: String)
}
