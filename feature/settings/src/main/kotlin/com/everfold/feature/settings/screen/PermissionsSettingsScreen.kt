package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.ui.system.SystemPermissionsCard
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_entry_permissions_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SystemPermissionsCard() }
            item { EverfoldSectionHeader(stringResource(R.string.perm_pref_section_live_activity)) }
            item {
                LiveActivityPreferencesGroup(
                    notifications = prefs.notifications,
                    onNotificationsChange = viewModel::setNotifications,
                )
            }
        }
    }
}

@Composable
private fun LiveActivityPreferencesGroup(
    notifications: NotificationPreferences,
    onNotificationsChange: (NotificationPreferences) -> Unit,
) {
    val itemCount = 3
    EverfoldGroupedList {
        EverfoldGroupedItem(index = 0, count = itemCount) {
            SettingSwitchRow(
                title = stringResource(R.string.perm_pref_live_capsule_title),
                subtitle = stringResource(R.string.perm_pref_live_capsule_subtitle),
                checked = notifications.liveActivityEnabled,
                onCheckedChange = { checked ->
                    onNotificationsChange(notifications.copy(liveActivityEnabled = checked))
                },
                modifier = Modifier.padding(16.dp),
            )
        }
        EverfoldGroupedItem(index = 1, count = itemCount) {
            SettingSwitchRow(
                title = stringResource(R.string.perm_pref_alert_attention_title),
                subtitle = stringResource(R.string.perm_pref_alert_attention_subtitle),
                checked = notifications.alertOnNeedsAttention,
                onCheckedChange = { checked ->
                    onNotificationsChange(notifications.copy(alertOnNeedsAttention = checked))
                },
                modifier = Modifier.padding(16.dp),
            )
        }
        EverfoldGroupedItem(index = 2, count = itemCount) {
            SettingSwitchRow(
                title = stringResource(R.string.perm_pref_alert_completed_title),
                subtitle = stringResource(R.string.perm_pref_alert_completed_subtitle),
                checked = notifications.alertOnCompleted,
                onCheckedChange = { checked ->
                    onNotificationsChange(notifications.copy(alertOnCompleted = checked))
                },
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
