package com.everfold.core.ui.system

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.ui.R

@Composable
fun SystemPermissionsCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readSystemPermissionStatus(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = readSystemPermissionStatus(context)
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            status = readSystemPermissionStatus(context)
        }

    Card(modifier = modifier.fillMaxWidth()) {
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
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
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
