package fr.antoinehory.neon5k.domain.model

/**
 * Type d'acteur contrôlant un joueur dans la partie.
 */
enum class PlayerType {
    HUMAN,
    BOT_SAFE,
    BOT_BALANCED,
    BOT_KAMIKAZE,
    REMOTE
}

/**
 * Palier de score sur la feuille de match d'un joueur (Score Checkpoint), mémorisant
 * le score atteint et le nombre d'encoches (`strikes` / `✕` : `0..2`) accumulées sur cette ligne.
 *
 * Permet de gérer fidèlement la variante « Recul au palier précédent avec mémoire des encoches » :
 * ex. `1000 (0✕)` -> `1200 (2✕)` -> `1700 (3✕ => retombe à 1200 avec ses 2✕)` -> `1 nouveau nul => 3✕ sur 1200 => retombe à 1000 (0✕)`.
 */
data class ScoreCheckpoint(
    val score: Int,
    val strikes: Int = 0
)

/**
 * État immuable d'un joueur au cours d'une partie de 5000.
 *
 * @property id Identifiant unique du joueur dans la partie (`0..N-1`).
 * @property name Pseudo affiché sur le plateau.
 * @property type Humain local, Bot IA ou Joueur distant.
 * @property score Score total actuellement banqué (correspond au sommet de [checkpoints] s'il est non vide).
 * @property previousScore Score du palier immédiatement inférieur dans la pile (ou `0` s'il n'y a pas de palier inférieur).
 * @property isOpened Vrai dès que le joueur a franchi le seuil d'ouverture (`openingThreshold`).
 * @property consecutiveBusts Nombre d'encoches (`✕` / Strikes : `0..2`) actuellement actives sur le palier courant.
 * @property checkpoints Pile complète des paliers de score validés par le joueur avec leurs encoches respectives.
 */
data class PlayerState(
    val id: Int,
    val name: String,
    val type: PlayerType = PlayerType.HUMAN,
    val score: Int = 0,
    val previousScore: Int = 0,
    val isOpened: Boolean = false,
    val consecutiveBusts: Int = 0,
    val checkpoints: List<ScoreCheckpoint> = emptyList()
) {
    /**
     * Retourne la pile des paliers normalisée (garantit la compatibilité ascendante si `checkpoints` n'a pas été fourni explicitement dans un test).
     */
    fun normalizedCheckpoints(): List<ScoreCheckpoint> {
        if (checkpoints.isNotEmpty()) return checkpoints
        return buildList {
            if (previousScore > 0 && previousScore != score) {
                add(ScoreCheckpoint(score = previousScore, strikes = 0))
            }
            if (score > 0 || consecutiveBusts > 0) {
                add(ScoreCheckpoint(score = score, strikes = consecutiveBusts))
            }
        }
    }
}
