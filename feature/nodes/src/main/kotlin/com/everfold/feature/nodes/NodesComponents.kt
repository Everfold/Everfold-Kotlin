package com.everfold.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeCapability
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeRole
import com.everfold.core.model.NodeStatus
import com.everfold.core.ui.connection.summaryLine

@Composable
internal fun ConnectionSummaryCard(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
    onConnectLocal: () -> Unit,
) {
    val isConnected = connectionState is ConnectionState.Connected
    val (title, subtitle) = connectionSummaryTexts(connectionState)
    val containerColor =
        if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val subtitleColor =
        if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleColor)
            }
            OutlinedButton(onClick = if (isConnected) onDisconnect else onConnectLocal) {
                Text(
                    stringResource(
                        if (isConnected) R.string.nodes_action_disconnect else R.string.nodes_action_use_local,
                    ),
                )
            }
        }
    }
}

@Composable
private fun connectionSummaryTexts(connectionState: ConnectionState): Pair<String, String> {
    val connected = connectionState as? ConnectionState.Connected
    val titleRes =
        when {
            connected?.endpoint?.mode == ConnectionMode.REMOTE -> R.string.nodes_mode_remote
            connected?.endpoint?.mode == ConnectionMode.LOCAL_ONLY -> R.string.nodes_mode_local
            else -> R.string.nodes_disconnected
        }
    val title = connected?.endpoint?.label?.ifBlank { null } ?: stringResource(titleRes)
    val subtitle =
        when {
            connected == null -> {
                stringResource(R.string.nodes_disconnected_subtitle)
            }

            connected.endpoint.mode == ConnectionMode.LOCAL_ONLY -> {
                stringResource(R.string.nodes_local_subtitle)
            }

            else -> {
                connected.endpoint.transport?.summaryLine()
                    ?: connected.endpoint.address.ifBlank { connected.endpoint.label }
            }
        }
    return title to subtitle
}

@Composable
internal fun SavedProfileCard(
    profile: ConnectionProfile,
    isActive: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavedProfileHeader(profile = profile, isActive = isActive, onOpenMenu = onOpenMenu)
            Text(
                text = profile.transport.summaryLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.note.isNotBlank()) {
                Text(
                    text = profile.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isActive) {
                    OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.nodes_action_disconnect)) }
                } else {
                    FilledTonalButton(onClick = onConnect, enabled = !isConnecting) {
                        if (isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.nodes_action_connect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProfileHeader(
    profile: ConnectionProfile,
    isActive: Boolean,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            EverfoldStatusPill(text = stringResource(R.string.nodes_profile_connected_badge))
        }
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.nodes_action_more),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NodeTopologyCard(node: NodeDescriptor) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = node.name, style = MaterialTheme.typography.titleMedium)
                    val latencyText = node.latencyMs?.let { " · ${it}ms" }.orEmpty()
                    Text(
                        text = "${node.platform}$latencyText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                EverfoldStatusPill(text = stringResource(node.status.labelRes()))
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                node.roles.forEach { role ->
                    EverfoldStatusPill(
                        text = role.toLabel(),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                node.capabilities.forEach { cap ->
                    EverfoldStatusPill(
                        text = cap.toLabel(),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun NodeStatus.labelRes(): Int =
    when (this) {
        NodeStatus.ONLINE -> R.string.node_status_online
        NodeStatus.DEGRADED -> R.string.node_status_degraded
        NodeStatus.OFFLINE -> R.string.node_status_offline
    }

private fun NodeRole.toLabel(): String =
    when (this) {
        NodeRole.AUTHORITY -> "Authority"
        NodeRole.WORKER -> "Worker"
    }

private fun NodeCapability.toLabel(): String =
    when (this) {
        NodeCapability.LOOP_HOST -> "Loop"
        NodeCapability.LLM_GATEWAY -> "LLM"
        NodeCapability.TOOL_RUNTIME -> "Tool"
        NodeCapability.MCP_BRIDGE -> "MCP"
        NodeCapability.CODE_MODE -> "CodeMode"
    }
