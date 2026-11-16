package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.connection_cached_info
import everfold.shared.generated.resources.connection_configuration
import everfold.shared.generated.resources.connection_connect
import everfold.shared.generated.resources.connection_disconnect
import everfold.shared.generated.resources.connection_disconnecting
import everfold.shared.generated.resources.connection_edit
import everfold.shared.generated.resources.connection_endpoint
import everfold.shared.generated.resources.connection_info_unavailable
import everfold.shared.generated.resources.connection_kernel_version
import everfold.shared.generated.resources.connection_last_connected
import everfold.shared.generated.resources.connection_never_connected
import everfold.shared.generated.resources.connection_new_session
import everfold.shared.generated.resources.connection_overview
import everfold.shared.generated.resources.connection_remove
import everfold.shared.generated.resources.connection_remove_hint
import everfold.shared.generated.resources.connection_remove_title
import everfold.shared.generated.resources.retry_connection

/** 所有连接共享概览与配置离线时保留编辑入口和最后一次内核信息 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionDetails(
    connection: Connection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSave: (ConnectionDraft) -> Boolean,
    onRemove: () -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier,
    overviewExtra: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var removing by rememberSaveable { mutableStateOf(false) }
    val pages = rememberSaveableStateHolder()
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(connection.profile.name.resolve(), style = MaterialTheme.typography.headlineSmall)
            ConnectionStatusLabel(connection.runtime.status)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ConnectionAction(connection.runtime.status, onConnect, onDisconnect)
                OutlinedButton(onClick = onNewConversation, enabled = connection.isConnected) {
                    Text(localizedString(Res.string.connection_new_session))
                }
            }
        }
        SecondaryScrollableTabRow(selectedTabIndex = tab, edgePadding = 24.dp) {
            listOf(Res.string.connection_overview, Res.string.connection_configuration).forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(localizedString(title)) })
            }
        }
        Box(Modifier.weight(1f)) {
            pages.SaveableStateProvider(tab) {
                if (tab == 0) {
                    ConnectionOverview(connection, onEdit = { tab = 1 }, onRemove = { removing = true }, overviewExtra)
                } else {
                    ConnectionEditor(connection.profile, onSave = { draft, _ -> onSave(draft) })
                }
            }
        }
    }
    if (removing) {
        AlertDialog(
            onDismissRequest = { removing = false },
            title = { Text(localizedString(Res.string.connection_remove_title, connection.profile.name.resolve())) },
            text = { Text(localizedString(Res.string.connection_remove_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    removing = false
                    onRemove()
                }) {
                    Text(localizedString(Res.string.connection_remove), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(localizedString(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun ConnectionOverview(
    connection: Connection,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    extraContent: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val runtime = connection.runtime
            if (runtime.status == ConnectionStatus.Connecting || runtime.status == ConnectionStatus.Disconnecting) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            runtime.failure?.let {
                Text(localizedString(it.description), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onEdit) { Text(localizedString(Res.string.connection_edit)) }
            }
            ConnectionInfo(localizedString(Res.string.connection_endpoint), connection.profile.endpointSummary())
            ConnectionInfo(
                localizedString(Res.string.connection_kernel_version),
                runtime.kernelVersion ?: localizedString(Res.string.connection_info_unavailable),
            )
            ConnectionInfo(
                localizedString(Res.string.connection_last_connected),
                runtime.lastConnectedAt
                    ?.toString()
                    ?.substringBefore('.')
                    ?.replace('T', ' ')
                    ?.removeSuffix("Z")
                    ?.plus(" UTC")
                    ?: localizedString(Res.string.connection_never_connected),
            )
            if (!connection.isConnected && runtime.lastConnectedAt != null) {
                Text(
                    localizedString(Res.string.connection_cached_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            extraContent()
            if (connection.profile.canRemove) {
                TextButton(
                    onClick = onRemove,
                ) { Text(localizedString(Res.string.connection_remove), color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ConnectionInfo(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ConnectionAction(
    status: ConnectionStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val disconnect = status in listOf(ConnectionStatus.Connected, ConnectionStatus.Connecting, ConnectionStatus.Disconnecting)
    Button(onClick = if (disconnect) onDisconnect else onConnect, enabled = status != ConnectionStatus.Disconnecting) {
        Text(
            localizedString(
                when (status) {
                    ConnectionStatus.Connected -> Res.string.connection_disconnect
                    ConnectionStatus.Connecting -> Res.string.cancel
                    ConnectionStatus.Disconnecting -> Res.string.connection_disconnecting
                    ConnectionStatus.Failed -> Res.string.retry_connection
                    ConnectionStatus.Disconnected -> Res.string.connection_connect
                },
            ),
        )
    }
}
