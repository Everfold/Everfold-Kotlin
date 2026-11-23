package com.everfold.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize

private const val BASE_PREVIEW_SP = 14f

internal data class SystemPermissionStatus(
    val notificationsGranted: Boolean,
    val backgroundUnrestricted: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageCard(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.interface_language_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = language == option,
                        onClick = {
                            onLanguageChange(option)
                            applyAppLanguage(context, option)
                        },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionPreferencesCard(
    prefs: AppPreferences,
    onChatTextSizeChange: (ChatTextSize) -> Unit,
    onAutoCollapseChange: (Boolean) -> Unit,
    onShowNodeBadgesChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.session_pref_text_size_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatTextSize.entries.forEach { size ->
                    FilterChip(
                        selected = prefs.chatTextSize == size,
                        onClick = { onChatTextSizeChange(size) },
                        label = { Text(stringResource(size.labelRes())) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.session_pref_text_preview),
                fontSize = (BASE_PREVIEW_SP * prefs.chatTextSize.scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSwitchRow(
                title = stringResource(R.string.session_pref_auto_collapse_title),
                subtitle = stringResource(R.string.session_pref_auto_collapse_subtitle),
                checked = prefs.autoCollapseLongMessages,
                onCheckedChange = onAutoCollapseChange,
            )
            SettingSwitchRow(
                title = stringResource(R.string.session_pref_show_traces_title),
                subtitle = stringResource(R.string.session_pref_show_traces_subtitle),
                checked = prefs.showNodeBadgesInline,
                onCheckedChange = onShowNodeBadgesChange,
            )
        }
    }
}

@Composable
internal fun PermissionsCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readSystemPermissionStatus(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = readSystemPermissionStatus(context)
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            status = readSystemPermissionStatus(context)
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NotificationPermissionRow(
                granted = status.notificationsGranted,
                onAction = {
                    if (!status.notificationsGranted &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            BackgroundPermissionRow(
                allowed = status.backgroundUnrestricted,
                onAction = {
                    if (status.backgroundUnrestricted) {
                        openBatteryManagementSettings(context)
                    } else {
                        requestIgnoreBatteryOptimizations(context)
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionActionRow(
                title = stringResource(R.string.perm_battery_title),
                subtitle = stringResource(R.string.perm_battery_subtitle),
                statusText = stringResource(R.string.perm_status_manual),
                buttonText = stringResource(R.string.perm_action_open_battery),
                onAction = { openBatteryManagementSettings(context) },
            )
        }
    }
}

@Composable
private fun NotificationPermissionRow(
    granted: Boolean,
    onAction: () -> Unit,
) {
    PermissionActionRow(
        title = stringResource(R.string.perm_notifications_title),
        subtitle = stringResource(R.string.perm_notifications_subtitle),
        statusText =
            stringResource(
                if (granted) R.string.perm_status_granted else R.string.perm_status_denied,
            ),
        buttonText =
            stringResource(
                if (granted) R.string.perm_action_notification_settings else R.string.perm_action_request,
            ),
        onAction = onAction,
    )
}

@Composable
private fun BackgroundPermissionRow(
    allowed: Boolean,
    onAction: () -> Unit,
) {
    PermissionActionRow(
        title = stringResource(R.string.perm_background_title),
        subtitle = stringResource(R.string.perm_background_subtitle),
        statusText =
            stringResource(
                if (allowed) R.string.perm_status_allowed else R.string.perm_status_restricted,
            ),
        buttonText =
            stringResource(
                if (allowed) R.string.perm_action_view_settings else R.string.perm_action_allow,
            ),
        onAction = onAction,
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    subtitle: String,
    statusText: String,
    buttonText: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            EverfoldStatusPill(text = statusText)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onAction) {
            Text(buttonText)
        }
    }
}

@StringRes
private fun AppLanguage.labelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.language_zh_hans
        AppLanguage.TRADITIONAL_CHINESE -> R.string.language_zh_hant
        AppLanguage.ENGLISH -> R.string.language_en
    }

@StringRes
private fun ChatTextSize.labelRes(): Int =
    when (this) {
        ChatTextSize.SMALL -> R.string.text_size_small
        ChatTextSize.MEDIUM -> R.string.text_size_medium
        ChatTextSize.LARGE -> R.string.text_size_large
        ChatTextSize.EXTRA_LARGE -> R.string.text_size_extra_large
    }
