package fr.antoinehory.neon5k.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.domain.model.PlayerState
import fr.antoinehory.neon5k.domain.model.PlayerType
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.FiveOfAKindMode
import fr.antoinehory.neon5k.domain.rules.FourOfAKindMode
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.RulesPreset
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.NeonSubScreenTopBar
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelIconId
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

/**
 * Écran de configuration complète d'une partie de 5000 :
 * - Sélection des joueurs (2 à 8 joueurs : Humains Pass & Play ou Bots IA avec 3 profils).
 * - Préréglages rapides (`Classique 5 Dés`, `Arcade Rapide`, `Stratégique Pro`, `Variante 6 Dés`, `Personnalisé`).
 * - Personnalisation fine de toutes les variantes du 5000 :
 *   - Nombre de dés (5 ou 6), Score cible, Seuil d'ouverture, Minimum par tour après ouverture,
 *   - Chiffre rond obligatoire pour banquer (multiple de 100), Sprint final 1 000 derniers points en 1 tour,
 *   - Score exact requis, Relance obligatoire sur Main pleine,
 *   - Carré (`×2 Brelan`), Quinte (`×4 Brelan` ou `5×5 = 5000`),
 *   - 5 As = Victoire instantanée (réglage indépendant de la Quinte `×4`),
 *   - Suite 5 dés (`1-5` / `2-6`) ET Grande Suite 6 dés (`1-2-3-4-5-6`) configurables séparément,
 *   - 3 Paires (6 dés), Bonus Full, Repiquage, Chapeau et 3 Encoches (`✕✕✕`) avec mémoire des paliers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    initialRules: GameRulesConfig,
    onBack: () -> Unit,
    onLaunchMatch: (GameRulesConfig, List<PlayerState>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    var rules by remember(initialRules) { mutableStateOf(initialRules) }

    val players = remember {
        mutableStateListOf(
            PlayerState(id = 0, name = "JOUEUR 1", type = PlayerType.HUMAN),
            PlayerState(id = 1, name = "CYBER-BOT", type = PlayerType.BOT_BALANCED)
        )
    }

    fun updateRulesCustom(transform: (GameRulesConfig) -> GameRulesConfig) {
        rules = transform(rules).copy(preset = RulesPreset.CUSTOM)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barre supérieure harmonisée
        NeonSubScreenTopBar(
            title = "CONFIGURER LA PARTIE",
            icon = PixelIconId.DICE,
            onBack = onBack
        )

        // Section 1 : Joueurs (2 à 8 joueurs)
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JOUEURS (${players.size}/8)",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PixelButton(
                            text = "- JOUEUR",
                            enabled = players.size > 1,
                            filled = false,
                            accentColor = colors.alertNeon,
                            onClick = {
                                if (players.size > 1) {
                                    players.removeAt(players.lastIndex)
                                }
                            }
                        )
                        PixelButton(
                            text = "+ JOUEUR",
                            enabled = players.size < 8,
                            filled = false,
                            accentColor = colors.primaryNeon,
                            onClick = {
                                if (players.size < 8) {
                                    val nextId = players.size
                                    players.add(
                                        PlayerState(
                                            id = nextId,
                                            name = "BOT #${nextId}",
                                            type = PlayerType.BOT_BALANCED
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                players.forEachIndexed { index, player ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "#${index + 1} • ${player.name}",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = playerTypeDescription(player.type),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        PixelButton(
                            text = playerTypeButtonLabel(player.type),
                            filled = player.type != PlayerType.HUMAN,
                            accentColor = if (player.type == PlayerType.HUMAN) colors.primaryNeon else colors.secondaryNeon,
                            onClick = {
                                val nextType = cyclePlayerType(player.type)
                                val nextName = defaultNameForType(index, nextType)
                                players[index] = player.copy(name = nextName, type = nextType)
                            }
                        )
                    }
                }
            }
        }

        // Section 2 : Préréglages de règles
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "PRÉRÉGLAGES DE RÈGLES",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        RulesPreset.CLASSIC_5_DICE,
                        RulesPreset.ARCADE_FAST,
                        RulesPreset.STRATEGIC_PRO,
                        RulesPreset.VARIANT_6_DICE
                    ).forEach { preset ->
                        val isSelected = rules.preset == preset
                        PixelButton(
                            text = preset.labelFr,
                            filled = isSelected,
                            accentColor = if (isSelected) colors.secondaryNeon else colors.primaryNeon,
                            onClick = { rules = GameRulesConfig.fromPreset(preset) }
                        )
                    }
                }
                if (rules.preset == RulesPreset.CUSTOM) {
                    Text(
                        text = "Mode Personnalisé actif — vos réglages seront sauvegardés comme favoris.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryNeon
                    )
                }
            }
        }

        // Section 3 : Objectif, Ouverture & Conditions de Banque
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "OBJECTIF, OUVERTURE & CONDITIONS DE BANQUE",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )

                OptionSelectorRow(
                    label = "Nombre de dés",
                    options = listOf(5 to "5 Dés", 6 to "6 Dés"),
                    selectedValue = rules.diceCount,
                    onSelect = { count -> updateRulesCustom { it.copy(diceCount = count) } }
                )

                OptionSelectorRow(
                    label = "Score cible",
                    options = listOf(
                        3000 to "3 000",
                        5000 to "5 000",
                        10000 to "10 000"
                    ),
                    selectedValue = rules.targetScore,
                    onSelect = { target -> updateRulesCustom { it.copy(targetScore = target) } }
                )

                OptionSelectorRow(
                    label = "Seuil d'ouverture (1re banque)",
                    options = listOf(
                        0 to "0 (Libre)",
                        500 to "500",
                        750 to "750",
                        1000 to "1 000"
                    ),
                    selectedValue = rules.openingThreshold,
                    onSelect = { opening -> updateRulesCustom { it.copy(openingThreshold = opening) } }
                )

                OptionSelectorRow(
                    label = "Score minimum par tour (après ouverture)",
                    options = listOf(
                        0 to "0 (Libre)",
                        200 to "200 pts",
                        250 to "250 pts",
                        500 to "500 pts"
                    ),
                    selectedValue = rules.minTurnScoreAfterOpening,
                    onSelect = { minTurn -> updateRulesCustom { it.copy(minTurnScoreAfterOpening = minTurn) } }
                )

                OptionSelectorRow(
                    label = "Chiffre rond requis pour banquer (multiple de 100, pas de 5 isolé)",
                    options = listOf(
                        false to "Désactivé (+50 autorisé)",
                        true to "Chiffre rond ×100 requis"
                    ),
                    selectedValue = rules.requireMultipleOf100ToBank,
                    onSelect = { roundReq -> updateRulesCustom { it.copy(requireMultipleOf100ToBank = roundReq) } }
                )

                OptionSelectorRow(
                    label = "Sprint final (franchir les 1 000 derniers points en un seul tour)",
                    options = listOf(
                        false to "Désactivé",
                        true to "1 000 derniers pts en 1 tour"
                    ),
                    selectedValue = rules.finalSprintLast1000InOneTurn,
                    onSelect = { sprint -> updateRulesCustom { it.copy(finalSprintLast1000InOneTurn = sprint) } }
                )

                OptionSelectorRow(
                    label = "Arrivée sur le score cible",
                    options = listOf(
                        true to "Score exact requis",
                        false to "Dépassement autorisé"
                    ),
                    selectedValue = rules.exactFinishRequired,
                    onSelect = { exact -> updateRulesCustom { it.copy(exactFinishRequired = exact) } }
                )

                OptionSelectorRow(
                    label = "Main pleine (tous les dés scorent)",
                    options = listOf(
                        true to "Relance obligatoire",
                        false to "Banque autorisée"
                    ),
                    selectedValue = rules.mandatoryRerollOnHotDice,
                    onSelect = { mandatory -> updateRulesCustom { it.copy(mandatoryRerollOnHotDice = mandatory) } }
                )
            }
        }

        // Section 4 : Combinaisons spéciales (Carré, Quinte, 5 As découplés, Suites 5D/6D)
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "COMBINAISONS SPÉCIALES",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )

                OptionSelectorRow(
                    label = "Carré (4 dés identiques)",
                    options = listOf(
                        FourOfAKindMode.DOUBLE_TRIPLE to "Brelan × 2",
                        FourOfAKindMode.DISABLED to "Désactivé"
                    ),
                    selectedValue = rules.fourOfAKindMode,
                    onSelect = { mode -> updateRulesCustom { it.copy(fourOfAKindMode = mode) } }
                )

                OptionSelectorRow(
                    label = "Quinte (5 dés identiques)",
                    options = listOf(
                        FiveOfAKindMode.QUAD_TRIPLE to "Brelan × 4",
                        FiveOfAKindMode.FIVE_FIVES_5000 to "×4 (et 5×5 = 5000)",
                        FiveOfAKindMode.DISABLED to "Désactivée"
                    ),
                    selectedValue = if (rules.fiveOfAKindMode == FiveOfAKindMode.INSTANT_WIN_ON_ONES) {
                        FiveOfAKindMode.QUAD_TRIPLE
                    } else {
                        rules.fiveOfAKindMode
                    },
                    onSelect = { mode -> updateRulesCustom { it.copy(fiveOfAKindMode = mode) } }
                )

                OptionSelectorRow(
                    label = "5 As en un seul lancer = Victoire instantanée (cumulable avec ×4)",
                    options = listOf(
                        false to "Désactivé",
                        true to "5×As = VICTOIRE !"
                    ),
                    selectedValue = rules.isInstantWinOnFiveOnesActive,
                    onSelect = { instantWin ->
                        updateRulesCustom {
                            it.copy(
                                instantWinOnFiveOnes = instantWin,
                                fiveOfAKindMode = if (it.fiveOfAKindMode == FiveOfAKindMode.INSTANT_WIN_ON_ONES) {
                                    FiveOfAKindMode.QUAD_TRIPLE
                                } else {
                                    it.fiveOfAKindMode
                                }
                            )
                        }
                    }
                )

                OptionSelectorRow(
                    label = "Suite de 5 dés (1-2-3-4-5 ou 2-3-4-5-6)",
                    options = listOf(
                        0 to "Off",
                        500 to "500",
                        1000 to "1 000",
                        1500 to "1 500"
                    ),
                    selectedValue = rules.straightScore,
                    onSelect = { score -> updateRulesCustom { it.copy(straightScore = score) } }
                )

                if (rules.diceCount == 6) {
                    OptionSelectorRow(
                        label = "Grande Suite de 6 dés (1-2-3-4-5-6)",
                        options = listOf(
                            0 to "Off",
                            1500 to "1 500",
                            2000 to "2 000",
                            2500 to "2 500",
                            3000 to "3 000"
                        ),
                        selectedValue = rules.fullStraight6DiceScore,
                        onSelect = { score6 -> updateRulesCustom { it.copy(fullStraight6DiceScore = score6) } }
                    )

                    OptionSelectorRow(
                        label = "Trois Paires (6 dés)",
                        options = listOf(
                            0 to "Off",
                            500 to "500",
                            1000 to "1 000",
                            1500 to "1 500"
                        ),
                        selectedValue = rules.threePairsScore,
                        onSelect = { score -> updateRulesCustom { it.copy(threePairsScore = score) } }
                    )
                }

                OptionSelectorRow(
                    label = "Bonus Full (Brelan + Paire)",
                    options = listOf(
                        0 to "Désactivé",
                        200 to "+200 pts",
                        250 to "+250 pts",
                        300 to "+300 pts"
                    ),
                    selectedValue = rules.fullHouseBonus,
                    onSelect = { bonus -> updateRulesCustom { it.copy(fullHouseBonus = bonus) } }
                )
            }
        }

        // Section 5 : Interactions & Pénalités
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "INTERACTIONS & PÉNALITÉS",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )

                OptionSelectorRow(
                    label = "Repiquage (reprendre le pot et les dés du joueur précédent)",
                    options = listOf(
                        false to "Désactivé",
                        true to "Activé"
                    ),
                    selectedValue = rules.piggybackEnabled,
                    onSelect = { enabled -> updateRulesCustom { it.copy(piggybackEnabled = enabled) } }
                )

                OptionSelectorRow(
                    label = "Chapeau / Écrasement (égaliser le score d'un adversaire)",
                    options = listOf(
                        BumpOpponentMode.DISABLED to "Désactivé",
                        BumpOpponentMode.MINUS_500 to "-500 pts",
                        BumpOpponentMode.RESET_TO_PREVIOUS to "Score préc."
                    ),
                    selectedValue = rules.bumpOpponentMode,
                    onSelect = { mode -> updateRulesCustom { it.copy(bumpOpponentMode = mode) } }
                )

                OptionSelectorRow(
                    label = "Pénalité 3 Encoches (3 tours nuls consécutifs : ✕✕✕)",
                    options = listOf(
                        ThreeStrikesMode.DISABLED to ThreeStrikesMode.DISABLED.labelFr,
                        ThreeStrikesMode.MINUS_500 to ThreeStrikesMode.MINUS_500.labelFr,
                        ThreeStrikesMode.MINUS_1000 to ThreeStrikesMode.MINUS_1000.labelFr,
                        ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES to ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES.labelFr,
                        ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES to ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES.labelFr
                    ),
                    selectedValue = rules.threeStrikesMode,
                    onSelect = { mode -> updateRulesCustom { it.copy(threeStrikesMode = mode) } }
                )
                Text(
                    text = rules.threeStrikesMode.descriptionFr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryNeon
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        PixelButton(
            text = "DÉMARRER LA PARTIE (${rules.targetScore} PTS • ${rules.diceCount} DÉS)",
            accentColor = colors.primaryNeon,
            onClick = {
                val initializedPlayers = players.mapIndexed { idx, p ->
                    p.copy(
                        id = idx,
                        score = 0,
                        previousScore = 0,
                        isOpened = rules.openingThreshold == 0,
                        consecutiveBusts = 0,
                        checkpoints = emptyList()
                    )
                }
                onLaunchMatch(rules, initializedPlayers)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionSelectorRow(
    label: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalNeon5KColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, text) ->
                val isSelected = value == selectedValue
                PixelButton(
                    text = text,
                    filled = isSelected,
                    accentColor = if (isSelected) colors.primaryNeon else colors.textMuted,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

private fun cyclePlayerType(current: PlayerType): PlayerType = when (current) {
    PlayerType.HUMAN -> PlayerType.BOT_SAFE
    PlayerType.BOT_SAFE -> PlayerType.BOT_BALANCED
    PlayerType.BOT_BALANCED -> PlayerType.BOT_KAMIKAZE
    PlayerType.BOT_KAMIKAZE, PlayerType.REMOTE -> PlayerType.HUMAN
}

private fun playerTypeButtonLabel(type: PlayerType): String = when (type) {
    PlayerType.HUMAN -> "HUMAIN"
    PlayerType.BOT_SAFE -> "IA PRUDENTE"
    PlayerType.BOT_BALANCED -> "IA ÉQUILIBRÉE"
    PlayerType.BOT_KAMIKAZE -> "IA KAMIKAZE"
    PlayerType.REMOTE -> "DISTANT"
}

private fun playerTypeDescription(type: PlayerType): String = when (type) {
    PlayerType.HUMAN -> "Joueur local (Pass & Play)"
    PlayerType.BOT_SAFE -> "Bot Safe Runner (sécurise dès que P(glitch) > 28 %)"
    PlayerType.BOT_BALANCED -> "Bot Cyber Balanced (optimise l'espérance nette EV + dataset local)"
    PlayerType.BOT_KAMIKAZE -> "Bot Overclock Kamikaze (chasse les mains pleines)"
    PlayerType.REMOTE -> "Joueur distant (P2P / Réseau)"
}

private fun defaultNameForType(index: Int, type: PlayerType): String = when (type) {
    PlayerType.HUMAN -> "JOUEUR ${index + 1}"
    PlayerType.BOT_SAFE -> "SAFE-BOT #${index + 1}"
    PlayerType.BOT_BALANCED -> "CYBER-BOT #${index + 1}"
    PlayerType.BOT_KAMIKAZE -> "KAMIKAZE #${index + 1}"
    PlayerType.REMOTE -> "REMOTE #${index + 1}"
}
