package fr.antoinehory.neon5k.domain.rules

/**
 * Préréglages rapides de règles du 5000 proposés à l'écran de configuration.
 */
enum class RulesPreset(val labelFr: String) {
    CLASSIC_5_DICE("Classique 5 Dés"),
    ARCADE_FAST("Arcade Rapide"),
    STRATEGIC_PRO("Stratégique Pro"),
    VARIANT_6_DICE("Variante 6 Dés"),
    CUSTOM("Personnalisé")
}

/**
 * Mode de valorisation d'un Carré (4 dés identiques lancés simultanément).
 */
enum class FourOfAKindMode(val labelFr: String) {
    /** Double la valeur du brelan correspondant (ex: 4 × 2 = 400 pts, 4 × 1 = 2000 pts). */
    DOUBLE_TRIPLE("Brelan × 2"),

    /** Non reconnu comme combinaison dédiée (seul le brelan + dé isolé éventuel comptent). */
    DISABLED("Désactivé")
}

/**
 * Mode de valorisation en points d'une Quinte (5 dés identiques lancés simultanément).
 *
 * Note : la victoire instantanée sur 5 As (`instantWinOnFiveOnes`) est désormais un réglage
 * booléen indépendant dans [GameRulesConfig], permettant d'activer `×4 Brelan` ET/OU `5 As = Victoire`.
 */
enum class FiveOfAKindMode(val labelFr: String) {
    /** Quadruple la valeur du brelan correspondant (ex: 5 × 2 = 800 pts, 5 × 1 = 4000 pts). */
    QUAD_TRIPLE("Brelan × 4"),

    /** Quadruple la valeur du brelan, et 5 × « 5 » valent 5 000 pts (variante traditionnelle). */
    FIVE_FIVES_5000("×4 (et 5×5 = 5000)"),

    /** Conservé pour compatibilité : équivaut à `QUAD_TRIPLE` + `instantWinOnFiveOnes = true`. */
    INSTANT_WIN_ON_ONES("5×As = Victoire + ×4"),

    /** Non reconnu comme combinaison dédiée. */
    DISABLED("Désactivée")
}

/**
 * Mode de pénalité en cas d'égalité de score avec un adversaire lors d'une banque ("Chapeau" / "Écrasement").
 */
enum class BumpOpponentMode(val labelFr: String) {
    /** Aucun effet si deux joueurs ont le même score. */
    DISABLED("Désactivé"),

    /** L'adversaire rejoint recule de 500 points (sans passer sous son palier d'ouverture ou 0). */
    MINUS_500("-500 pts à l'adversaire"),

    /** L'adversaire rejoint retombe à son score du tour précédent. */
    RESET_TO_PREVIOUS("Retour au score précédent")
}

/**
 * Mode de pénalité après 3 tours consécutifs sans marquer ("3 Encoches" / "3 Croix").
 *
 * Sur feuille papier, chaque coup nul est noté par une **Encoche** (`✕`) en face du **Palier**
 * de score actuel du joueur. À la 3e encoche sur un même palier, la pénalité choisie s'applique.
 */
enum class ThreeStrikesMode(
    val labelFr: String,
    val descriptionFr: String
) {
    /** Aucune pénalité après 3 coups nuls consécutifs. */
    DISABLED(
        labelFr = "Désactivé",
        descriptionFr = "Aucune pénalité après 3 tours nuls consécutifs."
    ),

    /** Pénalité fixe classique de -500 points (sans descendre sous 0), compteur remis à 0. */
    MINUS_500(
        labelFr = "-500 pts",
        descriptionFr = "3 encoches d'affilée retirent 500 pts au score (minimum 0)."
    ),

    /** Pénalité fixe sévère de -1000 points (sans descendre sous 0), compteur remis à 0. */
    MINUS_1000(
        labelFr = "-1 000 pts",
        descriptionFr = "3 encoches d'affilée retirent 1 000 pts au score (minimum 0)."
    ),

    /**
     * Variante "Chute de Palier (encoches effacées)" :
     * Après 3 tours nuls sur le palier actuel, le joueur recule à son palier de score précédent
     * avec 0 encoche sur ce palier retrouvé.
     */
    REVERT_TO_PREVIOUS_CLEAR_STRIKES(
        labelFr = "Palier préc. (0 ✕)",
        descriptionFr = "3 encoches font reculer au palier de score précédent en effaçant ses anciennes encoches."
    ),

    /**
     * Variante "Chute de Palier avec mémoire des encoches" (règle papier entre potes) :
     * Chaque palier de score conserve son historique d'encoches (`0..2`).
     * Si l'on avait déjà 2 encoches sur `1200`, qu'on monte à `1700`, puis qu'on fait 3 nuls sur `1700`,
     * on retombe à `1200` **avec ses 2 encoches** : un seul nouveau coup nul suffit alors à refaire
     * tomber au palier encore inférieur (`1000`) !
     */
    REVERT_TO_PREVIOUS_PERSIST_STRIKES(
        labelFr = "Palier préc. (mémoire ✕)",
        descriptionFr = "3 encoches font reculer au palier précédent en conservant les encoches déjà accumulées sur ce palier !"
    )
}

