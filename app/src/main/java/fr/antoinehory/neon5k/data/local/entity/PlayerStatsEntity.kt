package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room singleton (`id = 1`) stockant les statistiques locales cumulées du joueur humain.
 */
@Entity(tableName = "player_stats")
data class PlayerStatsEntity(
    @PrimaryKey
    val id: Int = 1,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val bestTurnScore: Int = 0,
    val totalHotDiceCount: Int = 0,
    val totalBustsCount: Int = 0,
    val biggestPiggybackScore: Int = 0,
    val totalRollsCount: Int = 0
)
