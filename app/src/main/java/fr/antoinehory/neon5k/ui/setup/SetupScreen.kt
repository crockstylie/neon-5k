package fr.antoinehory.neon5k.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors

private data class DraftPlayer(
    val name: String,
    val type: PlayerType
)

/**
 * Écran de configuration d'une partie Solo vs IA / Pass & Play (joueurs + profils de bots + presets et variantes du 5000).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    initialRules: GameRulesConfig,
    onBack: () -> Unit,
    onLaunchMatch: (rules: GameRulesConfig, players: List<PlayerState>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    var rules by remember(initialRules) { mutableStateOf(initialRules) }
    val draftPlayers = remember {
        mutableStateListOf(
            DraftPlayer(name = "JOUEUR 1", type = PlayerType.HUMAN),
            DraftPlayer(name = "CYBER-BOT", type = PlayerType.BOT_BALANCED)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelButton(
                text = "← ACCUEIL",
                onClick = onBack,
                filled = false,
                accentColor = colors.textPrimary
            )
            Text(
                text = "CONFIG // 5000",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryNeon
            )
        }

        // Section 1 : Joueurs Humains & Bots Locaux (Safe Runner, Cyber Balanced, Overclock Kamikaze)
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "JOUEURS & IA (${draftPlayers.size}/6)",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryNeon
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PixelButton(
                            text = "-1",
                            enabled = draftPlayers.size > 1,
                            filled = false,
                            accentColor = colors.secondaryNeon,
                            onClick = {
                                if (draftPlayers.size > 1) draftPlayers.removeAt(draftPlayers.lastIndex)
                            }
                        )
                        PixelButton(
                            text = "+HUMAIN",
                            enabled = draftPlayers.size < 6,
                            filled = false,
                            accentColor = colors.primaryNeon,
                            onClick = {
                                if (draftPlayers.size < 6) {
                                    draftPlayers.add(
                                        DraftPlayer(
                                            name = "JOUEUR ${draftPlayers.size + 1}",
                                            type = PlayerType.HUMAN
                                        )
                                    )
                                }
                            }
                        )
                        PixelButton(
                            text = "+BOT",
                            enabled = draftPlayers.size < 6,
                            filled = false,
                            accentColor = colors.alertNeon,
                            onClick = {
                                if (draftPlayers.size < 6) {
                                    draftPlayers.add(
                                        DraftPlayer(
                                            name = "BOT-${draftPlayers.size + 1}",
                                            type = PlayerType.BOT_BALANCED
                                        )
                                    )
                                }
                            }
                        )
                    }
                }

                draftPlayers.forEachIndexed { idx, draft ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { newValue ->
                                draftPlayers[idx] = draft.copy(name = newValue.take(18))
                            },
                            singleLine = true,
                            label = {
                                Text("Acteur #${idx + 1}", style = MaterialTheme.typography.labelSmall)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.primaryNeon,
                                unfocusedBorderColor = colors.primaryNeon.copy(alpha = 0.35f),
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedLabelColor = colors.primaryNeon,
                                unfocusedLabelColor = colors.textMuted
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                PlayerType.HUMAN to "Humain",
                                PlayerType.BOT_SAFE to "IA Safe",
                                PlayerType.BOT_BALANCED to "IA Balanced",
                                PlayerType.BOT_KAMIKAZE to "IA Kamikaze"
                            ).forEach { (typeOption, label) ->
                                val selected = draft.type == typeOption
                                PixelButton(
                                    text = label,
                                    filled = selected,
                                    accentColor = when {
                                        !selected -> colors.textMuted
                                        typeOption == PlayerType.HUMAN -> colors.primaryNeon
                                        typeOption == PlayerType.BOT_KAMIKAZE -> colors.alertNeon
                                        else -> colors.secondaryNeon
                                    },
                                    onClick = {
                                        val updatedName = when {
                                            typeOption == PlayerType.BOT_SAFE && draft.name.startsWith("JOUEUR") -> "SAFE-BOT"
                                            typeOption == PlayerType.BOT_BALANCED && draft.name.startsWith("JOUEUR") -> "CYBER-BOT"
                                            typeOption == PlayerType.BOT_KAMIKAZE && draft.name.startsWith("JOUEUR") -> "KAMIKAZE"
                                            else -> draft.name
                                        }
                                        draftPlayers[idx] = draft.copy(name = updatedName, type = typeOption)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2 : Presets rapides
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
                        RulesPreset.VARIANT_6_DICE,
                        RulesPreset.QUICK_2500
                    ).forEach { preset ->
                        val isSelected = rules.preset == preset
                        PixelButton(
                            text = preset.labelFr,
                            filled = isSelected,
                            accentColor = if (isSelected) colors.primaryNeon else colors.textMuted,
                            onClick = { rules = GameRulesConfig.fromPreset(preset) }
                        )
                    }
                }
                Text(
                    text = "Fingerprint : ${rules.fingerprint()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryNeon
                )
            }
        }

        // Section 3 : Variantes paramétrables
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PARAMÈTRES & VARIANTES DU 5000",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )

                OptionSelectorRow(
                    title = "Nombre de dés",
                    options = listOf(5 to "5 dés", 6 to "6 dés"),
                    selectedValue = rules.diceCount,
                    onSelect = { count ->
                        rules = rules.copy(
                            preset = RulesPreset.CUSTOM,
                            diceCount = count,
                            threePairsScore = if (count == 6 && rules.threePairsScore == 0) 1000 else rules.threePairsScore
                        )
                    }
                )

                OptionSelectorRow(
                    title = "Score cible",
                    options = listOf(2500 to "2 500", 5000 to "5 000", 10000 to "10 000"),
                    selectedValue = rules.targetScore,
                    onSelect = { target ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, targetScore = target)
                    }
                )

                OptionSelectorRow(
                    title = "Seuil d'ouverture",
                    options = listOf(0 to "0 (Libre)", 500 to "500", 750 to "750", 1000 to "1 000"),
                    selectedValue = rules.openingThreshold,
                    onSelect = { threshold ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, openingThreshold = threshold)
                    }
                )

                OptionSelectorRow(
                    title = "Suite (1-5 / 2-6)",
                    options = listOf(0 to "Off", 500 to "500", 1000 to "1 000", 1500 to "1 500"),
                    selectedValue = rules.straightScore,
                    onSelect = { score ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, straightScore = score)
                    }
                )

                if (rules.diceCount == 6) {
                    OptionSelectorRow(
                        title = "3 Paires (6 dés)",
                        options = listOf(0 to "Off", 750 to "750", 1000 to "1 000", 1500 to "1 500"),
                        selectedValue = rules.threePairsScore,
                        onSelect = { score ->
                            rules = rules.copy(preset = RulesPreset.CUSTOM, threePairsScore = score)
                        }
                    )
                }

                OptionSelectorRow(
                    title = "Bonus Full (Brelan + Paire)",
                    options = listOf(0 to "Off", 200 to "+200 pts"),
                    selectedValue = rules.fullHouseBonus,
                    onSelect = { bonus ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, fullHouseBonus = bonus)
                    }
                )

                OptionSelectorRow(
                    title = "Carré (4 identiques)",
                    options = listOf(
                        FourOfAKindMode.DOUBLE_TRIPLE to "×2 Brelan",
                        FourOfAKindMode.DISABLED to "Off"
                    ),
                    selectedValue = rules.fourOfAKindMode,
                    onSelect = { mode ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, fourOfAKindMode = mode)
                    }
                )

                OptionSelectorRow(
                    title = "Quinte (5 identiques)",
                    options = listOf(
                        FiveOfAKindMode.QUAD_TRIPLE to "×4 Brelan",
                        FiveOfAKindMode.INSTANT_WIN_ON_ONES to "5 As = Victoire",
                        FiveOfAKindMode.DISABLED to "Off"
                    ),
                    selectedValue = rules.fiveOfAKindMode,
                    onSelect = { mode ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, fiveOfAKindMode = mode)
                    }
                )

                OptionSelectorRow(
                    title = "Chapeau / Écrasement",
                    options = listOf(
                        BumpOpponentMode.DISABLED to "Off",
                        BumpOpponentMode.MINUS_500 to "-500 pts",
                        BumpOpponentMode.RESET_TO_PREVIOUS to "Score préc."
                    ),
                    selectedValue = rules.bumpOpponentMode,
                    onSelect = { mode ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, bumpOpponentMode = mode)
                    }
                )

                OptionSelectorRow(
                    title = "Pénalité 3 coups nuls",
                    options = listOf(0 to "Off", 500 to "-500 pts", 1000 to "-1 000 pts"),
                    selectedValue = rules.threeStrikesPenalty,
                    onSelect = { penalty ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, threeStrikesPenalty = penalty)
                    }
                )

                ToggleRuleRow(
                    title = "Score exact requis pour finir",
                    subtitle = "Si actif, dépasser ${rules.targetScore} annule le tour.",
                    checked = rules.exactFinishRequired,
                    onCheckedChange = { checked ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, exactFinishRequired = checked)
                    }
                )

                ToggleRuleRow(
                    title = "Main pleine : relance obligatoire",
                    subtitle = "Lorsque tous les dés scorent, oblige à relancer les ${rules.diceCount} dés.",
                    checked = rules.mandatoryRerollOnHotDice,
                    onCheckedChange = { checked ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, mandatoryRerollOnHotDice = checked)
                    }
                )

                ToggleRuleRow(
                    title = "Repiquage du pot précédent",
                    subtitle = "Un joueur ouvert peut reprendre le pot et les dés restants du joueur précédent.",
                    checked = rules.piggybackEnabled,
                    onCheckedChange = { checked ->
                        rules = rules.copy(preset = RulesPreset.CUSTOM, piggybackEnabled = checked)
                    }
                )
            }
        }

        PixelButton(
            text = "INITIALISER LA TABLE DE JEU",
            onClick = {
                val players = draftPlayers.mapIndexed { idx, draft ->
                    PlayerState(
                        id = idx,
                        name = draft.name.trim().ifEmpty { "ACTEUR ${idx + 1}" },
                        type = draft.type
                    )
                }
                onLaunchMatch(rules, players)
            },
            accentColor = colors.primaryNeon,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> OptionSelectorRow(
    title: String,
    options: List<Pair<T, String>>,
    selectedValue: T,
    onSelect: (T) -> Unit
) {
    val colors = LocalNeon5KColors.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (value, label) ->
                val selected = value == selectedValue
                PixelButton(
                    text = label,
                    filled = selected,
                    accentColor = if (selected) colors.secondaryNeon else colors.textMuted,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}

@Composable
private fun ToggleRuleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalNeon5KColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.primaryNeon,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surface
            )
        )
    }
}
