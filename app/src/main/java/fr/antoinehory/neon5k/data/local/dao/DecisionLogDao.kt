package fr.antoinehory.neon5k.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.antoinehory.neon5k.data.local.entity.DecisionLogEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room pour le journal des décisions destiné à l'entraînement futur d'IA (`decision_logs`).
 */
@Dao
interface DecisionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecision(entity: DecisionLogEntity): Long

    /**
     * Complète rétroactivement l'issue du tour (`turnFinalPoints` : `0` si coup nul, sinon points banqués)
     * en une seule requête SQL `UPDATE`.
     */
    @Query(
        """
        UPDATE decision_logs
        SET turnFinalPoints = :finalPoints
        WHERE matchUuid = :matchUuid AND turnIndex = :turnIndex
        """
    )
    suspend fun updateTurnOutcome(matchUuid: String, turnIndex: Int, finalPoints: Int)

    /**
     * Complète rétroactivement l'issue de la partie (`matchWon`) pour tous les coups de la partie
     * en une seule requête SQL `UPDATE`.
     */
    @Query(
        """
        UPDATE decision_logs
        SET matchWon = CASE WHEN actorId = :winnerActorId THEN 1 ELSE 0 END
        WHERE matchUuid = :matchUuid
        """
    )
    suspend fun updateMatchOutcome(matchUuid: String, winnerActorId: Int)

    @Query("SELECT COUNT(*) FROM decision_logs")
    fun observeTotalCount(): Flow<Int>

    @Query("SELECT * FROM decision_logs ORDER BY id ASC")
    suspend fun getAllDecisions(): List<DecisionLogEntity>

    @Query("DELETE FROM decision_logs")
    suspend fun clearAllDecisions()
}
