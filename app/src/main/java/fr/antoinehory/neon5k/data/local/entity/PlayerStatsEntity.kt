package fr.antoinehory.neon5k.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room singleton (`id = 1`) stockant les statistiques locales cumulées et la télémétrie fun du joueur humain (Lots 2 & 4).
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
    val totalRollsCount: Int = 0,

    // --- Entropie & Capteurs (Lot 4) ---
    val totalIndividualDiceThrown: Int = 0,
    val shakeRollsCount: Int = 0,
    val totalTouchPressDurationMs: Long = 0L,
    val face1Count: Int = 0,
    val face2Count: Int = 0,
    val face3Count: Int = 0,
    val face4Count: Int = 0,
    val face5Count: Int = 0,
    val face6Count: Int = 0,

    // --- Brelans & Combinaisons (Lot 4) ---
    val triple1Count: Int = 0,
    val triple2Count: Int = 0,
    val triple3Count: Int = 0,
    val triple4Count: Int = 0,
    val triple5Count: Int = 0,
    val triple6Count: Int = 0,
    val fourOfAKindCount: Int = 0,
    val fiveOfAKindCount: Int = 0,
    val straightCount: Int = 0,
    val threePairsCount: Int = 0,
    val fullHouseCount: Int = 0,
    val singleOnesCount: Int = 0,
    val singleFivesCount: Int = 0,

    // --- Prise de risque & Psychologie (Lot 4) ---
    val totalPointsBanked: Long = 0L,
    val totalPointsLostToBusts: Long = 0L,
    val worstBustPotLost: Int = 0,
    val oneDieClutchCount: Int = 0,
    val piggybacksSucceededCount: Int = 0,
    val bumpsInflictedCount: Int = 0,
    val bumpsSufferedCount: Int = 0,
    val threeStrikesPenaltiesCount: Int = 0,

    // --- Bilan vs IA par profil (Lot 4) ---
    val winsVsBotSafe: Int = 0,
    val lossesVsBotSafe: Int = 0,
    val winsVsBotBalanced: Int = 0,
    val lossesVsBotBalanced: Int = 0,
    val winsVsBotKamikaze: Int = 0,
    val lossesVsBotKamikaze: Int = 0
) {
    /** Total de tous les brelans obtenus (faces 1 à 6). */
    val totalTriplesCount: Int
        get() = triple1Count + triple2Count + triple3Count + triple4Count + triple5Count + triple6Count

    /** Masque binaire (bits 1..6) des faces pour lesquelles au moins un brelan a été réalisé. */
    val distinctTripleFacesMask: Int
        get() {
            var mask = 0
            if (triple1Count > 0) mask = mask or (1 shl 1)
            if (triple2Count > 0) mask = mask or (1 shl 2)
            if (triple3Count > 0) mask = mask or (1 shl 3)
            if (triple4Count > 0) mask = mask or (1 shl 4)
            if (triple5Count > 0) mask = mask or (1 shl 5)
            if (triple6Count > 0) mask = mask or (1 shl 6)
            return mask
        }

    /** Nombre de faces distinctes (0..6) ayant déjà donné un brelan. */
    val distinctTripleFacesCount: Int
        get() = Integer.bitCount(distinctTripleFacesMask)
}
