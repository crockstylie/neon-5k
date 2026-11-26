package fr.antoinehory.neon5k.ui.stats

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.data.export.DatasetExportFormat
import fr.antoinehory.neon5k.data.local.entity.PlayerStatsEntity
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.util.Locale

/**
 * Écran des statistiques locales cumulées et de l'export SAF (`.jsonl` / `.csv`) du dataset de décisions IA (Lot 2).
 */
@Composable
fun StatsScreen(
    stats: PlayerStatsEntity,
    decisionLogsCount: Int,
    exportStatusMessageFr: String?,
    onExportDataset: (uri: Uri, format: DatasetExportFormat) -> Unit,
    onClearDataset: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current

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

    val winRatePercent = if (stats.matchesPlayed > 0) {
        (stats.matchesWon * 100.0) / stats.matchesPlayed.toDouble()
    } else {
        0.0
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
                text = "STATS & DATASET IA",
                style = MaterialTheme.typography.titleLarge,
                color = colors.primaryNeon
            )
        }

        // Section 1 : Statistiques de jeu locales
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "PERFORMANCES LOCALES (JOUEUR HUMAIN)",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMetricCard(
                        label = "GLITCHS / CRASHS",
                        value = stats.totalBustsCount.toString(),
                        accentColor = colors.secondaryNeon,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "RECORD REPIQUAGE",
                        value = "${stats.biggestPiggybackScore} PTS",
                        accentColor = colors.alertNeon,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Section 2 : Journal des décisions IA & Export SAF (Zéro permission stockage)
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.secondaryNeon.copy(alpha = 0.55f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DATASET D'ENTRAÎNEMENT IA",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.secondaryNeon
                    )
                    Text(
                        text = "$decisionLogsCount DÉCISIONS",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryNeon
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
                        text = "EXPORTER .JSONL",
                        enabled = decisionLogsCount > 0,
                        onClick = {
                            jsonlLauncher.launch("neon5k_ai_dataset_${System.currentTimeMillis()}.jsonl")
                        },
                        accentColor = colors.primaryNeon,
                        modifier = Modifier.weight(1f)
                    )

                    PixelButton(
                        text = "EXPORTER .CSV",
                        enabled = decisionLogsCount > 0,
                        onClick = {
                            csvLauncher.launch("neon5k_ai_dataset_${System.currentTimeMillis()}.csv")
                        },
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
}

@Composable
private fun StatMetricCard(
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
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
