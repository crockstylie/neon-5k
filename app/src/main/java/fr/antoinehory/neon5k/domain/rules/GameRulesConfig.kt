package fr.antoinehory.neon5k.domain.rules

/**
 * Préréglages rapides des règles du jeu de dés 5000.
 */
enum class RulesPreset(val labelFr: String) {
    CLASSIC_5_DICE("Classique 5 dés"),
    VARIANT_6_DICE("Variante 6 dés"),
    QUICK_2500("Rapide 2 500"),
    CUSTOM("Personnalisé")
}

/**
 * Mode de valorisation du Carré (4 dés identiques en un seul lancer).
 */
enum class FourOfAKindMode {
    /** Désactivé : un carré est compté comme un brelan (+ 1 dé individuel s'il s'agit d'un 1 ou d'un 5). */
    DISABLED,
    /** Double la valeur du brelan correspondant (ex: quatre 2 = 400 pts, quatre As = 2 000 pts). */
    DOUBLE_TRIPLE
}

/**
 * Mode de valorisation de la Quinte (5 dés identiques en un seul lancer).
 */
enum class FiveOfAKindMode {
    /** Désactivé : compté selon le mode inférieur (carré ou brelan). */
    DISABLED,
    /** Quadruple la valeur du brelan correspondant (ex: cinq 2 = 800 pts, cinq As = 4 000 pts). */
    QUAD_TRIPLE,
    /** Quadruple les brelans ordinaires (2..6), mais 5 As en un seul lancer donnent la victoire instantanée. */
    INSTANT_WIN_ON_ONES
}

/**
 * Mode de la variante « Chapeau / Écrasement » lorsqu'un joueur atteint exactement le score d'un adversaire.
 */
enum class BumpOpponentMode {
    /** Aucun effet lorsqu'un joueur égalise le score d'un adversaire. */
    DISABLED,
    /** L'adversaire rejoint recule de 500 points (sans descendre sous 0). */
    MINUS_500,
    /** L'adversaire rejoint retombe à son score d'avant son dernier tour banqué (ou 0). */
    RESET_TO_PREVIOUS
}

/**
 * Configuration immuable complète d'une partie de 5000.
 * Pur Kotlin (aucune dépendance Android).
 */
data class GameRulesConfig(
    val preset: RulesPreset = RulesPreset.CLASSIC_5_DICE,
    val diceCount: Int = 5,
    val targetScore: Int = 5000,
    val openingThreshold: Int = 750,
    val exactFinishRequired: Boolean = true,
    val mandatoryRerollOnHotDice: Boolean = true,
    val fourOfAKindMode: FourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
    val fiveOfAKindMode: FiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
    val straightScore: Int = 1500,
    val threePairsScore: Int = 1000,
    val fullHouseBonus: Int = 0,
    val piggybackEnabled: Boolean = false,
    val bumpOpponentMode: BumpOpponentMode = BumpOpponentMode.DISABLED,
    val threeStrikesPenalty: Int = 0
) {
    init {
        require(diceCount in 5..6) { "diceCount must be 5 or 6 (was $diceCount)" }
        require(targetScore > 0) { "targetScore must be positive (was $targetScore)" }
        require(openingThreshold >= 0) { "openingThreshold must be >= 0 (was $openingThreshold)" }
        require(straightScore >= 0) { "straightScore must be >= 0" }
        require(threePairsScore >= 0) { "threePairsScore must be >= 0" }
        require(fullHouseBonus >= 0) { "fullHouseBonus must be >= 0" }
        require(threeStrikesPenalty >= 0) { "threeStrikesPenalty must be >= 0" }
    }

    /**
     * Empreinte compacte et déterministe des règles actives (utilisée pour taguer les logs IA et salons réseau).
     */
    fun fingerprint(): String = buildString {
        append("D").append(diceCount)
        append("_T").append(targetScore)
        append("_O").append(openingThreshold)
        append("_EX").append(if (exactFinishRequired) 1 else 0)
        append("_HD").append(if (mandatoryRerollOnHotDice) 1 else 0)
        append("_4K").append(fourOfAKindMode.ordinal)
        append("_5K").append(fiveOfAKindMode.ordinal)
        append("_ST").append(straightScore)
        append("_3P").append(if (diceCount == 6) threePairsScore else 0)
        append("_FH").append(fullHouseBonus)
        append("_PB").append(if (piggybackEnabled) 1 else 0)
        append("_BP").append(bumpOpponentMode.ordinal)
        append("_3S").append(threeStrikesPenalty)
    }

    companion object {
        /** Preset par défaut : Classique 5 dés (cible 5 000, ouverture 750, score exact, main pleine obligatoire). */
        val CLASSIC_5_DICE = GameRulesConfig(
            preset = RulesPreset.CLASSIC_5_DICE,
            diceCount = 5,
            targetScore = 5000,
            openingThreshold = 750,
            exactFinishRequired = true,
            mandatoryRerollOnHotDice = true,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            straightScore = 1500,
            threePairsScore = 0,
            fullHouseBonus = 0,
            piggybackEnabled = false,
            bumpOpponentMode = BumpOpponentMode.DISABLED,
            threeStrikesPenalty = 0
        )

        /** Preset Variante 6 dés (cible 5 000, ouverture 750, 3 paires = 1 000 pts, full +200 pts). */
        val VARIANT_6_DICE = GameRulesConfig(
            preset = RulesPreset.VARIANT_6_DICE,
            diceCount = 6,
            targetScore = 5000,
            openingThreshold = 750,
            exactFinishRequired = true,
            mandatoryRerollOnHotDice = true,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            straightScore = 1500,
            threePairsScore = 1000,
            fullHouseBonus = 200,
            piggybackEnabled = false,
            bumpOpponentMode = BumpOpponentMode.DISABLED,
            threeStrikesPenalty = 0
        )

        /** Preset Rapide 2 500 (5 dés, cible 2 500, ouverture 500, sans score exact obligatoire). */
        val QUICK_2500 = GameRulesConfig(
            preset = RulesPreset.QUICK_2500,
            diceCount = 5,
            targetScore = 2500,
            openingThreshold = 500,
            exactFinishRequired = false,
            mandatoryRerollOnHotDice = false,
            fourOfAKindMode = FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = FiveOfAKindMode.QUAD_TRIPLE,
            straightScore = 1500,
            threePairsScore = 0,
            fullHouseBonus = 0,
            piggybackEnabled = false,
            bumpOpponentMode = BumpOpponentMode.DISABLED,
            threeStrikesPenalty = 0
        )

        fun fromPreset(preset: RulesPreset): GameRulesConfig = when (preset) {
            RulesPreset.CLASSIC_5_DICE -> CLASSIC_5_DICE
            RulesPreset.VARIANT_6_DICE -> VARIANT_6_DICE
            RulesPreset.QUICK_2500 -> QUICK_2500
            RulesPreset.CUSTOM -> CLASSIC_5_DICE.copy(preset = RulesPreset.CUSTOM)
        }
    }
}
