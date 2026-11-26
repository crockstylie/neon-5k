package fr.antoinehory.neon5k.domain.achievement

/**
 * Rangs de maîtrise locaux attribués grâce au comptage continu post-déverrouillage (ADR-011).
 */
enum class MasteryRank(val labelFr: String) {
    LOCKED("VERROUILLÉ"),
    BRONZE("BRONZE"),
    SILVER("ARGENT"),
    GOLD("OR"),
    CYBER_PLATINUM("CYBER-PLATINE")
}

/**
 * Catalogue officiel des 20 succès de Neon 5K (totalisant exactement 1 000 XP sur Google Play Jeux).
 * Chaque succès embarque sa matrice Pixel Art `16×16` (`'0'` = vide, `'1'` = primaire cyan, `'2'` = secondaire magenta, `'3'` = alerte jaune)
 * utilisée à la fois pour le rendu Compose `Canvas` in-app et pour l'export des 20 icônes PNG `512×512` destinées à la Google Play Console.
 */
enum class AchievementId(
    val titleFr: String,
    val titleEn: String,
    val descriptionFr: String,
    val descriptionEn: String,
    val targetSteps: Int,
    val playGamesXp: Int,
    val silverThreshold: Int,
    val goldThreshold: Int,
    val platinumThreshold: Int,
    val iconMatrix16x16: List<String>
) {
    FIRST_BOOT(
        titleFr = "First Boot",
        titleEn = "First Boot",
        descriptionFr = "Franchir le seuil d'ouverture pour la première fois.",
        descriptionEn = "Pass the opening score threshold for the first time.",
        targetSteps = 1,
        playGamesXp = 25,
        silverThreshold = 10,
        goldThreshold = 50,
        platinumThreshold = 150,
        iconMatrix16x16 = listOf(
            "0000001111000000",
            "0000001331000000",
            "0000111331110000",
            "0001101331011000",
            "0011001331001100",
            "0110001331000110",
            "0110001331000110",
            "0110001111000110",
            "0110000000000110",
            "0110000220000110",
            "0011002222001100",
            "0001100220011000",
            "0000111111110000",
            "0000011111100000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    HIGH_VOLTAGE_OPENING(
        titleFr = "High Voltage Opening",
        titleEn = "High Voltage Opening",
        descriptionFr = "Réussir son ouverture avec au moins 1 500 points en un seul tour.",
        descriptionEn = "Open your score with at least 1,500 points in a single turn.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000000333300000",
            "0000003333000000",
            "0000033330000000",
            "0000333300000000",
            "0003333333330000",
            "0003333333330000",
            "0000000333300000",
            "0000003333000000",
            "0000033330000000",
            "0000333333330000",
            "0000000333300000",
            "0000003330000000",
            "0000033300000000",
            "0000330000000000",
            "0003000000000000",
            "0000000000000000"
        )
    ),

    TRIPLE_ACE_PROTOCOL(
        titleFr = "Triple Ace Protocol",
        titleEn = "Triple Ace Protocol",
        descriptionFr = "Obtenir un Brelan d'As (1 000 pts) en un seul lancer.",
        descriptionEn = "Roll Three Ones (1,000 pts) in a single roll.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 25,
        platinumThreshold = 100,
        iconMatrix16x16 = listOf(
            "0111110000111110",
            "0100010000100010",
            "0103010000103010",
            "0100010000100010",
            "0111110000111110",
            "0000000000000000",
            "0000011111100000",
            "0000010000100000",
            "0000010220100000",
            "0000010220100000",
            "0000010000100000",
            "0000011111100000",
            "0000000000000000",
            "0011111111111100",
            "0000033333300000",
            "0000000000000000"
        )
    ),

    BRELAN_COLLECTOR(
        titleFr = "Brelan Collector",
        titleEn = "Triple Collector",
        descriptionFr = "Réaliser au moins un brelan de chacune des 6 faces (1 à 6).",
        descriptionEn = "Roll a three-of-a-kind for each of the 6 die faces (1 to 6).",
        targetSteps = 6,
        playGamesXp = 50,
        silverThreshold = 25,
        goldThreshold = 100,
        platinumThreshold = 300,
        iconMatrix16x16 = listOf(
            "0111101111011110",
            "0130101221013310",
            "0100101001010310",
            "0111101111011110",
            "0000000000000000",
            "0111101111011110",
            "0120101301012210",
            "0102101031012210",
            "0111101111011110",
            "0000000000000000",
            "0011111111111100",
            "0013333333333100",
            "0011111111111100",
            "0000002222000000",
            "0000222222220000",
            "0000000000000000"
        )
    ),

    QUAD_CORE(
        titleFr = "Quad Core",
        titleEn = "Quad Core",
        descriptionFr = "Obtenir un Carré (4 dés identiques) en un seul lancer.",
        descriptionEn = "Roll a Four-of-a-Kind in a single roll.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0111111001111110",
            "0100001001000010",
            "0103301001033010",
            "0103301001033010",
            "0100001001000010",
            "0111111001111110",
            "0000000220000000",
            "0000002222000000",
            "0111111001111110",
            "0100001001000010",
            "0103301001033010",
            "0103301001033010",
            "0100001001000010",
            "0111111001111110",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    FIVE_OF_A_KIND(
        titleFr = "God Roll (Quinte)",
        titleEn = "God Roll (Five of a Kind)",
        descriptionFr = "Obtenir une Quinte (5 dés identiques) en un seul lancer.",
        descriptionEn = "Roll a Five-of-a-Kind in a single roll.",
        targetSteps = 1,
        playGamesXp = 75,
        silverThreshold = 3,
        goldThreshold = 10,
        platinumThreshold = 30,
        iconMatrix16x16 = listOf(
            "0030000330000300",
            "0033003333003300",
            "0033333223333300",
            "0033332222333300",
            "0033111111113300",
            "0001113333111000",
            "0011330000331100",
            "0011300220031100",
            "0011302222031100",
            "0011300220031100",
            "0011330000331100",
            "0001113333111000",
            "0000111111110000",
            "0002222222222000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    STRAIGHT_RUNNER(
        titleFr = "Straight Runner",
        titleEn = "Straight Runner",
        descriptionFr = "Réussir une Suite (1-2-3-4-5 ou 2-3-4-5-6) en un seul lancer.",
        descriptionEn = "Roll a Straight (1-2-3-4-5 or 2-3-4-5-6) in a single roll.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 25,
        platinumThreshold = 100,
        iconMatrix16x16 = listOf(
            "0000000000011110",
            "0000000000013310",
            "0000000011111110",
            "0000000012210000",
            "0000011111110000",
            "0000013310000000",
            "0011111110000000",
            "0012210000000000",
            "1111110000000000",
            "1331000000220000",
            "1111000000222000",
            "0000001111222200",
            "0000001111222000",
            "0000000000220000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    TWIN_MATRIX(
        titleFr = "Twin Matrix",
        titleEn = "Twin Matrix",
        descriptionFr = "Obtenir 3 Paires distinctes en un lancer (variante 6 dés).",
        descriptionEn = "Roll 3 distinct Pairs in a single roll (6-dice variant).",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0111100000011110",
            "0133102222013310",
            "0111100000011110",
            "0000000000000000",
            "0222200000022220",
            "0211201111021120",
            "0222200000022220",
            "0000000000000000",
            "0333300000033330",
            "0322301111032230",
            "0333300000033330",
            "0000000000000000",
            "0011111111111100",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    FULL_HOUSE_CYBER(
        titleFr = "Full House Cyber",
        titleEn = "Full House Cyber",
        descriptionFr = "Réussir un Full (Brelan + Paire) avec le bonus Full activé.",
        descriptionEn = "Roll a Full House (Triple + Pair) with the Full House bonus active.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000000110000000",
            "0000001111000000",
            "0000011331100000",
            "0000113333110000",
            "0001133223311000",
            "0011111111111100",
            "0001100000011000",
            "0001102222011000",
            "0001102002011000",
            "0001102002011000",
            "0001102222011000",
            "0001100000011000",
            "0001111111111000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    HOT_DICE(
        titleFr = "Hot Dice!",
        titleEn = "Hot Dice!",
        descriptionFr = "Réaliser une Main pleine en faisant scorer tous les dés.",
        descriptionEn = "Score with all dice to trigger Hot Dice.",
        targetSteps = 1,
        playGamesXp = 25,
        silverThreshold = 10,
        goldThreshold = 50,
        platinumThreshold = 200,
        iconMatrix16x16 = listOf(
            "0000000200000000",
            "0000002220000000",
            "0000022320200000",
            "0000223322220000",
            "0002233332222000",
            "0022333133322000",
            "0022331113322200",
            "0223311311332200",
            "0223313331332200",
            "0223311311332200",
            "0022331113322000",
            "0002233333220000",
            "0000222222200000",
            "0000001110000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    FULL_OVERCLOCK(
        titleFr = "Full Overclock",
        titleEn = "Full Overclock",
        descriptionFr = "Enchaîner 2 Mains pleines au cours du même tour et banquer.",
        descriptionEn = "Chain 2 Hot Dice in a single turn and bank.",
        targetSteps = 1,
        playGamesXp = 75,
        silverThreshold = 3,
        goldThreshold = 12,
        platinumThreshold = 40,
        iconMatrix16x16 = listOf(
            "0002000000020000",
            "0022200000222000",
            "0223220002232200",
            "0233320002333200",
            "0231320302313200",
            "0023203330232000",
            "0002000300020000",
            "0000000000000000",
            "0011111111111100",
            "0113333333333110",
            "0133222222223310",
            "0113333333333110",
            "0011111111111100",
            "0000033003300000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    ONE_DIE_MIRACLE(
        titleFr = "One Die Miraculé",
        titleEn = "One Die Miracle",
        descriptionFr = "Scorer un As ou un 5 en ne relançant qu'un seul dé restant.",
        descriptionEn = "Score a 1 or a 5 when rolling only 1 remaining free die.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 10,
        goldThreshold = 40,
        platinumThreshold = 120,
        iconMatrix16x16 = listOf(
            "0000000330000000",
            "0003000330003000",
            "0000300000030000",
            "0000000000000000",
            "0000111111110000",
            "0000100000010000",
            "0000100000010000",
            "0330100220010330",
            "0330100220010330",
            "0000100000010000",
            "0000100000010000",
            "0000111111110000",
            "0000000000000000",
            "0000300000030000",
            "0003000330003000",
            "0000000330000000"
        )
    ),

    SYSTEM_CRASH(
        titleFr = "System Crash",
        titleEn = "System Crash",
        descriptionFr = "Perdre un pot de tour d'au moins 1 000 points sur un Glitch.",
        descriptionEn = "Lose a turn pot of at least 1,000 points on a Bust.",
        targetSteps = 1,
        playGamesXp = 25,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 60,
        iconMatrix16x16 = listOf(
            "0000222222220000",
            "0002200000022000",
            "0022000000002200",
            "0020033003300200",
            "0020033003300200",
            "0020000000000200",
            "0020000110000200",
            "0022000110002200",
            "0002200000022000",
            "0000202020220000",
            "0000202020200000",
            "0000000000000000",
            "0022000000002200",
            "0000222002220000",
            "0022000220002200",
            "0000000000000000"
        )
    ),

    THREE_STRIKES_OUT(
        titleFr = "Three Strikes Out (✕✕✕)",
        titleEn = "Three Strikes Out",
        descriptionFr = "Subir la pénalité ou le recul de palier après 3 Encoches (✕✕✕).",
        descriptionEn = "Trigger the 3-Strikes penalty or checkpoint rollback.",
        targetSteps = 1,
        playGamesXp = 25,
        silverThreshold = 5,
        goldThreshold = 15,
        platinumThreshold = 50,
        iconMatrix16x16 = listOf(
            "0200202002020020",
            "0022000220002200",
            "0022000220002200",
            "0200202002020020",
            "0000000000000000",
            "0111111111111110",
            "0000000330000000",
            "0000000330000000",
            "0000000330000000",
            "0000333333330000",
            "0000033333300000",
            "0000003333000000",
            "0000000330000000",
            "0111111111111110",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    PIXEL_PERFECT_5000(
        titleFr = "Pixel Perfect 5000",
        titleEn = "Pixel Perfect 5000",
        descriptionFr = "Remporter une partie en tombant pile sur le score cible exact.",
        descriptionEn = "Win a match by landing on the exact target score.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 25,
        platinumThreshold = 100,
        iconMatrix16x16 = listOf(
            "0000001111000000",
            "0000110000110000",
            "0001000110001000",
            "0010011001100100",
            "0100100000010010",
            "0101000220001010",
            "1001002332001001",
            "1001002332001001",
            "0101000220001010",
            "0100100000010010",
            "0010011001100100",
            "0001000110001000",
            "0000110000110000",
            "0000001111000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    OVERKILL_TURN(
        titleFr = "Overkill Turn",
        titleEn = "Overkill Turn",
        descriptionFr = "Banquer au moins 2 000 points en un seul tour.",
        descriptionEn = "Bank at least 2,000 points in a single turn.",
        targetSteps = 1,
        playGamesXp = 75,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000000110000000",
            "0000001331000000",
            "0000013333100000",
            "0000013223100000",
            "0000133223310000",
            "0000133333310000",
            "0000133113310000",
            "0001133113311000",
            "0011133333311100",
            "0111111111111110",
            "0110002332000110",
            "0000022332200000",
            "0000020330200000",
            "0000000330000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    HIJACKER(
        titleFr = "Hijacker (Repiquage)",
        titleEn = "Hijacker (Piggyback)",
        descriptionFr = "Reprendre le pot d'un adversaire en Repiquage et banquer le tour.",
        descriptionEn = "Piggyback an opponent's pot and successfully bank the turn.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000011111000000",
            "0000110001100000",
            "0000100000100000",
            "0000000000100000",
            "0000000001100000",
            "0000000111000000",
            "0000000110000000",
            "0000000110000000",
            "0002222332222000",
            "0022000330002200",
            "0020011331100200",
            "0020013333100200",
            "0022001331002200",
            "0002222222222000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    HEADSHOT(
        titleFr = "Headshot (Chapeau)",
        titleEn = "Headshot (Bump)",
        descriptionFr = "Infliger un Chapeau en égalisant exactement le score d'un adversaire.",
        descriptionEn = "Bump an opponent by matching their exact score.",
        targetSteps = 1,
        playGamesXp = 50,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000111111110000",
            "0001133333311000",
            "0001333333331000",
            "0001332222331000",
            "0111111111111110",
            "0111111111111110",
            "0000000220000000",
            "0000000220000000",
            "0000022222200000",
            "0000002222000000",
            "0000000220000000",
            "0001111111111000",
            "0001000330001000",
            "0001111111111000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    BEAT_THE_MACHINE(
        titleFr = "Beat the Machine",
        titleEn = "Beat the Machine",
        descriptionFr = "Remporter une partie face au bot Overclock Kamikaze.",
        descriptionEn = "Win a match against the Overclock Kamikaze AI bot.",
        targetSteps = 1,
        playGamesXp = 75,
        silverThreshold = 5,
        goldThreshold = 20,
        platinumThreshold = 75,
        iconMatrix16x16 = listOf(
            "0000000220000000",
            "0000000220000000",
            "0001111111111000",
            "0011000000001100",
            "0110033003300110",
            "0110033003300110",
            "0110000000000110",
            "0110022222200110",
            "0110202002020110",
            "0011000000001100",
            "0001111111111000",
            "0000011001100000",
            "0001111111111000",
            "0000000000000000",
            "0000000000000000",
            "0000000000000000"
        )
    ),

    ENTROPY_MASTER(
        titleFr = "Entropy Master",
        titleEn = "Entropy Master",
        descriptionFr = "Terminer 50 parties complètes propulsées par l'entropie SHA-256.",
        descriptionEn = "Complete 50 full matches powered by SHA-256 hardware entropy.",
        targetSteps = 50,
        playGamesXp = 50,
        silverThreshold = 100,
        goldThreshold = 250,
        platinumThreshold = 500,
        iconMatrix16x16 = listOf(
            "0010100110010100",
            "0010101331010100",
            "0111111331111110",
            "0010000000000100",
            "1110111111110111",
            "0010100000010100",
            "1110100220010111",
            "0010100220010100",
            "1110100000010111",
            "0010111111110100",
            "1110000000000111",
            "0111111331111110",
            "0010101331010100",
            "0010100110010100",
            "0000000000000000",
            "0000000000000000"
        )
    );

    /**
     * Calcule le rang de maîtrise local d'un succès selon son état de déverrouillage et son compteur infini (`lifetimeCount`).
     */
    fun computeMasteryRank(isUnlocked: Boolean, lifetimeCount: Int): MasteryRank {
        if (!isUnlocked) return MasteryRank.LOCKED
        return when {
            lifetimeCount >= platinumThreshold -> MasteryRank.CYBER_PLATINUM
            lifetimeCount >= goldThreshold -> MasteryRank.GOLD
            lifetimeCount >= silverThreshold -> MasteryRank.SILVER
            else -> MasteryRank.BRONZE
        }
    }
}

/**
 * État immuable de progression d'un succès (avec compteur infini post-déverrouillage).
 */
data class AchievementProgress(
    val id: AchievementId,
    val isUnlocked: Boolean = false,
    val progressSteps: Int = 0,
    val lifetimeCount: Int = 0,
    val unlockedAtEpochMs: Long? = null
) {
    val masteryRank: MasteryRank
        get() = id.computeMasteryRank(isUnlocked, lifetimeCount)
}

/**
 * Notification émise lorsqu'un succès vient d'être déverrouillé pour la première fois
 * ou vient de passer un palier de maîtrise supérieur (*Argent*, *Or*, *Cyber-Platine*).
 */
data class AchievementUnlockNotification(
    val achievementId: AchievementId,
    val newRank: MasteryRank,
    val lifetimeCount: Int,
    val isFirstUnlock: Boolean
)
