package fr.antoinehory.neon5k.ui.stats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.data.export.DatasetExportFormat
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.data.local.entity.SavedMatchEntity
import fr.antoinehory.neon5k.data.playgames.PlayGamesStatus
import fr.antoinehory.neon5k.domain.achievement.AchievementProgress
import fr.antoinehory.neon5k.domain.achievement.MasteryRank
import fr.antoinehory.neon5k.domain.ai.AdaptiveLearningSummary
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.NeonSubScreenTopBar
import fr.antoinehory.neon5k.ui.components.PixelAchievementIconCanvas
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelIconId
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class StatsTab(val labelFr: String) {
    TELEMETRY("STATS"),
    MATCH_HISTORY("PARTIES"),
    ACHIEVEMENTS("SUCCÈS"),
    AI_DATASET("IA & DATA")
}

/**
 * Écran unifié de Télémétrie Fun (avec graphiques Canvas : Camemberts/Donuts, Barres, Courbes),
 * d'Historique complet des Parties & Lancers, des 20 Succès à Compteurs Infinis (Play Jeux v2)
 * et d'Auto-Apprentissage Local + Export SAF du Dataset IA.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsScreen(
    stats: PlayerStatsEntity,
    finishedMatches: List<SavedMatchEntity>,
    adaptiveLearning: AdaptiveLearningSummary,
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
        NeonSubScreenTopBar(
            title = "STATS & SUCCÈS",
            icon = PixelIconId.TROPHY,
            onBack = onBack
        )

        // Barre d'onglets (Stats | Parties | Succès | IA & Data)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
            StatsTab.TELEMETRY -> FunTelemetryTabContent(
                stats = stats,
                finishedMatches = finishedMatches
            )
            StatsTab.MATCH_HISTORY -> MatchHistoryTabContent(
                finishedMatches = finishedMatches
            )
            StatsTab.ACHIEVEMENTS -> AchievementsTabContent(
                achievements = achievements,
                playGamesStatus = playGamesStatus,
                statusMessageFr = exportStatusMessageFr,
                onOpenPlayGamesAchievements = onOpenPlayGamesAchievements
            )
            StatsTab.AI_DATASET -> AiDatasetTabContent(
                decisionLogsCount = decisionLogsCount,
                adaptiveLearning = adaptiveLearning,
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
private fun FunTelemetryTabContent(
    stats: PlayerStatsEntity,
    finishedMatches: List<SavedMatchEntity>
) {
    val colors = LocalNeon5KColors.current
    val winRatePercent = if (stats.matchesPlayed > 0) {
        (stats.matchesWon * 100.0) / stats.matchesPlayed.toDouble()
    } else {
        0.0
    }
    val lossesCount = (stats.matchesPlayed - stats.matchesWon).coerceAtLeast(0)

    // 1. Bilan général + Camemberts / Donuts (Victoires vs Défaites & Points Banqués vs Glitchs)
    CyberPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "BILAN GÉNÉRAL & CAMEMBERTS DE PERFORMANCE",
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

            // Double Camembert / Donut Chart : Taux de Victoire & Efficacité de Banque
            val totalPointsGenerated = (stats.totalPointsBanked + stats.totalPointsLostToBusts).coerceAtLeast(1L)
            val bankEfficiencyPercent = if (stats.totalPointsBanked + stats.totalPointsLostToBusts > 0L) {
                (stats.totalPointsBanked * 100.0) / totalPointsGenerated.toDouble()
            } else {
                100.0
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CyberDonutChartCard(
                    title = "VICTOIRES / DÉFAITES",
                    centerText = "${String.format(Locale.US, "%.0f", winRatePercent)}%",
                    primaryLabel = "${stats.matchesWon} V",
                    secondaryLabel = "$lossesCount D",
                    primaryValue = stats.matchesWon.toFloat(),
                    secondaryValue = lossesCount.toFloat(),
                    primaryColor = colors.primaryNeon,
                    secondaryColor = colors.alertNeon,
                    modifier = Modifier.weight(1f)
                )

                CyberDonutChartCard(
                    title = "BANQUÉS / GLITCHÉS",
                    centerText = "${String.format(Locale.US, "%.0f", bankEfficiencyPercent)}%",
                    primaryLabel = "${stats.totalPointsBanked} pts",
                    secondaryLabel = "${stats.totalPointsLostToBusts} pts",
                    primaryValue = stats.totalPointsBanked.toFloat(),
                    secondaryValue = stats.totalPointsLostToBusts.toFloat(),
                    primaryColor = colors.secondaryNeon,
                    secondaryColor = colors.alertNeon,
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

    // 2. Courbe d'évolution des scores des dernières parties
    val recentHumanScores = finishedMatches
        .take(15)
        .reversed()
        .mapNotNull { entity ->
            val players = entity.decodedPlayers()
            players.firstOrNull { it.type == fr.antoinehory.neon5k.domain.model.PlayerType.HUMAN }?.score
                ?: players.firstOrNull()?.score
        }

    if (recentHumanScores.size >= 2) {
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.primaryNeon.copy(alpha = 0.65f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "COURBE DES SCORES FINAUX (${recentHumanScores.size} DERNIÈRES PARTIES)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                CyberLineChart(
                    points = recentHumanScores.map { it.toFloat() },
                    lineColor = colors.primaryNeon,
                    pointColor = colors.alertNeon,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }
    }

    // 3. Collection de Brelans (Graphique en Barres + Badges) & Combinaisons Spéciales
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "BRELANS & COMBINAISONS (GRAPHIQUE EN BARRES)",
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

            // Graphique en barres verticales des Brelans par face 1..6
            val tripleBars = listOf(
                BarChartItem("3×1", stats.triple1Count.toFloat(), "×${stats.triple1Count}", colors.alertNeon),
                BarChartItem("3×2", stats.triple2Count.toFloat(), "×${stats.triple2Count}", colors.primaryNeon),
                BarChartItem("3×3", stats.triple3Count.toFloat(), "×${stats.triple3Count}", colors.primaryNeon),
                BarChartItem("3×4", stats.triple4Count.toFloat(), "×${stats.triple4Count}", colors.primaryNeon),
                BarChartItem("3×5", stats.triple5Count.toFloat(), "×${stats.triple5Count}", colors.secondaryNeon),
                BarChartItem("3×6", stats.triple6Count.toFloat(), "×${stats.triple6Count}", colors.primaryNeon)
            )
            CyberVerticalBarChart(
                items = tripleBars,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            TelemetryKeyValueRow("Carrés (4 dés identiques)", "×${stats.fourOfAKindCount}", colors.primaryNeon)
            TelemetryKeyValueRow("Quintes / God Rolls (5 identiques)", "×${stats.fiveOfAKindCount}", colors.alertNeon)
            TelemetryKeyValueRow("Suites (1-5, 2-6 ou 1-6)", "×${stats.straightCount}", colors.secondaryNeon)
            TelemetryKeyValueRow("3 Paires (variante 6 dés)", "×${stats.threePairsCount}", colors.primaryNeon)
            TelemetryKeyValueRow("Fulls avec bonus (Brelan + Paire)", "×${stats.fullHouseCount}", colors.secondaryNeon)
            TelemetryKeyValueRow("As isolés / Cinq isolés tirés", "${stats.singleOnesCount} As • ${stats.singleFivesCount} Cinq", colors.textPrimary)
        }
    }

    // 4. Psychologie, Prise de Risque & Paliers
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

    // 5. Entropie Matérielle SHA-256 & Graphique en Barres d'équité des faces 1..6
    CyberPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AUDIT D'ÉQUITÉ SHA-256 & DISTRIBUTION DES FACES",
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

            Text(
                text = "GRAPHIQUE DES FACES 1 À 6 (LIGNE THÉORIQUE : 16.7 %)",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryNeon
            )

            val faceBarItems = faceCounts.map { (face, count) ->
                val pct = if (stats.totalIndividualDiceThrown > 0) {
                    (count * 100.0) / totalFaces.toDouble()
                } else {
                    0.0
                }
                BarChartItem(
                    label = "[$face]",
                    value = count.toFloat(),
                    topCaption = "${String.format(Locale.US, "%.1f", pct)}%",
                    color = if (face == 1 || face == 5) colors.primaryNeon else colors.secondaryNeon
                )
            }

            CyberVerticalBarChart(
                items = faceBarItems,
                targetReferenceValue = if (stats.totalIndividualDiceThrown > 0) totalFaces / 6f else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(135.dp)
            )
        }
    }
}

/**
 * Onglet "PARTIES" : journal complet de toutes les parties terminées avec scores finaux,
 * paliers d'encoches (`✕`) et historique dépliable lancer par lancer.
 */
