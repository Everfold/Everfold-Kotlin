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
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.AppPreferences
import com.everfold.feature.settings.screen.labelRes
import com.everfold.feature.settings.system.SystemPermissionStatus
import com.everfold.feature.settings.system.readSystemPermissionStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenDrawer: () -> Unit,
    onOpenSection: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(readSystemPermissionStatus(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionStatus = readSystemPermissionStatus(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.settings_open_menu),
                        )
                    }
                },
                title = { Text(stringResource(R.string.settings_title)) },
            )
        },
    ) { innerPadding ->
        SettingsHubList(
            prefs = prefs,
            permissionStatus = permissionStatus,
            versionName = viewModel.appInfo.versionName,
            onOpenSection = onOpenSection,
            onDisconnect = viewModel::disconnectAuthority,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun SettingsHubList(
    prefs: AppPreferences,
    permissionStatus: SystemPermissionStatus,
    versionName: String,
    onOpenSection: (SettingsSection) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { EverfoldSectionHeader(stringResource(R.string.settings_group_appearance_interface)) }
        item {
            AppearanceInterfaceHubCard(prefs = prefs, onOpenSection = onOpenSection)
        }
        item { EverfoldSectionHeader(stringResource(R.string.settings_group_sessions)) }
        item {
            SettingsEntryCard(
                icon = Icons.Rounded.ChatBubbleOutline,
                title = stringResource(R.string.settings_entry_sessions_title),
                subtitle = sessionsSummary(prefs),
                onClick = { onOpenSection(SettingsSection.SESSIONS) },
            )
        }
        item { EverfoldSectionHeader(stringResource(R.string.settings_group_permissions)) }
        item {
            SettingsEntryCard(
                icon = Icons.Rounded.Security,
                title = stringResource(R.string.settings_entry_permissions_title),
                subtitle = permissionsSummary(permissionStatus),
                onClick = { onOpenSection(SettingsSection.PERMISSIONS) },
            )
        }
        item { EverfoldSectionHeader(stringResource(R.string.settings_group_connection_about)) }
        item {
            SettingsEntryCard(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.settings_about_title),
                subtitle = stringResource(R.string.settings_about_entry_subtitle, versionName),
                onClick = { onOpenSection(SettingsSection.ABOUT) },
            )
        }
        item {
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_disconnect))
            }
        }
    }
}

@Composable
private fun AppearanceInterfaceHubCard(
    prefs: AppPreferences,
    onOpenSection: (SettingsSection) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SettingsEntryRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.settings_entry_appearance_title),
                subtitle =
                    stringResource(
                        R.string.settings_summary_pair,
                        stringResource(prefs.themeMode.labelRes()),
                        stringResource(prefs.themeColorMode.labelRes()),
                    ),
                onClick = { onOpenSection(SettingsSection.APPEARANCE) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingsEntryRow(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.settings_entry_language_title),
                subtitle = stringResource(prefs.language.labelRes()),
                onClick = { onOpenSection(SettingsSection.LANGUAGE) },
            )
        }
    }
}

@Composable
private fun SettingsEntryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        SettingsEntryRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            onClick = onClick,
        )
    }
}

@Composable
private fun SettingsEntryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
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

@Composable
private fun sessionsSummary(prefs: AppPreferences): String {
    val sizeLabel = stringResource(prefs.chatTextSize.labelRes())
    val collapseLabel =
        stringResource(
            if (prefs.autoCollapseLongMessages) {
                R.string.settings_summary_auto_collapse_on
            } else {
                R.string.settings_summary_auto_collapse_off
            },
        )
    return stringResource(R.string.settings_summary_pair, sizeLabel, collapseLabel)
}

@Composable
private fun permissionsSummary(status: SystemPermissionStatus): String {
    val notificationText =
        stringResource(
            R.string.settings_summary_notifications_status,
            stringResource(
                if (status.notificationsGranted) R.string.perm_status_granted else R.string.perm_status_denied,
            ),
        )
    val backgroundText =
        stringResource(
            R.string.settings_summary_background_status,
            stringResource(
                if (status.backgroundUnrestricted) R.string.perm_status_allowed else R.string.perm_status_restricted,
            ),
        )
    return stringResource(R.string.settings_summary_pair, notificationText, backgroundText)
}
