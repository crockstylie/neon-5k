package fr.antoinehory.neon5k.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.antoinehory.neon5k.domain.ai.BotSpeed
import fr.antoinehory.neon5k.domain.rules.BumpOpponentMode
import fr.antoinehory.neon5k.domain.rules.FiveOfAKindMode
import fr.antoinehory.neon5k.domain.rules.FourOfAKindMode
import fr.antoinehory.neon5k.domain.rules.GameRulesConfig
import fr.antoinehory.neon5k.domain.rules.RulesPreset
import fr.antoinehory.neon5k.domain.rules.ThreeStrikesMode
import fr.antoinehory.neon5k.domain.theme.CyberPalette
import fr.antoinehory.neon5k.domain.theme.CyberThemePreset
import fr.antoinehory.neon5k.domain.theme.DiceVisualStyle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.neon5kDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "neon5k_user_preferences"
)

/**
 * Modèle immuable de l'ensemble des préférences visuelles, ergonomiques et de règles favorites de l'utilisateur.
 */
data class UserPreferences(
    val themePreset: CyberThemePreset = CyberThemePreset.NEON_NIGHT,
    val customPalette: CyberPalette = CyberPalette.NEON_NIGHT.copy(preset = CyberThemePreset.CUSTOM),
    val diceStyle: DiceVisualStyle = DiceVisualStyle.PIXEL_SOLID,
    val showScanlines: Boolean = true,
    val leftHandedMode: Boolean = false,
    val hapticEnabled: Boolean = true,
    val shakeToRollEnabled: Boolean = true,
    val keepScreenOn: Boolean = true,
    val soundEnabled: Boolean = true,
    val botSpeed: BotSpeed = BotSpeed.NORMAL,
    val hasCompletedFirstRollTutorial: Boolean = false,
    val shareAnonymousAiLogs: Boolean = false,
    val favoriteRules: GameRulesConfig = GameRulesConfig.CLASSIC_5_DICE
) {
    /** Palette active résolue (soit le preset choisi, soit la palette custom de l'utilisateur). */
    val activePalette: CyberPalette
        get() = if (themePreset == CyberThemePreset.CUSTOM) {
            customPalette
        } else {
            CyberPalette.fromPreset(themePreset)
        }
}

/**
 * Repository DataStore gérant la persistance réactive du thème, des couleurs sur-mesure,
 * du style de dés, des options ergonomiques et des règles favorites.
 */
