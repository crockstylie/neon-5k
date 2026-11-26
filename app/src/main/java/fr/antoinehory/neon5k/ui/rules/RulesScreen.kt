package fr.antoinehory.neon5k.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import fr.antoinehory.neon5k.R
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.DiceScorer
import fr.antoinehory.neon5k.domain.rules.FiveOfAKindMode
import fr.antoinehory.neon5k.domain.rules.FourOfAKindMode
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.RulesPreset
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import kotlin.random.Random

/**
 * Écran Codex des règles du 5000 avec Simulateur Interactif de Barème Pixel Art (Lot 3).
 * Permet de manipuler 5 ou 6 dés en temps réel pour visualiser les combinaisons détectées par [DiceScorer]
 * selon les variantes actives, et détaille toutes les règles officielles et variantes (dont les Paliers & 3 Encoches `✕`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RulesScreen(
    activeRules: GameRulesConfig,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    var simulatedRules by remember(activeRules) { mutableStateOf(activeRules) }
    var rollAnimationTick by remember { mutableIntStateOf(0) }

    val simulatorFaces = remember {
        mutableStateListOf(1, 5, 5, 5, 2, 6)
    }

    val currentFaces = simulatorFaces.take(simulatedRules.diceCount)
    val scoringIndices = remember(currentFaces, simulatedRules) {
        DiceScorer.findScoringDieIndices(currentFaces, simulatedRules)
    }
    val evaluation = remember(currentFaces, simulatedRules) {
        DiceScorer.evaluateMaxScoringSubset(currentFaces, simulatedRules)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Barre supérieure
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelButton(
                text = "← ${stringResource(R.string.back).uppercase()}",
                onClick = onBack,
                filled = false,
                accentColor = colors.textPrimary
            )
            Text(
                text = stringResource(R.string.rules_screen_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryNeon
            )
        }

        // Section 1 : Simulateur Interactif de Barème
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.primaryNeon
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.rules_simulator_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                Text(
                    text = stringResource(R.string.rules_simulator_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )

                // Sélecteur rapide de preset pour le simulateur
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        RulesPreset.CLASSIC_5_DICE to "5 Dés Classique",
                        RulesPreset.VARIANT_6_DICE to "6 Dés (3 Paires/Full)"
                    ).forEach { (preset, label) ->
                        val selected = simulatedRules.preset == preset &&
                            simulatedRules.diceCount == GameRulesConfig.fromPreset(preset).diceCount
                        PixelButton(
                            text = label,
                            filled = selected,
                            accentColor = if (selected) colors.secondaryNeon else colors.textMuted,
                            onClick = { simulatedRules = GameRulesConfig.fromPreset(preset) }
                        )
                    }
                    if (activeRules.preset == RulesPreset.CUSTOM) {
                        val isCustom = simulatedRules == activeRules
                        PixelButton(
                            text = "Mes Règles (${activeRules.diceCount}D)",
                            filled = isCustom,
                            accentColor = if (isCustom) colors.primaryNeon else colors.textMuted,
                            onClick = { simulatedRules = activeRules }
                        )
                    }
                }

                // Ligne de dés Pixel Art cliquables (1..6)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentFaces.forEachIndexed { idx, face ->
                        val isScoring = idx in scoringIndices
                        PixelDiceCanvas(
                            die = DieState(
                                index = idx,
                                face = face,
                                isLocked = false,
                                isSelected = isScoring,
                                canScoreOnCurrentRoll = isScoring
                            ),
                            rollTrigger = rollAnimationTick,
                            onClick = {
                                simulatorFaces[idx] = (face % 6) + 1
                            },
                            dieSize = 54.dp
                        )
                    }
                }

                // Boutons de combinaisons d'exemple rapides
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PixelButton(
                        text = "ALÉATOIRE",
                        filled = false,
                        accentColor = colors.primaryNeon,
                        onClick = {
                            for (i in 0 until 6) {
                                simulatorFaces[i] = Random.nextInt(1, 7)
                            }
                            rollAnimationTick++
                        }
                    )
                    PixelButton(
                        text = "BRELAN D'AS",
                        filled = false,
                        accentColor = colors.secondaryNeon,
                        onClick = {
                            val presetFaces = listOf(1, 1, 1, 5, 2, 4)
                            presetFaces.forEachIndexed { i, f -> simulatorFaces[i] = f }
                        }
                    )
                    PixelButton(
                        text = "SUITE",
                        filled = false,
                        accentColor = colors.secondaryNeon,
                        onClick = {
                            val presetFaces = listOf(1, 2, 3, 4, 5, 6)
                            presetFaces.forEachIndexed { i, f -> simulatorFaces[i] = f }
                        }
                    )
                    if (simulatedRules.diceCount == 6) {
                        PixelButton(
                            text = "3 PAIRES",
                            filled = false,
                            accentColor = colors.alertNeon,
                            onClick = {
                                val presetFaces = listOf(2, 2, 4, 4, 6, 6)
                                presetFaces.forEachIndexed { i, f -> simulatorFaces[i] = f }
                            }
                        )
                    }
                }

                // Résultat d'évaluation en direct
                val isHotDice = scoringIndices.size == simulatedRules.diceCount && evaluation.totalPoints > 0
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background.copy(alpha = 0.65f), CyberCutShape)
                        .border(
                            width = 1.dp,
                            color = when {
                                evaluation.totalPoints == 0 -> colors.alertNeon
                                isHotDice -> colors.secondaryNeon
                                else -> colors.primaryNeon.copy(alpha = 0.5f)
                            },
                            shape = CyberCutShape
                        )
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (evaluation.totalPoints > 0) {
                            "+${evaluation.totalPoints} PTS"
                        } else {
                            "0 PT // GLITCH (COUP NUL)"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (evaluation.totalPoints > 0) colors.primaryNeon else colors.alertNeon
                    )
                    val combosLabel = if (evaluation.combinations.isEmpty()) {
                        "Aucun As, aucun 5, ni combinaison valide sur ce tirage."
                    } else {
                        evaluation.combinations.joinToString(" + ") { "${it.labelFr} (${it.points})" }
                    }
                    Text(
                        text = combosLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    if (isHotDice) {
                        Text(
                            text = "★ MAIN PLEINE (${simulatedRules.diceCount}/${simulatedRules.diceCount} dés scorants) — Relance de tous les dés !",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryNeon,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Section 2 : Barème officiel & actif
        PaytableSectionPanel(rules = simulatedRules)

        // Section 3 : Déroulement d'un tour & Règles fondamentales
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DÉROULEMENT D'UN TOUR & MAIN PLEINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                RuleBullet(
                    title = "1. Sélection obligatoire",
                    body = "À chaque lancer, vous devez mettre de côté au moins un dé scorant (As, 5 ou combinaison complète obtenue sur ce même lancer). Les brelans ne se cumulent jamais sur deux lancers séparés."
                )
                RuleBullet(
                    title = "2. Relancer ou Banquer",
                    body = "Tant qu'il vous reste des dés non verrouillés, vous pouvez soit BANQUER le pot accumulé sur votre feuille de score, soit RELANCER les dés restants en risquant tout le pot du tour."
                )
                RuleBullet(
                    title = "3. Glitch (Coup nul)",
                    body = "Si un lancer ne produit aucun dé scorant parmi les dés relancés, le pot du tour est perdu et vous notez une Encoche (✕) sur votre palier de score actuel."
                )
                RuleBullet(
                    title = "4. Main pleine (Hot Dice)",
                    body = "Lorsque vos ${simulatedRules.diceCount} dés ont tous scoré, vous récupérez l'intégralité des ${simulatedRules.diceCount} dés pour continuer à faire grimper le pot du même tour !"
                )
            }
        }

        // Section 4 : Paliers, 3 Encoches (✕) & Variantes entre potes
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PALIERS, 3 ENCOCHES (✕) & VARIANTES",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondaryNeon
                )
                RuleBullet(
                    title = "Ouverture (${simulatedRules.openingThreshold} pts)",
                    body = if (simulatedRules.openingThreshold > 0) {
                        "Pour entrer dans la partie, votre premier tour banqué doit atteindre au moins ${simulatedRules.openingThreshold} pts en un seul tour."
                    } else {
                        "Ouverture libre (0 pt) : vous pouvez banquer dès votre premier point."
                    }
                )
                RuleBullet(
                    title = "3 Encoches (✕✕✕) & Recul de Palier avec mémoire",
                    body = "Sur feuille papier, chaque score banqué crée un Palier et chaque coup nul ajoute une Encoche (✕) en face de ce palier. Avec la variante « Palier préc. (mémoire ✕) » : si vous êtes à 1 000, montez à 1 200 et faites 2 nuls (1 200 [✕✕·]), puis montez à 1 700 et faites 3 nuls d'affilée, vous retombez au palier 1 200 en retrouvant vos 2 encoches [✕✕·] ! Un seul nouveau nul suffit alors à totaliser 3 encoches sur 1 200 et vous refait descendre à 1 000."
                )
                RuleBullet(
                    title = "Chapeau / Écrasement",
                    body = "Si la variante est activée et que vous banquez un score total identique à celui d'un adversaire, vous lui faites un « Chapeau » : il recule de 500 pts ou retombe à son palier précédent."
                )
                RuleBullet(
                    title = "Repiquage du pot précédent",
                    body = "Si activé, lorsqu'un joueur banque en laissant des dés libres, le joueur suivant (s'il est déjà ouvert) peut reprendre ce pot et lancer uniquement les dés restants."
                )
            }
        }
    }
}

/**
 * Tableau récapitulatif du barème dynamique selon une configuration [GameRulesConfig].
 */
