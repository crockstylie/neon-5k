package fr.antoinehory.neon5k.ui.info

import android.app.Activity
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import fr.antoinehory.neon5k.R
import fr.antoinehory.neon5k.ui.components.CyberCutShape
import fr.antoinehory.neon5k.ui.components.CyberPanel
import fr.antoinehory.neon5k.ui.components.PixelButton
import fr.antoinehory.neon5k.ui.theme.LocalNeon5KColors
import java.util.Locale

/**
 * Fiche Informations & Crédits de Neon 5K (Lot 3), alignée sur l'écran `InfoScreen` d'`OCRAurebesh` :
 * - Coordonnées développeur (`Antoine Crock HORY`, `antoinehory.fr`, `contact@antoinehory.fr`, `paypal.me/kuroku`)
 * - Bouton de don PayPal (`https://paypal.me/kuroku`)
 * - Bouton de notation 5 ★ sur Google Play (`market://details?id=fr.antoinehory.neon5k`)
 * - Lien vers les autres applications du développeur (`OCRAurebesh`, etc.)
 * - Bandeau des 6 icônes vectorielles XML de réseaux sociaux (`LinkedIn`, `Behance`, `Instagram`, `Facebook`, `Spotify`, `Steam`)
 * - Sélecteur de langue in-app (`FR | EN | ES | PT`) et résumé de confidentialité / entropie matérielle.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val colors = LocalNeon5KColors.current
    val currentLang = Locale.getDefault().language.lowercase(Locale.ROOT)

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
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
                text = stringResource(R.string.personal_info_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.primaryNeon
            )
        }

        // Carte principale Développeur + Don PayPal + Note 5 ★ + Autres applications
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.primaryNeon
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoItemColumn(
                    label = stringResource(R.string.developer_label),
                    value = "Antoine Crock HORY"
                )
                InfoItemColumn(
                    label = stringResource(R.string.website_label),
                    value = "antoinehory.fr",
                    isLink = true,
                    linkUri = "https://antoinehory.fr"
                )
                InfoItemColumn(
                    label = stringResource(R.string.email_label),
                    value = "contact@antoinehory.fr",
                    isLink = true,
                    linkUri = "mailto:contact@antoinehory.fr"
                )
                InfoItemColumn(
                    label = stringResource(R.string.donate_label),
                    value = "paypal.me/kuroku",
                    isLink = true,
                    linkUri = "https://paypal.me/kuroku"
                )

                Spacer(modifier = Modifier.height(6.dp))

                PixelButton(
                    text = stringResource(R.string.donate_button),
                    onClick = { uriHandler.openUri("https://paypal.me/kuroku") },
                    accentColor = colors.alertNeon,
                    filled = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PixelButton(
                    text = stringResource(R.string.rate_app_button),
                    onClick = { openPlayStoreListing(context) },
                    accentColor = colors.primaryNeon,
                    filled = true,
                    modifier = Modifier.fillMaxWidth()
                )

                PixelButton(
                    text = stringResource(R.string.other_apps_button),
                    onClick = { openDeveloperApps(context) },
                    accentColor = colors.secondaryNeon,
                    filled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Bandeau Réseaux Sociaux (6 icônes XML identiques à OCRAurebesh)
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.secondaryNeon.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.find_me_on),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondaryNeon,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_linkedin_logo,
                        contentDescription = "linkedin_profile",
                        url = "https://www.linkedin.com/in/antoinehory/"
                    )
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_behance_logo,
                        contentDescription = "behance_profile",
                        url = "https://www.behance.net/antoine-hory"
                    )
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_instagram_logo,
                        contentDescription = "instagram_profile",
                        url = "https://www.instagram.com/antoine.hory.web/"
                    )
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_facebook_logo,
                        contentDescription = "facebook_profile",
                        url = "https://www.facebook.com/antoinehory/"
                    )
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_spotify_logo,
                        contentDescription = "spotify_profile",
                        url = "https://open.spotify.com/user/crockstylie"
                    )
                    SocialMediaIcon(
                        iconResId = R.drawable.ic_steam_logo,
                        contentDescription = "steam_profile",
                        url = "https://steamcommunity.com/id/crockstylie/"
                    )
                }
            }
        }

        // Sélecteur de langue rapide (FR | EN | ES | PT)
        CyberPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.language_selector_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primaryNeon
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "fr" to "FRANÇAIS",
                        "en" to "ENGLISH",
                        "es" to "ESPAÑOL",
                        "pt" to "PORTUGUÊS"
                    ).forEach { (tag, label) ->
                        val isSelected = currentLang.startsWith(tag)
                        PixelButton(
                            text = label,
                            filled = isSelected,
                            accentColor = if (isSelected) colors.primaryNeon else colors.textMuted,
                            onClick = { applyAppLocale(context, tag) }
                        )
                    }
                }
            }
        }

        // Résumé Confidentialité, Entropie Matérielle & Version
        CyberPanel(
            modifier = Modifier.fillMaxWidth(),
            borderColor = colors.textMuted.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_summary_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.secondaryNeon
                )
                Text(
                    text = stringResource(R.string.privacy_summary_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NEON 5K v1.0.0 (BUILD 1) • MINSDK 29 / TARGETSDK 37 • PIXEL & SHA-256 ENGINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryNeon,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun InfoItemColumn(
    label: String,
    value: String,
    isLink: Boolean = false,
    linkUri: String? = null,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val colors = LocalNeon5KColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label :",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            textAlign = TextAlign.Center
        )
        if (isLink && linkUri != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.primaryNeon,
                textDecoration = TextDecoration.Underline,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(linkUri) }
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SocialMediaIcon(
    iconResId: Int,
    contentDescription: String,
    url: String,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val colors = LocalNeon5KColors.current
    IconButton(
        onClick = { uriHandler.openUri(url) },
        modifier = modifier
            .size(48.dp)
            .background(colors.background.copy(alpha = 0.5f), CyberCutShape)
            .border(1.dp, colors.primaryNeon.copy(alpha = 0.35f), CyberCutShape)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

private fun openPlayStoreListing(context: Context) {
    val packageName = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

private fun openDeveloperApps(context: Context) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://search?q=pub:Crock")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (_: ActivityNotFoundException) {
        val webIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/search?q=pub:Crock&c=apps")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}

private fun applyAppLocale(context: Context, languageTag: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)
            ?.applicationLocales = LocaleList.forLanguageTags(languageTag)
    } else {
        val locale = Locale.forLanguageTag(languageTag)
        Locale.setDefault(locale)
        @Suppress("DEPRECATION")
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        (context as? Activity)?.recreate()
    }
}
