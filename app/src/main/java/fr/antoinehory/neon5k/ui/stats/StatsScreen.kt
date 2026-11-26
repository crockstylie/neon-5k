package fr.antoinehory.neon5k.ui.stats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.data.export.DatasetExportFormat
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.playgames.PlayGamesStatus
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress
import fr.antoinehory.neon5k.domain.achievement.MasteryRank
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelAchievementIconCanvas
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.util.Locale
import kotlin.math.abs

private enum class StatsTab(val labelFr: String) {
    TELEMETRY("FUN STATS"),
    ACHIEVEMENTS("20 SUCCÈS"),
    AI_DATASET("DATASET IA")
}

/**
 * Écran unifié de Télémétrie Fun, des 20 Succès à Compteurs Infinis (Play Jeux v2)
 * et d'Export SAF du Dataset IA (Lots 2 & 4).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    stats: PlayerStatsEntity,
    achievements: List<AchievementProgress>,
    playGamesStatus: PlayGamesStatus,
    decisionLogsCount: Int,
    exportStatusMessageFr: String?,
    onOpenPlayGamesAchievements: () -> Unit,
    onExportDataset: (uri: Uri, format: DatasetExportFormat) -> Unit,
    onClearDataset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    var activeTab by rememberSaveable { mutableStateOf(StatsTab.TELEMETRY) }

    val jsonlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DatasetExportFormat.JSONL.mimeType)
    ) { uri ->
        if (uri != null) {
            onExportDataset(uri, DatasetExportFormat.JSONL)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(DatasetExportFormat.CSV.mimeType)
    ) { uri ->
        if (uri != null) {
            onExportDataset(uri, DatasetExportFormat.CSV)
        }
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
        fr.antoinehory.neon5k.ui.components.NeonSubScreenTopBar(
            title = "STATS & SUCCÈS",
            icon = fr.antoinehory.neon5k.ui.components.PixelIconId.TROPHY,
            onBack = onBack
        )

        // Barre d'onglets (Fun Stats | 20 Succès | Dataset IA)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsTab.entries.forEach { tab ->
                val selected = activeTab == tab
                PixelButton(
                    text = tab.labelFr,
                    filled = selected,
                    accentColor = when {
                        !selected -> colors.textMuted
                        tab == StatsTab.ACHIEVEMENTS -> colors.alertNeon
                        tab == StatsTab.AI_DATASET -> colors.secondaryNeon
                        else -> colors.primaryNeon
                    },
                    onClick = { activeTab = tab },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (activeTab) {
            StatsTab.TELEMETRY -> FunTelemetryTabContent(stats = stats)
            StatsTab.ACHIEVEMENTS -> AchievementsTabContent(
                achievements = achievements,
                playGamesStatus = playGamesStatus,
                statusMessageFr = exportStatusMessageFr,
                onOpenPlayGamesAchievements = onOpenPlayGamesAchievements
            )
            StatsTab.AI_DATASET -> AiDatasetTabContent(
                decisionLogsCount = decisionLogsCount,
                exportStatusMessageFr = exportStatusMessageFr,
                onLaunchJsonl = {
                    jsonlLauncher.launch("neon5k_ai_dataset_${System.currentTimeMillis()}.jsonl")
                },
                onLaunchCsv = {
                    csvLauncher.launch("neon5k_ai_dataset_${System.currentTimeMillis()}.csv")
                },
                onClearDataset = onClearDataset
            )
        }
    }
}

@Composable
private fun FunTelemetryTabContent(stats: PlayerStatsEntity) {
    val colors = LocalNeon5KColors.current
    val winRatePercent = if (stats.matchesPlayed > 0) {
        (stats.matchesWon * 100.0) / stats.matchesPlayed.toDouble()
    } else {
        0.0
    }

    // 1. Bilan général & Duels vs IA
    CyberPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "BILAN GÉNÉRAL & DUELS CONTRE L'IA",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    label = "PARTIES JOUÉES",
                    value = stats.matchesPlayed.toString(),
                    accentColor = colors.primaryNeon,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label = "VICTOIRES",
                    value = "${stats.matchesWon} (${String.format(Locale.US, "%.0f", winRatePercent)}%)",
                    accentColor = colors.secondaryNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricCard(
                    label = "MEILLEUR TOUR",
                    value = "${stats.bestTurnScore} PTS",
                    accentColor = colors.alertNeon,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    label = "MAINS PLEINES",
                    value = stats.totalHotDiceCount.toString(),
                    accentColor = colors.primaryNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            TelemetryKeyValueRow(
                label = "Vs IA Safe Runner (Prudent)",
                value = "${stats.winsVsBotSafe} V  /  ${stats.lossesVsBotSafe} D",
                valueColor = colors.primaryNeon
            )
            TelemetryKeyValueRow(
                label = "Vs IA Cyber Balanced (Équilibré)",
                value = "${stats.winsVsBotBalanced} V  /  ${stats.lossesVsBotBalanced} D",
                valueColor = colors.secondaryNeon
            )
            TelemetryKeyValueRow(
                label = "Vs IA Overclock Kamikaze (Agressif)",
                value = "${stats.winsVsBotKamikaze} V  /  ${stats.lossesVsBotKamikaze} D",
                valueColor = colors.alertNeon
            )
        }
    }

    // 2. Collection de Brelans (par face 1..6) & Combinaisons Spéciales
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "BRELANS & COMBINAISONS",
                style = MaterialTheme.typography.labelLarge,
                color = colors.secondaryNeon
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.55f), CyberCutShape)
                    .border(1.dp, colors.primaryNeon.copy(alpha = 0.25f), CyberCutShape)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brelan Collector : ${stats.distinctTripleFacesCount}/6 faces",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                Text(
                    text = "TOTAL BRELANS : ×${stats.totalTriplesCount}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryNeon
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniComboBadge("3 × AS (1)", stats.triple1Count, colors.alertNeon, Modifier.weight(1f))
                MiniComboBadge("3 × DEUX (2)", stats.triple2Count, colors.primaryNeon, Modifier.weight(1f))
                MiniComboBadge("3 × TROIS (3)", stats.triple3Count, colors.primaryNeon, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MiniComboBadge("3 × QUATRE (4)", stats.triple4Count, colors.primaryNeon, Modifier.weight(1f))
                MiniComboBadge("3 × CINQ (5)", stats.triple5Count, colors.secondaryNeon, Modifier.weight(1f))
                MiniComboBadge("3 × SIX (6)", stats.triple6Count, colors.primaryNeon, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(2.dp))

            TelemetryKeyValueRow("Carrés (4 dés identiques)", "×${stats.fourOfAKindCount}", colors.primaryNeon)
            TelemetryKeyValueRow("Quintes / God Rolls (5 identiques)", "×${stats.fiveOfAKindCount}", colors.alertNeon)
            TelemetryKeyValueRow("Suites (1-5 ou 2-6)", "×${stats.straightCount}", colors.secondaryNeon)
            TelemetryKeyValueRow("3 Paires (variante 6 dés)", "×${stats.threePairsCount}", colors.primaryNeon)
            TelemetryKeyValueRow("Fulls avec bonus (Brelan + Paire)", "×${stats.fullHouseCount}", colors.secondaryNeon)
            TelemetryKeyValueRow("As isolés / Cinq isolés tirés", "${stats.singleOnesCount} As • ${stats.singleFivesCount} Cinq", colors.textPrimary)
        }
    }

    // 3. Psychologie, Prise de Risque & Paliers
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.alertNeon.copy(alpha = 0.55f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "PSYCHOLOGIE, PRISE DE RISQUE & ENCOCHES (✕)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.alertNeon
            )

            val totalGenerated = stats.totalPointsBanked + stats.totalPointsLostToBusts
            val bankEfficiency = if (totalGenerated > 0L) {
                (stats.totalPointsBanked * 100.0) / totalGenerated.toDouble()
            } else {
                100.0
            }

            TelemetryKeyValueRow(
                label = "Points totaux sécurisés en banque",
                value = "${stats.totalPointsBanked} pts (${String.format(Locale.US, "%.0f", bankEfficiency)}%)",
                valueColor = colors.primaryNeon
            )
            TelemetryKeyValueRow(
                label = "Points partis en fumée (Glitch)",
                value = "${stats.totalPointsLostToBusts} pts (${stats.totalBustsCount} crashs)",
                valueColor = colors.alertNeon
            )
            TelemetryKeyValueRow(
                label = "Pire crash en un seul tour",
                value = "${stats.worstBustPotLost} pts perdus",
                valueColor = colors.alertNeon
            )
            TelemetryKeyValueRow(
                label = "Sauvetages miraculés à 1 seul dé",
                value = "×${stats.oneDieClutchCount}",
                valueColor = colors.secondaryNeon
            )
            TelemetryKeyValueRow(
                label = "Repiquages réussis (Record)",
                value = "×${stats.piggybacksSucceededCount} (Max : ${stats.biggestPiggybackScore} pts)",
                valueColor = colors.primaryNeon
            )
            TelemetryKeyValueRow(
                label = "Chapeaux infligés vs subis",
                value = "${stats.bumpsInflictedCount} infligés • ${stats.bumpsSufferedCount} subis",
                valueColor = colors.secondaryNeon
            )
            TelemetryKeyValueRow(
                label = "Pénalités / Reculs (3 Encoches ✕✕✕)",
                value = "×${stats.threeStrikesPenaltiesCount}",
                valueColor = colors.alertNeon
            )
        }
    }

    // 4. Entropie Matérielle SHA-256 & Histogramme d'équité des faces 1..6
    CyberPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AUDIT D'ÉQUITÉ SHA-256 & CAPTEURS",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon
            )

            val pressSeconds = stats.totalTouchPressDurationMs / 1000.0
            TelemetryKeyValueRow(
                label = "Lancers effectués (Dés jetés)",
                value = "${stats.totalRollsCount} lancers (${stats.totalIndividualDiceThrown} dés)",
                valueColor = colors.textPrimary
            )
            TelemetryKeyValueRow(
                label = "Lancers à la secousse physique",
                value = "×${stats.shakeRollsCount}",
                valueColor = colors.secondaryNeon
            )
            TelemetryKeyValueRow(
                label = "Durée cumulée d'entropie tactile",
                value = "${String.format(Locale.US, "%.2f", pressSeconds)} s",
                valueColor = colors.primaryNeon
            )

            Spacer(modifier = Modifier.height(4.dp))

            val faceCounts = listOf(
                1 to stats.face1Count,
                2 to stats.face2Count,
                3 to stats.face3Count,
                4 to stats.face4Count,
                5 to stats.face5Count,
                6 to stats.face6Count
            )
            val totalFaces = faceCounts.sumOf { it.second }.coerceAtLeast(1)
            val maxFaceCount = faceCounts.maxOf { it.second }.coerceAtLeast(1)

            Text(
                text = "DISTRIBUTION DES FACES 1 À 6 (CIBLE THÉORIQUE : 16.7 %)",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryNeon
            )

            faceCounts.forEach { (face, count) ->
                val pct = if (stats.totalIndividualDiceThrown > 0) (count * 100.0) / totalFaces.toDouble() else 0.0
                val barFraction = if (stats.totalIndividualDiceThrown > 0) {
                    (count.toFloat() / maxFaceCount.toFloat()).coerceIn(0.04f, 1f)
                } else {
                    0.04f
                }
                val deviation = abs(pct - 16.666)

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FACE [$face]",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (face == 1 || face == 5) colors.primaryNeon else colors.textPrimary
                        )
                        Text(
                            text = "$count× (${String.format(Locale.US, "%.1f", pct)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (deviation < 3.5 || stats.totalIndividualDiceThrown < 30) {
                                colors.secondaryNeon
                            } else {
                                colors.alertNeon
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(colors.background, CyberCutShape)
                            .border(1.dp, colors.primaryNeon.copy(alpha = 0.25f), CyberCutShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .fillMaxHeight()
                                .background(
                                    if (face == 1 || face == 5) colors.primaryNeon else colors.secondaryNeon,
                                    CyberCutShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsTabContent(
    achievements: List<AchievementProgress>,
    playGamesStatus: PlayGamesStatus,
    statusMessageFr: String?,
    onOpenPlayGamesAchievements: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val unlockedCount = achievements.count { it.isUnlocked }
    val earnedXp = achievements.filter { it.isUnlocked }.sumOf { it.id.playGamesXp }
    val totalLifetimeTriggers = achievements.sumOf { it.lifetimeCount }

    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.alertNeon
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "PROGRESSION : $unlockedCount / ${achievements.size} SUCCÈS",
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryNeon
            )
            Text(
                text = "$earnedXp / 1 000 XP PLAY JEUX • TOTAL OCCURRENCES : ×$totalLifetimeTriggers",
                style = MaterialTheme.typography.labelSmall,
                color = colors.alertNeon
            )

            Text(
                text = "Les compteurs de Neon 5K ne s'arrêtent jamais après le déverrouillage : continuez à cumuler chaque exploit pour faire évoluer vos badges du rang BRONZE jusqu'au rang CYBER-PLATINE !",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )

            Text(
                text = "Play Jeux : ${playGamesStatus.statusNoteFr}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryNeon
            )

            if (statusMessageFr != null) {
                Text(
                    text = statusMessageFr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.alertNeon
                )
            }

            PixelButton(
                text = "PLAY JEUX // VOIR LES SUCCÈS CLOUD",
                onClick = onOpenPlayGamesAchievements,
                filled = false,
                accentColor = colors.primaryNeon,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    achievements.forEach { progress ->
        AchievementCard(progress = progress)
    }
}

@Composable
private fun AchievementCard(progress: AchievementProgress) {
    val colors = LocalNeon5KColors.current
    val def = progress.id
    val rank = progress.masteryRank

    val rankColor = when (rank) {
        MasteryRank.LOCKED -> colors.textMuted
        MasteryRank.BRONZE -> colors.primaryNeon
        MasteryRank.SILVER -> colors.secondaryNeon
        MasteryRank.GOLD -> colors.alertNeon
        MasteryRank.CYBER_PLATINUM -> colors.primaryNeon
    }

    val nextRankHint = when (rank) {
        MasteryRank.LOCKED -> "Déblocage : ${progress.progressSteps}/${def.targetSteps}"
        MasteryRank.BRONZE -> "Prochain rang ARGENT à ×${def.silverThreshold}"
        MasteryRank.SILVER -> "Prochain rang OR à ×${def.goldThreshold}"
        MasteryRank.GOLD -> "Prochain rang PLATINE à ×${def.platinumThreshold}"
        MasteryRank.CYBER_PLATINUM -> "RANG MAXIMAL ATTEINT ★"
    }

    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (progress.isUnlocked) rankColor.copy(alpha = 0.75f) else colors.textMuted.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PixelAchievementIconCanvas(
                achievementId = def,
                isUnlocked = progress.isUnlocked,
                masteryRank = rank,
                iconSize = 52.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = def.titleFr,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (progress.isUnlocked) colors.textPrimary else colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "+${def.playGamesXp} XP • ×${progress.lifetimeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isUnlocked) colors.alertNeon else colors.secondaryNeon
                    )
                }

                Text(
                    text = def.descriptionFr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )

                Text(
                    text = "[${rank.labelFr}] • $nextRankHint",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = rankColor
                )
            }
        }
    }
}

@Composable
private fun AiDatasetTabContent(
    decisionLogsCount: Int,
    exportStatusMessageFr: String?,
    onLaunchJsonl: () -> Unit,
    onLaunchCsv: () -> Unit,
    onClearDataset: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "DATASET D'ENTRAÎNEMENT IA",
                style = MaterialTheme.typography.labelLarge,
                color = colors.secondaryNeon
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background.copy(alpha = 0.6f), CyberCutShape)
                    .border(1.dp, colors.primaryNeon.copy(alpha = 0.35f), CyberCutShape)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$decisionLogsCount DÉCISIONS ENREGISTRÉES",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryNeon,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "Chaque sélection de dés (Humain ou Bot) est enregistrée avec son empreinte de règles (rulesFingerprint), l'état des scores, les dés tirés/gardés et l'issue finale du tour et de la partie.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted
            )

            if (exportStatusMessageFr != null) {
                Text(
                    text = exportStatusMessageFr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryNeon
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelButton(
                    text = "EXPORT .JSONL",
                    enabled = decisionLogsCount > 0,
                    onClick = onLaunchJsonl,
                    accentColor = colors.primaryNeon,
                    modifier = Modifier.weight(1f)
                )

                PixelButton(
                    text = "EXPORT .CSV",
                    enabled = decisionLogsCount > 0,
                    onClick = onLaunchCsv,
                    accentColor = colors.secondaryNeon,
                    modifier = Modifier.weight(1f)
                )
            }

            if (decisionLogsCount > 0) {
                PixelButton(
                    text = "PURGER LE JOURNAL DES DÉCISIONS",
                    onClick = onClearDataset,
                    filled = false,
                    accentColor = colors.alertNeon,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    CyberPanel(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.35f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun MiniComboBadge(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    Box(
        modifier = modifier
            .background(colors.background.copy(alpha = 0.6f), CyberCutShape)
            .border(
                width = 1.dp,
                color = if (count > 0) accentColor.copy(alpha = 0.6f) else colors.textMuted.copy(alpha = 0.2f),
                shape = CyberCutShape
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
            Text(
                text = "×$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) accentColor else colors.textMuted
            )
        }
    }
}

@Composable
private fun TelemetryKeyValueRow(
    label: String,
    value: String,
    valueColor: Color
) {
    val colors = LocalNeon5KColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background.copy(alpha = 0.55f), CyberCutShape)
            .border(1.dp, valueColor.copy(alpha = 0.20f), CyberCutShape)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1.15f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.85f)
        )
    }
}