@Composable
fun PaytableSectionPanel(
    rules: GameRulesConfig,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    CyberPanel(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "BARÈME DES COMBINAISONS (${rules.diceCount} DÉS • ${rules.preset.labelFr.uppercase()})",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon
            )
            PaytableRow("As isolé (1)", "100 pts")
            PaytableRow("Cinq isolé (5)", "50 pts")
            PaytableRow("Brelan d'As (3 × 1 en un lancer)", "1 000 pts")
            PaytableRow("Brelan de 2 à 6 (3 × N en un lancer)", "N × 100 pts (200 à 600)")
            PaytableRow(
                label = "Carré (4 identiques en un lancer)",
                value = when (rules.fourOfAKindMode) {
                    FourOfAKindMode.DOUBLE_TRIPLE -> "Brelan × 2 (ex: 4×As = 2 000)"
                    FourOfAKindMode.DISABLED -> "Désactivé (compté en Brelan)"
                }
            )
            PaytableRow(
                label = "Quinte (5 identiques en un lancer)",
                value = when (rules.fiveOfAKindMode) {
                    FiveOfAKindMode.QUAD_TRIPLE -> "Brelan × 4 (ex: 5×As = 4 000)"
                    FiveOfAKindMode.INSTANT_WIN_ON_ONES -> "5×As = VICTOIRE • Autres = Brelan × 4"
                    FiveOfAKindMode.DISABLED -> "Désactivé"
                }
            )
            PaytableRow(
                label = "Suite (1-2-3-4-5 ou 2-3-4-5-6)",
                value = if (rules.straightScore > 0) "${rules.straightScore} pts" else "Désactivée"
            )
            if (rules.diceCount == 6) {
                PaytableRow(
                    label = "3 Paires distinctes (6 dés)",
                    value = if (rules.threePairsScore > 0) "${rules.threePairsScore} pts" else "Désactivées"
                )
            }
            PaytableRow(
                label = "Bonus Full (Brelan + Paire distincte)",
                value = if (rules.fullHouseBonus > 0) "Brelan + ${rules.fullHouseBonus} pts" else "Désactivé"
            )
            PaytableRow(
                label = "Pénalité 3 Encoches (✕✕✕)",
                value = when (rules.threeStrikesMode) {
                    ThreeStrikesMode.DISABLED -> "Aucune"
                    ThreeStrikesMode.MINUS_500 -> "-500 pts"
                    ThreeStrikesMode.MINUS_1000 -> "-1 000 pts"
                    ThreeStrikesMode.REVERT_TO_PREVIOUS_CLEAR_STRIKES -> "Recul palier préc. (0 ✕)"
                    ThreeStrikesMode.REVERT_TO_PREVIOUS_PERSIST_STRIKES -> "Recul palier préc. (mémoire ✕)"
                }
            )
            PaytableRow(
                label = "Chapeau / Écrasement",
                value = when (rules.bumpOpponentMode) {
                    BumpOpponentMode.DISABLED -> "Désactivé"
                    BumpOpponentMode.MINUS_500 -> "-500 pts à l'adversaire égalisé"
                    BumpOpponentMode.RESET_TO_PREVIOUS -> "Adversaire renvoyé au palier préc."
                }
            )
        }
    }
}

@Composable
private fun PaytableRow(label: String, value: String) {
    val colors = LocalNeon5KColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.secondaryNeon,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun RuleBullet(title: String, body: String) {
    val colors = LocalNeon5KColors.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
    }
}

/**
 * Modale rapide accessible via le bouton `[?]` depuis `GameScreen` en pleine partie.
 */
@Composable
fun QuickRulesPaytableDialog(
    rules: GameRulesConfig,
    onDismiss: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    Dialog(onDismissRequest = onDismiss) {
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.primaryNeon
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AIDE RAPIDE // RÈGLES DE LA TABLE",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryNeon
                )
                Text(
                    text = "Cible : ${rules.targetScore} pts (${if (rules.exactFinishRequired) "Score exact requis" else "Dépassement autorisé"}) • Ouverture : ${rules.openingThreshold} pts • Main pleine : ${if (rules.mandatoryRerollOnHotDice) "Relance obligatoire" else "Banque libre"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryNeon
                )
                PaytableSectionPanel(rules = rules)
                Spacer(modifier = Modifier.height(4.dp))
                PixelButton(
                    text = "FERMER L'AIDE",
                    onClick = onDismiss,
                    accentColor = colors.primaryNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
