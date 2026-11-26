package fr.antoinehory.neon5k.domain.ai

import fr.antoinehory.neon5k.domain.model.MatchState
import fr.antoinehory.neon5k.domain.model.PlayerType

/**
 * Profil de personnalité d'une IA locale au 5000.
 */
enum class BotProfile(
    val playerType: PlayerType,
    val displayNameFr: String,
    val descriptionFr: String
) {
    SAFE_RUNNER(
        playerType = PlayerType.BOT_SAFE,
        displayNameFr = "Bot Safe Runner",
        descriptionFr = "Prudent : sécurise rapidement ses points dès que le risque de glitch dépasse 28 %."
    ),
    CYBER_BALANCED(
        playerType = PlayerType.BOT_BALANCED,
        displayNameFr = "Bot Cyber Balanced",
        descriptionFr = "Équilibré : optimise l'espérance mathématique nette (EV) et s'adapte à l'écart de score."
    ),
    OVERCLOCK_KAMIKAZE(
        playerType = PlayerType.BOT_KAMIKAZE,
        displayNameFr = "Bot Overclock Kamikaze",
        descriptionFr = "Agressif : chasse les mains pleines, repique les pots risqués et pousse jusqu'aux gros scores."
    );

    companion object {
        fun fromPlayerType(type: PlayerType): BotProfile? = entries.firstOrNull { it.playerType == type }
    }
}

/**
 * Décision finale d'un bot à l'issue de sa sélection de dés ou au début de son tour.
 */
enum class BotTurnAction {
    ROLL_AGAIN,
    BANK
}

/**
 * Plan de coup complet choisi par un bot sur un lancer en phase `SELECTING_DICE`.
 *
 * @property indicesToSelect Indices des dés (`0 until diceCount`) que le bot choisit de verrouiller sur ce lancer.
 * @property nextAction Action à exécuter après avoir sélectionné ces dés (`ROLL_AGAIN` ou `BANK`).
 */
data class BotMovePlan(
    val indicesToSelect: List<Int>,
    val nextAction: BotTurnAction
)

/**
 * Contrat pur Kotlin d'une stratégie d'IA pour le 5000.
 */
interface BotPolicy {
    /**
     * Vrai si le bot choisit d'accepter l'offre de Repiquage (`piggybackOffer`) au début de son tour.
     */
    fun shouldAcceptPiggyback(state: MatchState, profile: BotProfile): Boolean

    /**
     * Choisit quels dés scorants garder sur le lancer courant et s'il faut relancer ou banquer.
     */
    fun planMove(state: MatchState, profile: BotProfile): BotMovePlan
}
