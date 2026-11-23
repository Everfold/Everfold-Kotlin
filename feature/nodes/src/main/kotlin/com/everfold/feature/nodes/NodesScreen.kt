package com.everfold.feature.nodes

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeCapability
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeRole
import com.everfold.core.model.NodeStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NodesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = stringResource(R.string.nodes_open_menu),
                        )
                    }
                },
                title = { Text(stringResource(R.string.nodes_title)) },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.nodes_refresh),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConnectionSummaryCard(
                    connectionState = state.connectionState,
                    onDisconnect = viewModel::disconnect,
                )
            }
            item {
                EverfoldSectionHeader(
                    title = stringResource(R.string.nodes_topology_header, state.nodes.size),
                )
            }
            items(state.nodes, key = { it.id }) { node ->
                NodeCard(node = node)
            }
        }
    }
}

@Composable
private fun ConnectionSummaryCard(
    connectionState: ConnectionState,
    onDisconnect: () -> Unit,
) {
    val connected = connectionState as? ConnectionState.Connected
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text =
                        stringResource(
                            connected?.endpoint?.mode?.labelRes() ?: R.string.nodes_disconnected,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        connected?.endpoint?.address?.ifBlank {
                            stringResource(R.string.nodes_local_subtitle)
                        } ?: stringResource(R.string.nodes_select_authority),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            OutlinedButton(onClick = onDisconnect) {
                Text(stringResource(R.string.nodes_switch))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeCard(node: NodeDescriptor) {
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

@StringRes
private fun ConnectionMode.labelRes(): Int =
    when (this) {
        ConnectionMode.REMOTE_AUTHORITY -> R.string.nodes_mode_remote
        ConnectionMode.RELAY -> R.string.nodes_mode_relay
        ConnectionMode.LOCAL_ONLY -> R.string.nodes_mode_local
    }

@StringRes
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
