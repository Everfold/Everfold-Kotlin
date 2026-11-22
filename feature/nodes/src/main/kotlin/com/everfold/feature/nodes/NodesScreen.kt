package com.everfold.feature.nodes

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeCapability
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeRole
import com.everfold.core.model.NodeStatus
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodesScreen(
    modifier: Modifier = Modifier,
    viewModel: NodesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("节点管理") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "刷新节点")
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
                EverfoldSectionHeader(title = "可见节点拓扑 (${state.nodes.size})")
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
                    text = connected?.endpoint?.label ?: "未连接",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text =
                        connected?.endpoint?.address?.ifBlank { "本机独立运行 (Mock)" }
                            ?: "请选择 Authority 节点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            OutlinedButton(onClick = onDisconnect) {
                Text("切换")
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
                EverfoldStatusPill(text = node.status.toLabel())
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

private fun NodeStatus.toLabel(): String =
    when (this) {
        NodeStatus.ONLINE -> "在线"
        NodeStatus.DEGRADED -> "降级"
        NodeStatus.OFFLINE -> "离线"
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
