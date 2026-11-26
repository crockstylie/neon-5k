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
 * État immuable d'un joueur au cours d'une partie de 5000.
 *
 * @property id Identifiant unique du joueur dans la partie (0..N-1).
 * @property name Pseudo affiché sur le plateau.
 * @property type Humain local, Bot IA ou Joueur distant.
 * @property score Score total actuellement banqué.
 * @property previousScore Score avant le dernier tour banqué (utilisé par la variante Chapeau RESET_TO_PREVIOUS).
 * @property isOpened Vrai dès que le joueur a franchi le seuil d'ouverture (`openingThreshold`).
 * @property consecutiveBusts Nombre de coups nuls (busts/dépassements) consécutifs (utilisé par la pénalité `threeStrikesPenalty`).
 */
data class PlayerState(
    val id: Int,
    val name: String,
    val type: PlayerType = PlayerType.HUMAN,
    val score: Int = 0,
    val previousScore: Int = 0,
    val isOpened: Boolean = false,
    val consecutiveBusts: Int = 0
)
