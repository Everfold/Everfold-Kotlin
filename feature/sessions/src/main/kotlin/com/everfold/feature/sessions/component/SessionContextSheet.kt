package com.everfold.feature.sessions.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.NodeStatus
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceDescriptor
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.nodeMatchesQuery
import com.everfold.feature.sessions.visibleWorkspacesForNode
import kotlinx.collections.immutable.ImmutableList

private const val PICKER_LIST_MAX_HEIGHT_DP = 420

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionContextPickerSheet(
    nodes: ImmutableList<NodeDescriptor>,
    selectedContext: SessionRuntimeContext?,
    onSelect: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
    onOpenNodes: () -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedRelay by rememberSaveable { mutableStateOf<String?>(null) }
    var onlineOnly by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (nodes.isEmpty()) {
            EmptyNodesPickerState(onOpenNodes = onOpenNodes, onDismiss = onDismiss)
        } else {
            ContextPickerBody(
                nodes = nodes,
                selectedContext = selectedContext,
                query = query,
                selectedRelay = selectedRelay,
                onlineOnly = onlineOnly,
                onQueryChange = { query = it },
                onRelayChange = { selectedRelay = it },
                onOnlineOnlyToggle = { onlineOnly = !onlineOnly },
                onSelect = { node, workspace ->
                    onSelect(node, workspace)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun ContextPickerBody(
    nodes: ImmutableList<NodeDescriptor>,
    selectedContext: SessionRuntimeContext?,
    query: String,
    selectedRelay: String?,
    onlineOnly: Boolean,
    onQueryChange: (String) -> Unit,
    onRelayChange: (String?) -> Unit,
    onOnlineOnlyToggle: () -> Unit,
    onSelect: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
) {
    val relayNames = nodes.map { it.relayName }.filter { it.isNotBlank() }.distinct()
    val filteredNodes =
        nodes.filter { node ->
            val relayMatches = selectedRelay == null || node.relayName == selectedRelay
            val statusMatches = !onlineOnly || node.status == NodeStatus.ONLINE
            relayMatches && statusMatches && nodeMatchesQuery(node = node, query = query)
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.session_context_picker_title),
            style = MaterialTheme.typography.titleLarge,
        )
        ContextSearchField(query = query, onQueryChange = onQueryChange)
        ContextFilterChipsRow(
            relayNames = relayNames,
            selectedRelay = selectedRelay,
            onlineOnly = onlineOnly,
            onRelayChange = onRelayChange,
            onOnlineOnlyToggle = onOnlineOnlyToggle,
        )
        ContextPickerResults(
            filteredNodes = filteredNodes,
            query = query,
            selectedContext = selectedContext,
            onSelect = onSelect,
        )
    }
}

@Composable
private fun ContextPickerResults(
    filteredNodes: List<NodeDescriptor>,
    query: String,
    selectedContext: SessionRuntimeContext?,
    onSelect: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
) {
    if (filteredNodes.isEmpty()) {
        Text(
            text = stringResource(R.string.session_context_no_search_match),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 24.dp),
        )
    } else {
        val grouped = filteredNodes.groupBy { it.relayName }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = PICKER_LIST_MAX_HEIGHT_DP.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            grouped.forEach { (relayName, relayNodes) ->
                if (relayName.isNotBlank()) {
                    item(key = "relay-$relayName") { RelayHeaderRow(relayName = relayName) }
                }
                items(relayNodes, key = { it.id }) { node ->
                    NodeWorkspacesSection(
                        node = node,
                        query = query,
                        selectedContext = selectedContext,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContextSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.session_context_search_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, shapes = IconButtonDefaults.shapes()) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = null)
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ContextFilterChipsRow(
    relayNames: List<String>,
    selectedRelay: String?,
    onlineOnly: Boolean,
    onRelayChange: (String?) -> Unit,
    onOnlineOnlyToggle: () -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item(key = "filter-all") {
            FilterChip(
                selected = selectedRelay == null,
                onClick = { onRelayChange(null) },
                label = { Text(stringResource(R.string.session_context_filter_all)) },
            )
        }
        items(relayNames, key = { "relay-$it" }) { relay ->
            FilterChip(
                selected = selectedRelay == relay,
                onClick = { onRelayChange(if (selectedRelay == relay) null else relay) },
                label = { Text(relay, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
        item(key = "filter-online") {
            FilterChip(
                selected = onlineOnly,
                onClick = onOnlineOnlyToggle,
                label = { Text(stringResource(R.string.session_context_filter_online_only)) },
            )
        }
    }
}

@Composable
private fun RelayHeaderRow(relayName: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Hub,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = relayName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NodeWorkspacesSection(
    node: NodeDescriptor,
    query: String,
    selectedContext: SessionRuntimeContext?,
    onSelect: (NodeDescriptor, WorkspaceDescriptor?) -> Unit,
) {
    val visibleWorkspaces = visibleWorkspacesForNode(node = node, query = query)
    val latencyLabel = node.latencyMs?.let { stringResource(R.string.session_context_latency_ms, it) }
    val metaSuffix = if (latencyLabel != null) "${node.platform} · $latencyLabel" else node.platform
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(node.status.dotColor()))
            Text(text = node.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(text = metaSuffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (visibleWorkspaces.isEmpty()) {
            EmptyWorkspaceOption(
                selected = selectedContext?.nodeId == node.id && selectedContext.workspaceId == null,
                onClick = { onSelect(node, null) },
            )
        } else {
            EverfoldGroupedList {
                visibleWorkspaces.forEachIndexed { index, workspace ->
                    WorkspaceOptionCard(
                        index = index,
                        count = visibleWorkspaces.size,
                        workspace = workspace,
                        selected = selectedContext?.nodeId == node.id && selectedContext.workspaceId == workspace.id,
                        onClick = { onSelect(node, workspace) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWorkspaceOption(
    selected: Boolean,
    onClick: () -> Unit,
) {
    EverfoldGroupedList {
        EverfoldGroupedItem(
            index = 0,
            count = 1,
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            selected = selected,
            onClick = onClick,
        ) {
            Text(
                text = stringResource(R.string.session_context_no_workspace),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}

@Composable
private fun WorkspaceOptionCard(
    index: Int,
    count: Int,
    workspace: WorkspaceDescriptor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    EverfoldGroupedItem(
        index = index,
        count = count,
        containerColor =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        selected = selected,
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = workspace.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                WorkspacePermissionBadge(permission = workspace.permissionLevel)
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Text(
                text = workspace.fileScope,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (workspace.capabilities.isNotEmpty()) {
                WorkspaceCapabilityChips(capabilities = workspace.capabilities)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EmptyNodesPickerState(
    onOpenNodes: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.session_context_empty_nodes_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.session_context_empty_nodes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = {
                onDismiss()
                onOpenNodes()
            },
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(stringResource(R.string.session_context_go_to_nodes))
        }
    }
}