class UserPreferencesRepository(
    context: Context
) {
    private val dataStore = context.applicationContext.neon5kDataStore

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val themePreset = prefs[KEY_THEME_PRESET]
            ?.let { runCatching { CyberThemePreset.valueOf(it) }.getOrNull() }
            ?: CyberThemePreset.NEON_NIGHT

        val defaultCustom = CyberPalette.NEON_NIGHT
        val customPalette = CyberPalette(
            preset = CyberThemePreset.CUSTOM,
            backgroundArgb = prefs[KEY_CUSTOM_BG] ?: defaultCustom.backgroundArgb,
            surfaceArgb = prefs[KEY_CUSTOM_SURFACE] ?: defaultCustom.surfaceArgb,
            primaryArgb = prefs[KEY_CUSTOM_PRIMARY] ?: defaultCustom.primaryArgb,
            secondaryArgb = prefs[KEY_CUSTOM_SECONDARY] ?: defaultCustom.secondaryArgb,
            alertArgb = prefs[KEY_CUSTOM_ALERT] ?: defaultCustom.alertArgb,
            textArgb = prefs[KEY_CUSTOM_TEXT] ?: defaultCustom.textArgb
        )

        val diceStyle = prefs[KEY_DICE_STYLE]
            ?.let { runCatching { DiceVisualStyle.valueOf(it) }.getOrNull() }
            ?: DiceVisualStyle.PIXEL_SOLID

        val rawFiveKind = prefs[KEY_RULES_FIVE_KIND]
            ?.let { runCatching { FiveOfAKindMode.valueOf(it) }.getOrNull() }
            ?: FiveOfAKindMode.QUAD_TRIPLE

        val favoriteRules = GameRulesConfig(
            preset = prefs[KEY_RULES_PRESET]
                ?.let { runCatching { RulesPreset.valueOf(it) }.getOrNull() }
                ?: RulesPreset.CLASSIC_5_DICE,
            diceCount = (prefs[KEY_RULES_DICE_COUNT] ?: 5).coerceIn(5, 6),
            targetScore = prefs[KEY_RULES_TARGET_SCORE] ?: 5000,
            openingThreshold = prefs[KEY_RULES_OPENING] ?: 750,
            minTurnScoreAfterOpening = prefs[KEY_RULES_MIN_TURN_AFTER_OPENING] ?: 0,
            requireMultipleOf100ToBank = prefs[KEY_RULES_REQUIRE_ROUND_100] ?: false,
            finalSprintLast1000InOneTurn = prefs[KEY_RULES_FINAL_SPRINT_1000] ?: false,
            exactFinishRequired = prefs[KEY_RULES_EXACT_FINISH] ?: true,
            mandatoryRerollOnHotDice = prefs[KEY_RULES_HOT_DICE_MANDATORY] ?: true,
            fourOfAKindMode = prefs[KEY_RULES_FOUR_KIND]
                ?.let { runCatching { FourOfAKindMode.valueOf(it) }.getOrNull() }
                ?: FourOfAKindMode.DOUBLE_TRIPLE,
            fiveOfAKindMode = if (rawFiveKind == FiveOfAKindMode.INSTANT_WIN_ON_ONES) {
                FiveOfAKindMode.QUAD_TRIPLE
            } else {
                rawFiveKind
            },
            instantWinOnFiveOnes = prefs[KEY_RULES_INSTANT_WIN_FIVE_ONES]
                ?: (rawFiveKind == FiveOfAKindMode.INSTANT_WIN_ON_ONES),
            straightScore = prefs[KEY_RULES_STRAIGHT] ?: 1500,
            fullStraight6DiceScore = prefs[KEY_RULES_FULL_STRAIGHT_6] ?: 2500,
            threePairsScore = prefs[KEY_RULES_THREE_PAIRS] ?: 1000,
            fullHouseBonus = prefs[KEY_RULES_FULL_HOUSE] ?: 0,
            piggybackEnabled = prefs[KEY_RULES_PIGGYBACK] ?: false,
            bumpOpponentMode = prefs[KEY_RULES_BUMP_MODE]
                ?.let { runCatching { BumpOpponentMode.valueOf(it) }.getOrNull() }
                ?: BumpOpponentMode.DISABLED,
            threeStrikesMode = prefs[KEY_RULES_THREE_STRIKES_MODE]
                ?.let { runCatching { ThreeStrikesMode.valueOf(it) }.getOrNull() }
                ?: ThreeStrikesMode.DISABLED
        )

        val botSpeed = prefs[KEY_BOT_SPEED]
            ?.let { runCatching { BotSpeed.valueOf(it) }.getOrNull() }
            ?: BotSpeed.NORMAL

        UserPreferences(
            themePreset = themePreset,
            customPalette = customPalette,
            diceStyle = diceStyle,
            showScanlines = prefs[KEY_SHOW_SCANLINES] ?: true,
            leftHandedMode = prefs[KEY_LEFT_HANDED] ?: false,
            hapticEnabled = prefs[KEY_HAPTIC_ENABLED] ?: true,
            shakeToRollEnabled = prefs[KEY_SHAKE_TO_ROLL] ?: true,
            keepScreenOn = prefs[KEY_KEEP_SCREEN_ON] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            botSpeed = botSpeed,
            hasCompletedFirstRollTutorial = prefs[KEY_FIRST_ROLL_TUTORIAL_DONE] ?: false,
            shareAnonymousAiLogs = prefs[KEY_SHARE_AI_LOGS] ?: false,
            favoriteRules = favoriteRules
        )
    }

    suspend fun setThemePreset(preset: CyberThemePreset) {
        dataStore.edit { it[KEY_THEME_PRESET] = preset.name }
    }

    suspend fun updateCustomPalette(palette: CyberPalette) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_PRESET] = CyberThemePreset.CUSTOM.name
            prefs[KEY_CUSTOM_BG] = palette.backgroundArgb
            prefs[KEY_CUSTOM_SURFACE] = palette.surfaceArgb
            prefs[KEY_CUSTOM_PRIMARY] = palette.primaryArgb
            prefs[KEY_CUSTOM_SECONDARY] = palette.secondaryArgb
            prefs[KEY_CUSTOM_ALERT] = palette.alertArgb
            prefs[KEY_CUSTOM_TEXT] = palette.textArgb
        }
    }

    suspend fun setDiceStyle(style: DiceVisualStyle) {
        dataStore.edit { it[KEY_DICE_STYLE] = style.name }
    }

    suspend fun setShowScanlines(enabled: Boolean) {
        dataStore.edit { it[KEY_SHOW_SCANLINES] = enabled }
    }

    suspend fun setLeftHandedMode(enabled: Boolean) {
        dataStore.edit { it[KEY_LEFT_HANDED] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_HAPTIC_ENABLED] = enabled }
    }

    suspend fun setShakeToRollEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SHAKE_TO_ROLL] = enabled }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        dataStore.edit { it[KEY_KEEP_SCREEN_ON] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setBotSpeed(speed: BotSpeed) {
        dataStore.edit { it[KEY_BOT_SPEED] = speed.name }
    }

    suspend fun setFirstRollTutorialCompleted(completed: Boolean = true) {
        dataStore.edit { it[KEY_FIRST_ROLL_TUTORIAL_DONE] = completed }
    }

    suspend fun setShareAnonymousAiLogs(enabled: Boolean) {
        dataStore.edit { it[KEY_SHARE_AI_LOGS] = enabled }
    }

    suspend fun saveFavoriteRules(rules: GameRulesConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_RULES_PRESET] = rules.preset.name
            prefs[KEY_RULES_DICE_COUNT] = rules.diceCount
            prefs[KEY_RULES_TARGET_SCORE] = rules.targetScore
            prefs[KEY_RULES_OPENING] = rules.openingThreshold
            prefs[KEY_RULES_MIN_TURN_AFTER_OPENING] = rules.minTurnScoreAfterOpening
            prefs[KEY_RULES_REQUIRE_ROUND_100] = rules.requireMultipleOf100ToBank
            prefs[KEY_RULES_FINAL_SPRINT_1000] = rules.finalSprintLast1000InOneTurn
            prefs[KEY_RULES_EXACT_FINISH] = rules.exactFinishRequired
            prefs[KEY_RULES_HOT_DICE_MANDATORY] = rules.mandatoryRerollOnHotDice
            prefs[KEY_RULES_FOUR_KIND] = rules.fourOfAKindMode.name
            prefs[KEY_RULES_FIVE_KIND] = rules.fiveOfAKindMode.name
            prefs[KEY_RULES_INSTANT_WIN_FIVE_ONES] = rules.instantWinOnFiveOnes
            prefs[KEY_RULES_STRAIGHT] = rules.straightScore
            prefs[KEY_RULES_FULL_STRAIGHT_6] = rules.fullStraight6DiceScore
            prefs[KEY_RULES_THREE_PAIRS] = rules.threePairsScore
            prefs[KEY_RULES_FULL_HOUSE] = rules.fullHouseBonus
            prefs[KEY_RULES_PIGGYBACK] = rules.piggybackEnabled
            prefs[KEY_RULES_BUMP_MODE] = rules.bumpOpponentMode.name
            prefs[KEY_RULES_THREE_STRIKES_MODE] = rules.threeStrikesMode.name
        }
    }

    companion object {
        private val KEY_THEME_PRESET = stringPreferencesKey("theme_preset")
        private val KEY_CUSTOM_BG = longPreferencesKey("custom_bg_argb")
        private val KEY_CUSTOM_SURFACE = longPreferencesKey("custom_surface_argb")
        private val KEY_CUSTOM_PRIMARY = longPreferencesKey("custom_primary_argb")
        private val KEY_CUSTOM_SECONDARY = longPreferencesKey("custom_secondary_argb")
        private val KEY_CUSTOM_ALERT = longPreferencesKey("custom_alert_argb")
        private val KEY_CUSTOM_TEXT = longPreferencesKey("custom_text_argb")
        private val KEY_DICE_STYLE = stringPreferencesKey("dice_style")
        private val KEY_SHOW_SCANLINES = booleanPreferencesKey("show_scanlines")
        private val KEY_LEFT_HANDED = booleanPreferencesKey("left_handed_mode")
        private val KEY_HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        private val KEY_SHAKE_TO_ROLL = booleanPreferencesKey("shake_to_roll")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        private val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        private val KEY_BOT_SPEED = stringPreferencesKey("bot_speed")
        private val KEY_FIRST_ROLL_TUTORIAL_DONE = booleanPreferencesKey("first_roll_tutorial_done")
        private val KEY_SHARE_AI_LOGS = booleanPreferencesKey("share_anonymous_ai_logs")

        private val KEY_RULES_PRESET = stringPreferencesKey("rules_preset")
        private val KEY_RULES_DICE_COUNT = intPreferencesKey("rules_dice_count")
        private val KEY_RULES_TARGET_SCORE = intPreferencesKey("rules_target_score")
        private val KEY_RULES_OPENING = intPreferencesKey("rules_opening")
        private val KEY_RULES_MIN_TURN_AFTER_OPENING = intPreferencesKey("rules_min_turn_after_opening")
        private val KEY_RULES_REQUIRE_ROUND_100 = booleanPreferencesKey("rules_require_round_100")
        private val KEY_RULES_FINAL_SPRINT_1000 = booleanPreferencesKey("rules_final_sprint_1000")
        private val KEY_RULES_EXACT_FINISH = booleanPreferencesKey("rules_exact_finish")
        private val KEY_RULES_HOT_DICE_MANDATORY = booleanPreferencesKey("rules_hot_dice_mandatory")
        private val KEY_RULES_FOUR_KIND = stringPreferencesKey("rules_four_kind")
        private val KEY_RULES_FIVE_KIND = stringPreferencesKey("rules_five_kind")
        private val KEY_RULES_INSTANT_WIN_FIVE_ONES = booleanPreferencesKey("rules_instant_win_five_ones")
        private val KEY_RULES_STRAIGHT = intPreferencesKey("rules_straight")
        private val KEY_RULES_FULL_STRAIGHT_6 = intPreferencesKey("rules_full_straight_6")
        private val KEY_RULES_THREE_PAIRS = intPreferencesKey("rules_three_pairs")
        private val KEY_RULES_FULL_HOUSE = intPreferencesKey("rules_full_house")
        private val KEY_RULES_PIGGYBACK = booleanPreferencesKey("rules_piggyback")
        private val KEY_RULES_BUMP_MODE = stringPreferencesKey("rules_bump_mode")
        private val KEY_RULES_THREE_STRIKES_MODE = stringPreferencesKey("rules_three_strikes_mode")
    }
}
