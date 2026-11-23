package com.everfold.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import org.koin.androidx.compose.koinViewModel

private data class SettingsActions(
    val onThemeModeChange: (ThemeMode) -> Unit,
    val onThemeColorModeChange: (ThemeColorMode) -> Unit,
    val onSeedColorChange: (SeedColorPreset) -> Unit,
    val onCustomColorHexChange: (String) -> Unit,
    val onLanguageChange: (AppLanguage) -> Unit,
    val onChatTextSizeChange: (ChatTextSize) -> Unit,
    val onAutoCollapseChange: (Boolean) -> Unit,
    val onShowNodeBadgesChange: (Boolean) -> Unit,
    val onOpenAbout: () -> Unit,
    val onDisconnect: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    var showAbout by rememberSaveable { mutableStateOf(false) }

    if (showAbout) {
        AboutScreen(
            appInfo = viewModel.appInfo,
            onBack = { showAbout = false },
            modifier = modifier,
        )
        return
    }

    val actions =
        remember(viewModel) {
            SettingsActions(
                onThemeModeChange = viewModel::setThemeMode,
                onThemeColorModeChange = viewModel::setThemeColorMode,
                onSeedColorChange = viewModel::setSeedColor,
                onCustomColorHexChange = viewModel::setCustomColorHex,
                onLanguageChange = viewModel::setLanguage,
                onChatTextSizeChange = viewModel::setChatTextSize,
                onAutoCollapseChange = viewModel::setAutoCollapseLongMessages,
                onShowNodeBadgesChange = viewModel::setShowNodeBadgesInline,
                onOpenAbout = { showAbout = true },
                onDisconnect = viewModel::disconnectAuthority,
            )
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { innerPadding ->
        SettingsContentList(
            prefs = prefs,
            versionName = viewModel.appInfo.versionName,
            actions = actions,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun SettingsContentList(
    prefs: AppPreferences,
    versionName: String,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { EverfoldSectionHeader(title = stringResource(R.string.settings_section_appearance)) }
        item {
            AppearanceCard(
                prefs = prefs,
                onThemeModeChange = actions.onThemeModeChange,
                onThemeColorModeChange = actions.onThemeColorModeChange,
                onSeedColorChange = actions.onSeedColorChange,
                onCustomColorHexChange = actions.onCustomColorHexChange,
            )
        }
        item { EverfoldSectionHeader(title = stringResource(R.string.settings_section_interface)) }
        item {
            LanguageCard(
                language = prefs.language,
                onLanguageChange = actions.onLanguageChange,
            )
        }
        item { EverfoldSectionHeader(title = stringResource(R.string.settings_section_sessions)) }
        item {
            SessionPreferencesCard(
                prefs = prefs,
                onChatTextSizeChange = actions.onChatTextSizeChange,
                onAutoCollapseChange = actions.onAutoCollapseChange,
                onShowNodeBadgesChange = actions.onShowNodeBadgesChange,
            )
        }
        item { EverfoldSectionHeader(title = stringResource(R.string.settings_section_permissions)) }
        item { PermissionsCard() }
        item {
            EverfoldSectionHeader(title = stringResource(R.string.settings_section_connection_about))
        }
        item {
            AboutEntryCard(
                versionName = versionName,
                onClick = actions.onOpenAbout,
            )
        }
        item {
            OutlinedButton(
                onClick = actions.onDisconnect,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_disconnect))
            }
        }
    }
}

@Composable
private fun AboutEntryCard(
    versionName: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(R.string.settings_about_entry_subtitle, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
