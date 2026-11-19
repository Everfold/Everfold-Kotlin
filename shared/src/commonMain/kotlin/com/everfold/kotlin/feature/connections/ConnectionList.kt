package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.clear_search
import everfold.shared.generated.resources.cluster_cloud
import everfold.shared.generated.resources.connection_count
import everfold.shared.generated.resources.connection_discovery
import everfold.shared.generated.resources.connection_no_results
import everfold.shared.generated.resources.connection_search

/** 列表按保存顺序渲染状态更新只刷新原位置搜索同时匹配名称与接入摘要 */
@Composable
internal fun ConnectionList(
    connections: List<Connection>,
    selectedId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 0.dp,
) {
    val names = connections.associate { it.id to it.profile.name.resolve() }
    val endpoints = connections.associate { it.id to it.profile.endpointSummary() }
    val filtered =
        connections.filter {
            names.getValue(it.id).contains(query, ignoreCase = true) || endpoints.getValue(it.id).contains(query, ignoreCase = true)
        }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            localizedString(Res.string.connection_count, connections.size, connections.count { it.isConnected }),
            Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            placeholder = { Text(localizedString(Res.string.connection_search)) },
            leadingIcon = { AppIcon(AppSymbol.Search) },
            trailingIcon = {
                if (query.isNotEmpty()) TextButton(onClick = { onQueryChange("") }) { Text(localizedString(Res.string.clear_search)) }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp + bottomPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (filtered.isEmpty()) {
                item {
                    Text(
                        localizedString(Res.string.connection_no_results),
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(filtered, key = { it.id }) { connection ->
                ConnectionListItem(
                    connection,
                    names.getValue(connection.id),
                    endpoints.getValue(connection.id),
                    connection.id == selectedId,
                ) {
                    onSelect(connection.id)
                }
            }
        }
    }
}

@Composable
internal fun ConnectionProfile.endpointSummary(): String =
    when {
        setup == null -> "Beamlet"
        setup.method == ClusterMethod.Cloud -> localizedString(Res.string.cluster_cloud)
        setup.ip.isBlank() -> localizedString(Res.string.connection_discovery)
        ':' in setup.ip -> "[${setup.ip.trim()}]:${setup.port.trim()}"
        else -> "${setup.ip.trim()}:${setup.port.trim()}"
    }

@Composable
internal fun ConnectionStatusLabel(status: ConnectionStatus) {
    Text(
        localizedString(status.label),
        style = MaterialTheme.typography.labelMedium,
        color = connectionStatusColor(status),
        maxLines = 1,
    )
}

@Composable
private fun ConnectionListItem(
    connection: Connection,
    name: String,
    endpoint: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth().selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onSelect,
            ),
        shape = RoundedCornerShape(16.dp),
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppIcon(AppSymbol.Connection)
                Text(
                    name,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ConnectionStatusLabel(connection.runtime.status)
            }
            Text(
                endpoint,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