/**
 * Configuration complète, immuable et sérialisable des règles d'une partie de 5000.
 *
 * Ce modèle appartient à la couche `domain/` (100% Kotlin pur, sans dépendance Android).
 */
data class GameRulesConfig(
    val preset: RulesPreset = RulesPreset.CLASSIC_5_DICE,
    val diceCount: Int = 5,
    val targetScore: Int = 5000,
    val openingThreshold: Int = 750,
    val minTurnScoreAfterOpening: Int = 0,
    val requireMultipleOf100ToBank: Boolean = false,
    val finalSprintLast1000InOneTurn: Boolean = false,
    val exactFinishRequired: Boolean = true,
    val mandatoryRerollOnHotDice: Boolean = true,
    val fourOfAKindMode: FourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
    val fiveOfAKindMode: FiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
    val instantWinOnFiveOnes: Boolean = false,
    val straightScore: Int = 1500,
    val fullStraight6DiceScore: Int = 2500,
    val threePairsScore: Int = 1000,
    val fullHouseBonus: Int = 0,
    val piggybackEnabled: Boolean = false,
    val bumpOpponentMode: BumpOpponentMode = BumpOpponentMode.DISABLED,
    val threeStrikesMode: ThreeStrikesMode = ThreeStrikesMode.DISABLED
) {
    init {
        require(diceCount in 5..6) { "diceCount must be 5 or 6 (got $diceCount)" }
        require(targetScore >= 1000) { "targetScore must be >= 1000 (got $targetScore)" }
        require(openingThreshold >= 0) { "openingThreshold must be >= 0 (got $openingThreshold)" }
        require(minTurnScoreAfterOpening >= 0) { "minTurnScoreAfterOpening must be >= 0" }
    }

    /** Vrai si la règle « 5 As en un seul lancer = Victoire instantanée » est active. */
    val isInstantWinOnFiveOnesActive: Boolean
        get() = instantWinOnFiveOnes || fiveOfAKindMode == FiveOfAKindMode.INSTANT_WIN_ON_ONES

    /** Compatibilité ascendante avec les anciens appels `threeStrikesPenalty`. */
    val threeStrikesPenalty: Int
        get() = when (threeStrikesMode) {
            ThreeStrikesMode.MINUS_500 -> 500
            ThreeStrikesMode.MINUS_1000 -> 1000
            else -> 0
        }

    /**
     * Empreinte compacte et déterministe de la configuration des règles (pour les logs IA et stats).
     */
    fun toFingerprint(): String = buildString {
        append("d").append(diceCount)
        append("_t").append(targetScore)
        append("_o").append(openingThreshold)
        append("_mto").append(minTurnScoreAfterOpening)
        append("_r100").append(if (requireMultipleOf100ToBank) 1 else 0)
        append("_fs1k").append(if (finalSprintLast1000InOneTurn) 1 else 0)
        append("_ex").append(if (exactFinishRequired) 1 else 0)
        append("_hd").append(if (mandatoryRerollOnHotDice) 1 else 0)
        append("_4k").append(fourOfAKindMode.ordinal)
        append("_5k").append(fiveOfAKindMode.ordinal)
        append("_iw1").append(if (isInstantWinOnFiveOnesActive) 1 else 0)
        append("_st").append(straightScore)
        append("_st6").append(fullStraight6DiceScore)
        append("_3p").append(threePairsScore)
        append("_fh").append(fullHouseBonus)
        append("_pb").append(if (piggybackEnabled) 1 else 0)
        append("_bm").append(bumpOpponentMode.ordinal)
        append("_3s").append(threeStrikesMode.name)
    }

    /** Alias de compatibilité pour [toFingerprint]. */
    fun fingerprint(): String = toFingerprint()

    companion object {
        val CLASSIC_5_DICE = GameRulesConfig(
            preset = RulesPreset.CLASSIC_5_DICE,
            diceCount = 5,
            targetScore = 5000,
            openingThreshold = 750,
            minTurnScoreAfterOpening = 0,
            requireMultipleOf100ToBank = false,
            finalSprintLast1000InOneTurn = false,
            exactFinishRequired = true,
            mandatoryRerollOnHotDice = true,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            instantWinOnFiveOnes = false,
            straightScore = 1500,
            fullStraight6DiceScore = 2500,
            threePairsScore = 0,
            fullHouseBonus = 0,
            piggybackEnabled = false,
            bumpOpponentMode = BumpOpponentMode.DISABLED,
            threeStrikesMode = ThreeStrikesMode.DISABLED
        )

        val ARCADE_FAST = GameRulesConfig(
            preset = RulesPreset.ARCADE_FAST,
            diceCount = 5,
            targetScore = 3000,
            openingThreshold = 500,
            minTurnScoreAfterOpening = 0,
            requireMultipleOf100ToBank = false,
            finalSprintLast1000InOneTurn = false,
            exactFinishRequired = false,
            mandatoryRerollOnHotDice = false,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            instantWinOnFiveOnes = true,
            straightScore = 1500,
            fullStraight6DiceScore = 2500,
            threePairsScore = 0,
            fullHouseBonus = 250,
            piggybackEnabled = true,
            bumpOpponentMode = BumpOpponentMode.MINUS_500,
            threeStrikesMode = ThreeStrikesMode.DISABLED
        )

        val STRATEGIC_PRO = GameRulesConfig(
            preset = RulesPreset.STRATEGIC_PRO,
            diceCount = 5,
            targetScore = 5000,
            openingThreshold = 750,
            minTurnScoreAfterOpening = 0,
            requireMultipleOf100ToBank = true,
            finalSprintLast1000InOneTurn = false,
            exactFinishRequired = true,
            mandatoryRerollOnHotDice = true,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            instantWinOnFiveOnes = false,
            straightScore = 1500,
            fullStraight6DiceScore = 2500,
            threePairsScore = 0,
            fullHouseBonus = 0,
            piggybackEnabled = true,
            bumpOpponentMode = BumpOpponentMode.MINUS_500,
            threeStrikesMode = ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES
        )

        val VARIANT_6_DICE = GameRulesConfig(
            preset = RulesPreset.VARIANT_6_DICE,
            diceCount = 6,
            targetScore = 10000,
            openingThreshold = 1000,
            minTurnScoreAfterOpening = 0,
            requireMultipleOf100ToBank = false,
            finalSprintLast1000InOneTurn = false,
            exactFinishRequired = true,
            mandatoryRerollOnHotDice = false,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            instantWinOnFiveOnes = false,
            straightScore = 1500,
            fullStraight6DiceScore = 2500,
            threePairsScore = 1000,
            fullHouseBonus = 250,
            piggybackEnabled = false,
            bumpOpponentMode = BumpOpponentMode.DISABLED,
            threeStrikesMode = ThreeStrikesMode.MINUS_500
        )

        fun fromPreset(preset: RulesPreset): GameRulesConfig = when (preset) {
            RulesPreset.CLASSIC_5_DICE -> CLASSIC_5_DICE
            RulesPreset.ARCADE_FAST -> ARCADE_FAST
            RulesPreset.STRATEGIC_PRO -> STRATEGIC_PRO
            RulesPreset.VARIANT_6_DICE -> VARIANT_6_DICE
            RulesPreset.CUSTOM -> CLASSIC_5_DICE.copy(preset = RulesPreset.CUSTOM)
        }
    }
}