@Composable
private fun MatchHistoryTabContent(
    finishedMatches: List<SavedMatchEntity>
) {
    val colors = LocalNeon5KColors.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.primaryNeon.copy(alpha = 0.65f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "JOURNAL DES PARTIES TERMINÉES (${finishedMatches.size})",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon
            )
            Text(
                text = "Touchez une partie pour déplier son classement complet, l'historique des paliers (encoches ✕) et le journal détaillé de tous ses lancers.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }
    }

    if (finishedMatches.isEmpty()) {
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Aucune partie terminée enregistrée pour le moment. Terminez une partie jusqu'à la cible pour consulter son journal détaillé ici !",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted
            )
        }
    } else {
        finishedMatches.forEach { entity ->
            FinishedMatchHistoryCard(
                entity = entity,
                formattedDate = dateFormat.format(Date(entity.updatedAtEpochMs))
            )
        }
    }
}

@Composable
private fun FinishedMatchHistoryCard(
    entity: SavedMatchEntity,
    formattedDate: String
) {
    val colors = LocalNeon5KColors.current
    var expanded by rememberSaveable(entity.matchUuid) { mutableStateOf(false) }
    val players = entity.decodedPlayers()
    val rules = entity.decodedRules()
    val winner = players.firstOrNull { it.id == entity.winnerPlayerId }
        ?: players.maxByOrNull { it.score }
    val rollHistory = entity.decodedRollHistory()

    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        borderColor = if (expanded) colors.primaryNeon else colors.secondaryNeon.copy(alpha = 0.45f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VAINQUEUR : ${winner?.name ?: "INCONNU"} (${winner?.score ?: 0} PTS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryNeon
                    )
                    Text(
                        text = "$formattedDate • Cible ${rules.targetScore} pts • ${entity.turnNumber} tours • ${entity.totalRollsInMatch} lancers",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                Text(
                    text = if (expanded) "▲ REPLIER" else "▼ DÉTAILS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.secondaryNeon
                )
            }

            // Résumé des scores des joueurs (FlowRow responsive anti-tassement)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                players.sortedByDescending { it.score }.forEach { player ->
                    val isWinner = player.id == winner?.id
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isWinner) colors.primaryNeon.copy(alpha = 0.15f) else colors.surface.copy(alpha = 0.5f),
                                shape = CyberCutShape
                            )
                            .border(
                                width = 1.dp,
                                color = if (isWinner) colors.primaryNeon.copy(alpha = 0.7f) else colors.secondaryNeon.copy(alpha = 0.3f),
                                shape = CyberCutShape
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${player.name}: ${player.score} pts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            color = if (isWinner) colors.primaryNeon else colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            if (expanded) {
                // 1. Paliers et encoches de chaque joueur à la fin de la partie
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background.copy(alpha = 0.65f), CyberCutShape)
                        .border(1.dp, colors.secondaryNeon.copy(alpha = 0.35f), CyberCutShape)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CLASSEMENT & HISTORIQUE DES PALIERS (ENCOCHES ✕)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.secondaryNeon
                    )
                    players.sortedByDescending { it.score }.forEachIndexed { rank, player ->
                        val timeline = player.normalizedCheckpoints().joinToString(" → ") { cp ->
                            val marks = "✕".repeat(cp.strikes.coerceIn(0, 3)) + "·".repeat((3 - cp.strikes).coerceIn(0, 3))
                            "${cp.score}[$marks]"
                        }
                        Text(
                            text = "#${rank + 1} ${player.name} : ${player.score} pts  |  $timeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rank == 0) colors.primaryNeon else colors.textPrimary
                        )
                    }
                }

                // 2. Journal chronologique des lancers de la partie
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background.copy(alpha = 0.65f), CyberCutShape)
                        .border(1.dp, colors.primaryNeon.copy(alpha = 0.35f), CyberCutShape)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "JOURNAL DES LANCERS (${rollHistory.size} ENTRÉES)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryNeon
                    )

                    if (rollHistory.isEmpty()) {
                        Text(
                            text = "Aucun lancer détaillé archivé pour cette partie.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    } else {
                        rollHistory.takeLast(60).reversed().forEach { entry ->
                            val facesText = if (entry.rolledFaces.isEmpty()) "" else " " + entry.rolledFaces.joinToString("") { "[$it]" }
                            val marks = "✕".repeat(entry.playerStrikesAfter.coerceIn(0, 3)) +
                                "·".repeat((3 - entry.playerStrikesAfter).coerceIn(0, 3))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "T#${entry.turnNumber}.${entry.rollNumber} ${entry.playerName}$facesText",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (entry.isBust) colors.alertNeon else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${entry.summaryFr} [$marks]",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        entry.isBust -> colors.alertNeon
                                        entry.isBank || entry.isHotDice -> colors.secondaryNeon
                                        else -> colors.primaryNeon
                                    }
                                )
                            }
                        }
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
    adaptiveLearning: AdaptiveLearningSummary,
    exportStatusMessageFr: String?,
    onLaunchJsonl: () -> Unit,
    onLaunchCsv: () -> Unit,
    onClearDataset: () -> Unit
) {
    val colors = LocalNeon5KColors.current

    // 1. Auto-Apprentissage Local de l'IA (Reinforcement Learning Tabulaire Embarqué)
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.primaryNeon.copy(alpha = 0.75f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "AUTO-APPRENTISSAGE LOCAL DE L'IA (ACTIF)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.primaryNeon
            )
            Text(
                text = "Les bots locaux se nourrissent automatiquement en temps réel du journal de décisions (decision_logs) : ils hybrident l'espérance mathématique exacte (6^N) avec le rendement empirique observé sur vos parties.",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )

            TelemetryKeyValueRow(
                label = "Échantillons résolus assimilés par l'IA",
                value = "${adaptiveLearning.completedSamplesCount} décisions",
                valueColor = colors.primaryNeon
            )
            TelemetryKeyValueRow(
                label = "Cellules d'états apprises (dés × pot × phase)",
                value = "${adaptiveLearning.activeStateBucketsCount} états actifs",
                valueColor = colors.secondaryNeon
            )
            TelemetryKeyValueRow(
                label = "Gain net empirique moyen observé post-relance",
                value = "${adaptiveLearning.averageEmpiricalRollOutcome} pts",
                valueColor = colors.alertNeon
            )
            TelemetryKeyValueRow(
                label = "Poids d'hybridation empirique IA (confiance)",
                value = "${adaptiveLearning.learnedConfidencePercent} %",
                valueColor = colors.primaryNeon
            )
        }
    }

    // 2. Export SAF du Dataset IA
    CyberPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "EXPORT SAF DU DATASET D'ENTRAÎNEMENT IA",
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

