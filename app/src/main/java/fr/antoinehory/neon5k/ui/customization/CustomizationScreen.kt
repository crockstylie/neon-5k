package fr.antoinehory.neon5k.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.data.preferences.UserPreferences
import fr.antoinehory.neon5k.domain.ai.BotSpeed
import fr.antoinehory.neon5k.domain.model.DieState
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.CyberThemePreset
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import fr.antoinehory.neon5k.domain.theme.ThemePresetCategory
import fr.antoinehory.neon5k.ui.components.ColorPickerDialog
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.NeonSubScreenTopBar
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.components.PixelDiceCanvas
import fr.antoinehory.neon5k.ui.components.PixelIconId
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.util.Locale

private enum class EditableColorSlot(val labelFr: String) {
    BACKGROUND("Fond principal"),
    SURFACE("Surface panneaux"),
    PRIMARY("Accent Néon 1"),
    SECONDARY("Accent Néon 2"),
    ALERT("Alerte / Verrou"),
    TEXT("Texte principal")
}

/**
 * Studio de personnalisation de la fenêtre de jeu (Lots 2 & 4+) :
 * - En-tête harmonisé sur 1 ligne via [NeonSubScreenTopBar],
 * - Aperçu des 4 états de dés en haut suivi immédiatement du sélecteur de style de dés (*Pixel Solid*, *Neon Wireframe*, *Dot Matrix*),
 * - Générateurs de thèmes aléatoires (*Random Harmonieux* & *Random Chaos*),
 * - Palettes classées par familles (*Cyberpunk & Rétro*, *Niveaux de Gris 3 contrastes*, *Sépia 3 contrastes*, *Accessibilité Daltonisme*),
 * - Éditeur de couleurs sur-mesure (HSV / RGB + Random + `#RRGGBB` + ratio WCAG 2.1).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomizationScreen(
    preferences: UserPreferences,
    onSelectThemePreset: (CyberThemePreset) -> Unit,
    onUpdateCustomPalette: (CyberPalette) -> Unit,
    onSelectDiceStyle: (DiceVisualStyle) -> Unit,
    onToggleScanlines: (Boolean) -> Unit,
    onToggleLeftHanded: (Boolean) -> Unit,
    onToggleHaptic: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onSelectBotSpeed: (BotSpeed) -> Unit,
    onToggleShakeToRoll: (Boolean) -> Unit,
    onToggleKeepScreenOn: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNeon5KColors.current
    val activePalette = preferences.activePalette
    var editingSlot by remember { mutableStateOf<EditableColorSlot?>(null) }
    var previewRollTick by remember { mutableIntStateOf(0) }

    editingSlot?.let { slot ->
        val initialArgb = when (slot) {
            EditableColorSlot.BACKGROUND -> activePalette.backgroundArgb
            EditableColorSlot.SURFACE -> activePalette.surfaceArgb
            EditableColorSlot.PRIMARY -> activePalette.primaryArgb
            EditableColorSlot.SECONDARY -> activePalette.secondaryArgb
            EditableColorSlot.ALERT -> activePalette.alertArgb
            EditableColorSlot.TEXT -> activePalette.textArgb
        }
        ColorPickerDialog(
            title = slot.labelFr,
            initialArgb = initialArgb,
            referenceBackgroundArgb = activePalette.backgroundArgb,
            onDismiss = { editingSlot = null },
            onColorConfirmed = { newArgb ->
                val updated = when (slot) {
                    EditableColorSlot.BACKGROUND -> activePalette.copy(preset = CyberThemePreset.CUSTOM, backgroundArgb = newArgb)
                    EditableColorSlot.SURFACE -> activePalette.copy(preset = CyberThemePreset.CUSTOM, surfaceArgb = newArgb)
                    EditableColorSlot.PRIMARY -> activePalette.copy(preset = CyberThemePreset.CUSTOM, primaryArgb = newArgb)
                    EditableColorSlot.SECONDARY -> activePalette.copy(preset = CyberThemePreset.CUSTOM, secondaryArgb = newArgb)
                    EditableColorSlot.ALERT -> activePalette.copy(preset = CyberThemePreset.CUSTOM, alertArgb = newArgb)
                    EditableColorSlot.TEXT -> activePalette.copy(preset = CyberThemePreset.CUSTOM, textArgb = newArgb)
                }
                onUpdateCustomPalette(updated)
                editingSlot = null
            }
        )
    }

    val previewDice = remember {
        listOf(
            DieState(index = 0, face = 1, canScoreOnCurrentRoll = true),
            DieState(index = 1, face = 5, isSelected = true, canScoreOnCurrentRoll = true),
            DieState(index = 2, face = 6, isLocked = true),
            DieState(index = 3, face = 3, canScoreOnCurrentRoll = false)
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
        NeonSubScreenTopBar(
            title = "THÈMES & DÉS",
            icon = PixelIconId.PALETTE,
            onBack = onBack
        )

        // 1. Aperçu temps réel des 4 états de dés (toujours tout en haut)
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
                    text = "APERÇU EN DIRECT (SCORANT / SÉLECTIONNÉ / VERROUILLÉ / INACTIF)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryNeon,
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    previewDice.forEach { die ->
                        PixelDiceCanvas(
                            die = die,
                            rollTrigger = previewRollTick,
                            onClick = { previewRollTick++ },
                            dieSize = 58.dp,
                            styleOverride = preferences.diceStyle,
                            alwaysClickable = true
                        )
                    }
                }
                val contrast = activePalette.textOnBackgroundContrast
                val wcagPass = contrast >= 4.5
                Text(
                    text = String.format(
                        Locale.US,
                        "Contraste Texte/Fond : %.2f:1 %s",
                        contrast,
                        if (wcagPass) "(WCAG AA Conforme ✓)" else "(Attention : contraste < 4.5:1)"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (wcagPass) colors.primaryNeon else colors.alertNeon
                )
            }
        }

        // 2. Style visuel des dés (placé juste entre l'aperçu des dés et les palettes !)
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "STYLE DE RENDU DES DÉS (GRILLE 16×16)",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondaryNeon
                )
                Text(
                    text = "Changez de style en regardant l'aperçu juste au-dessus (touchez un dé pour tester sa rotation) :",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiceVisualStyle.entries.forEach { style ->
                        val isSelected = preferences.diceStyle == style
                        PixelButton(
                            text = style.labelFr,
                            icon = PixelIconId.DICE,
                            filled = isSelected,
                            accentColor = if (isSelected) colors.secondaryNeon else colors.textMuted,
                            onClick = { onSelectDiceStyle(style) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Générateur de Thème Aléatoire (Harmonieux vs Chaos Total) + Couleurs sur-mesure
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "GÉNÉRATEUR ALÉATOIRE & STUDIO CUSTOM",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelButton(
                        text = "HARMONIEUX",
                        icon = PixelIconId.SPARKLE,
                        filled = true,
                        accentColor = colors.primaryNeon,
                        onClick = { onUpdateCustomPalette(CyberPalette.randomHarmonious()) },
                        modifier = Modifier.weight(1f)
                    )
                    PixelButton(
                        text = "RANDOM CHAOS",
                        icon = PixelIconId.CHAOS,
                        filled = false,
                        accentColor = colors.alertNeon,
                        onClick = { onUpdateCustomPalette(CyberPalette.randomChaos()) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Ou touchez une pastille pour éditer la couleur (Mode HSV / RGB / #RRGGBB / Random) :",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorSlotChip(
                        label = EditableColorSlot.BACKGROUND.labelFr,
                        argb = activePalette.backgroundArgb,
                        onClick = { editingSlot = EditableColorSlot.BACKGROUND }
                    )
                    ColorSlotChip(
                        label = EditableColorSlot.SURFACE.labelFr,
                        argb = activePalette.surfaceArgb,
                        onClick = { editingSlot = EditableColorSlot.SURFACE }
                    )
                    ColorSlotChip(
                        label = EditableColorSlot.PRIMARY.labelFr,
                        argb = activePalette.primaryArgb,
                        onClick = { editingSlot = EditableColorSlot.PRIMARY }
                    )
                    ColorSlotChip(
                        label = EditableColorSlot.SECONDARY.labelFr,
                        argb = activePalette.secondaryArgb,
                        onClick = { editingSlot = EditableColorSlot.SECONDARY }
                    )
                    ColorSlotChip(
                        label = EditableColorSlot.ALERT.labelFr,
                        argb = activePalette.alertArgb,
                        onClick = { editingSlot = EditableColorSlot.ALERT }
                    )
                    ColorSlotChip(
                        label = EditableColorSlot.TEXT.labelFr,
                        argb = activePalette.textArgb,
                        onClick = { editingSlot = EditableColorSlot.TEXT }
                    )
                }
            }
        }

        // 4. Palettes Prédéfinies par catégorie (Cyberpunk, Niveaux de gris, Sépia, Daltonisme)
        val categoriesToDisplay = listOf(
            ThemePresetCategory.CYBER_RETRO,
            ThemePresetCategory.GRAYSCALE,
            ThemePresetCategory.SEPIA,
            ThemePresetCategory.DALTONISM
        )

        categoriesToDisplay.forEach { category ->
            val presetsInCategory = CyberThemePreset.entries.filter { it.category == category }
            CyberPanel(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = category.titleFr,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primaryNeon
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetsInCategory.forEach { preset ->
                            val isSelected = preferences.themePreset == preset
                            PixelButton(
                                text = preset.labelFr,
                                icon = PixelIconId.PALETTE,
                                filled = isSelected,
                                accentColor = if (isSelected) colors.primaryNeon else colors.textMuted,
                                onClick = { onSelectThemePreset(preset) }
                            )
                        }
                    }
                }
            }
        }

        // 5. Ergonomie, Scanlines, Haptique & Capteurs
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ERGONOMIE & SENSATIONS",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )

                PreferenceSwitchRow(
                    title = "Effet Scanlines CRT",
                    subtitle = "Trame horizontale rétro subtile sur le terminal.",
                    checked = preferences.showScanlines,
                    onCheckedChange = onToggleScanlines
                )

                PreferenceSwitchRow(
                    title = "Mode Gaucher (tenue à une main)",
                    subtitle = "Adapte l'alignement des commandes pour le pouce gauche.",
                    checked = preferences.leftHandedMode,
                    onCheckedChange = onToggleLeftHanded
                )

                PreferenceSwitchRow(
                    title = "Vibrations haptiques",
                    subtitle = "Retour tactile lors des lancers, sélections, mains pleines et coups nuls.",
                    checked = preferences.hapticEnabled,
                    onCheckedChange = onToggleHaptic
                )

                PreferenceSwitchRow(
                    title = "Effets sonores 8-bit rétro",
                    subtitle = "Synthèse audio chiptune légère (lancers, sélections, main pleine, banque, glitch).",
                    checked = preferences.soundEnabled,
                    onCheckedChange = onToggleSound
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Vitesse des Bots IA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Cadence d'exécution des tours IA (modifiable aussi en direct sur le plateau).",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BotSpeed.entries.forEach { speed ->
                            val isSelected = preferences.botSpeed == speed
                            PixelButton(
                                text = speed.labelFr,
                                filled = isSelected,
                                accentColor = if (isSelected) colors.secondaryNeon else colors.textMuted,
                                onClick = { onSelectBotSpeed(speed) }
                            )
                        }
                    }
                }

                PreferenceSwitchRow(
                    title = "Secouer l'appareil pour lancer",
                    subtitle = "Permet de déclencher un lancer par une secousse physique de l'accéléromètre.",
                    checked = preferences.shakeToRollEnabled,
                    onCheckedChange = onToggleShakeToRoll
                )

                PreferenceSwitchRow(
                    title = "Garder l'écran allumé en partie",
                    subtitle = "Empêche la mise en veille pendant une partie sur table (Pass & Play).",
                    checked = preferences.keepScreenOn,
                    onCheckedChange = onToggleKeepScreenOn
                )
            }
        }
    }
}

@Composable
private fun ColorSlotChip(
    label: String,
    argb: Long,
    onClick: () -> Unit
) {
    val colors = LocalNeon5KColors.current
    val hex = String.format(Locale.US, "#%06X", argb and 0xFFFFFFL)
    Row(
        modifier = Modifier
            .background(colors.background, CyberCutShape)
            .border(1.dp, colors.primaryNeon.copy(alpha = 0.4f), CyberCutShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(Color(argb), CyberCutShape)
                .border(1.dp, colors.textPrimary.copy(alpha = 0.5f), CyberCutShape)
        )
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.textPrimary)
            Text(text = hex, style = MaterialTheme.typography.labelSmall, color = colors.secondaryNeon)
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
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
