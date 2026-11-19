package com.everfold.kotlin.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.connections.ClusterConnectionForm
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionPreviewOptions
import com.everfold.kotlin.feature.connections.ConnectionPreviewResult
import com.everfold.kotlin.feature.connections.description
import com.everfold.kotlin.feature.settings.ThemeColorSettings
import com.everfold.kotlin.feature.settings.labelResource
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.AppearancePreferences
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.ThemeMode
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.appearance
import everfold.shared.generated.resources.cluster_hint
import everfold.shared.generated.resources.connection_success
import everfold.shared.generated.resources.feature_continuity
import everfold.shared.generated.resources.feature_continuity_detail
import everfold.shared.generated.resources.feature_customization
import everfold.shared.generated.resources.feature_customization_detail
import everfold.shared.generated.resources.feature_topology
import everfold.shared.generated.resources.feature_topology_detail
import everfold.shared.generated.resources.feature_workspace
import everfold.shared.generated.resources.feature_workspace_detail
import everfold.shared.generated.resources.language
import everfold.shared.generated.resources.mode_cluster
import everfold.shared.generated.resources.mode_offline
import everfold.shared.generated.resources.offline_hint
import everfold.shared.generated.resources.onboarding_intro
import org.jetbrains.compose.resources.StringResource
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class WelcomeFeature(
    val symbol: AppSymbol,
    val title: StringResource,
    val detail: StringResource,
)

private val WELCOME_FEATURES =
    listOf(
        WelcomeFeature(AppSymbol.Connection, Res.string.feature_workspace, Res.string.feature_workspace_detail),
        WelcomeFeature(AppSymbol.Chat, Res.string.feature_topology, Res.string.feature_topology_detail),
        WelcomeFeature(AppSymbol.Spark, Res.string.feature_continuity, Res.string.feature_continuity_detail),
        WelcomeFeature(AppSymbol.Settings, Res.string.feature_customization, Res.string.feature_customization_detail),
    )

@Composable
internal fun OnboardingWelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = localizedString(Res.string.onboarding_intro),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            WELCOME_FEATURES.forEachIndexed { index, feature ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                BasicComponent(
                    title = localizedString(feature.title),
                    summary = localizedString(feature.detail),
                    startAction = { FeatureBadge(feature.symbol) },
                )
            }
        }
    }
}

@Composable
internal fun OnboardingPreferencesStep(
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val backdrop = LocalAppBackdrop.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            GlassDropdownPreference(
                title = localizedString(Res.string.appearance),
                items = ThemeMode.entries.map { localizedString(it.label) },
                selectedIndex = ThemeMode.entries.indexOf(appearance.themeMode),
                onSelectedIndexChange = { appearance.setTheme(ThemeMode.entries[it]) },
                backdrop = backdrop,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            GlassDropdownPreference(
                title = localizedString(Res.string.language),
                items = AppLanguage.entries.map { localizedString(it.labelResource) },
                selectedIndex = AppLanguage.entries.indexOf(language),
                onSelectedIndexChange = { onLanguageChange(AppLanguage.entries[it]) },
                backdrop = backdrop,
            )
        }
        ThemeColorSettings(
            selection = appearance.themeColor,
            onSystemColorChange = appearance::useSystemThemeColor,
            onPresetChange = appearance::selectThemePreset,
            onCustomColorChange = appearance::applyCustomThemeColor,
        )
    }
}

@Composable
internal fun OnboardingModeStep(
    cluster: Boolean,
    onClusterChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().selectableGroup()) {
        RadioButtonPreference(
            title = localizedString(Res.string.mode_offline),
            summary = localizedString(Res.string.offline_hint),
            selected = !cluster,
            onClick = { onClusterChange(false) },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        RadioButtonPreference(
            title = localizedString(Res.string.mode_cluster),
            summary = localizedString(Res.string.cluster_hint),
            selected = cluster,
            onClick = { onClusterChange(true) },
        )
    }
}

@Composable
internal fun OnboardingClusterStep(
    form: ClusterSetup,
    showError: Boolean,
    previewResult: ConnectionPreviewResult,
    test: ConnectionTest,
    onFormChange: (ClusterSetup) -> Unit,
    onPreviewResultChange: (ConnectionPreviewResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ClusterConnectionForm(form = form, wide = false, onChange = onFormChange, showErrors = showError)
        ConnectionPreviewOptions(previewResult, onSelect = onPreviewResultChange)
        when (test) {
            ConnectionTest.Testing -> {
                LinearProgressIndicator(Modifier.fillMaxWidth(), progress = null)
            }

            ConnectionTest.Success -> {
                Text(
                    text = localizedString(Res.string.connection_success),
                    color = MiuixTheme.colorScheme.primary,
                    style = MiuixTheme.textStyles.body2,
                )
            }

            ConnectionTest.Failure -> {
                Text(
                    text = localizedString(previewResult.description),
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.body2,
                )
            }

            ConnectionTest.Idle -> {}
        }
    }
}

@Composable
private fun FeatureBadge(symbol: AppSymbol) {
    Surface(
        modifier = Modifier.padding(end = 12.dp).squircleSurface(MiuixTheme.colorScheme.primaryContainer, 12.dp),
        color = Color.Transparent,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            AppIcon(symbol, modifier = Modifier.size(20.dp), tint = MiuixTheme.colorScheme.onPrimaryContainer)
        }
    }
}