private data class BarChartItem(
    val label: String,
    val value: Float,
    val topCaption: String,
    val color: Color
)

/**
 * Camembert / Donut Chart vectoriel Cyberpunk en Compose Canvas.
 */
@Composable
private fun CyberDonutChartCard(
    title: String,
    centerText: String,
    primaryLabel: String,
    secondaryLabel: String,
    primaryValue: Float,
    secondaryValue: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val total = (primaryValue + secondaryValue).coerceAtLeast(1f)
    val primarySweep = if (primaryValue + secondaryValue > 0f) {
        (primaryValue / total) * 360f
    } else {
        180f
    }

    Column(
        modifier = modifier
            .background(colors.background.copy(alpha = 0.6f), CyberCutShape)
            .border(1.dp, primaryColor.copy(alpha = 0.35f), CyberCutShape)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier.size(86.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                val strokeWidth = 11.dp.toPx()
                drawArc(
                    color = secondaryColor.copy(alpha = 0.75f),
                    startAngle = -90f + primarySweep,
                    sweepAngle = 360f - primarySweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = primarySweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
            Text(
                text = centerText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "■ $primaryLabel",
                style = MaterialTheme.typography.labelSmall,
                color = primaryColor
            )
            Text(
                text = "■ $secondaryLabel",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor
            )
        }
    }
}

/**
 * Graphique en barres verticales Cyberpunk avec ligne de référence théorique optionnelle.
 */
@Composable
private fun CyberVerticalBarChart(
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    targetReferenceValue: Float? = null
) {
    val colors = LocalNeon5KColors.current
    val maxVal = (items.maxOfOrNull { it.value } ?: 1f)
        .coerceAtLeast(targetReferenceValue ?: 1f)
        .coerceAtLeast(1f)

    Column(
        modifier = modifier
            .background(colors.background.copy(alpha = 0.6f), CyberCutShape)
            .border(1.dp, colors.primaryNeon.copy(alpha = 0.25f), CyberCutShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                Text(
                    text = item.topCaption,
                    style = MaterialTheme.typography.labelSmall,
                    color = item.color,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val count = items.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.52f).coerceAtLeast(8f)

            if (targetReferenceValue != null && targetReferenceValue > 0f) {
                val refRatio = (targetReferenceValue / maxVal).coerceIn(0f, 1f)
                val refY = size.height * (1f - refRatio)
                drawLine(
                    color = colors.alertNeon.copy(alpha = 0.75f),
                    start = Offset(0f, refY),
                    end = Offset(size.width, refY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            }

            items.forEachIndexed { index, item ->
                val ratio = (item.value / maxVal).coerceIn(0.04f, 1f)
                val barHeight = size.height * ratio
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = item.color.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Courbe d'évolution (Sparkline) Cyberpunk en Compose Canvas.
 */
@Composable
private fun CyberLineChart(
    points: List<Float>,
    lineColor: Color,
    pointColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val minVal = (points.minOrNull() ?: 0f).coerceAtMost(0f)
    val maxVal = (points.maxOrNull() ?: 5000f).coerceAtLeast(1000f)
    val span = (maxVal - minVal).coerceAtLeast(1f)

    Column(
        modifier = modifier
            .background(colors.background.copy(alpha = 0.6f), CyberCutShape)
            .border(1.dp, lineColor.copy(alpha = 0.35f), CyberCutShape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (points.size < 2) return@Canvas
            val stepX = size.width / (points.size - 1).toFloat()
            val path = Path()

            points.forEachIndexed { idx, value ->
                val x = idx * stepX
                val normalized = ((value - minVal) / span).coerceIn(0f, 1f)
                val y = size.height * (1f - normalized)
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEachIndexed { idx, value ->
                val x = idx * stepX
                val normalized = ((value - minVal) / span).coerceIn(0f, 1f)
                val y = size.height * (1f - normalized)
                drawCircle(
                    color = pointColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min : ${points.minOrNull()?.toInt() ?: 0} pts",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
            Text(
                text = "Dernier : ${points.lastOrNull()?.toInt() ?: 0} pts",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = lineColor
            )
            Text(
                text = "Max : ${points.maxOrNull()?.toInt() ?: 0} pts",
                style = MaterialTheme.typography.labelSmall,
                color = pointColor
            )
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
