package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
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
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val UNSELECTED_SURFACE_ALPHA = 0.65f

/** 连接列表：统计标题与搜索栏放入同一滚动列表，支持顶底玻璃穿透。 */
@Composable
internal fun ConnectionList(
    connections: List<Connection>,
    selectedId: String?,
    query: String,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val names = connections.associate { it.id to it.profile.name.resolve() }
    val endpoints = connections.associate { it.id to it.profile.endpointSummary() }
    val filtered =
        connections.filter {
            names.getValue(it.id).contains(query, ignoreCase = true) || endpoints.getValue(it.id).contains(query, ignoreCase = true)
        }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "count") {
            SmallTitle(
                text = localizedString(Res.string.connection_count, connections.size, connections.count { it.isConnected }),
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            )
        }
        item(key = "search") {
            ConnectionSearchBar(query = query, onQueryChange = onQueryChange)
        }
        if (filtered.isEmpty()) {
            item(key = "empty") {
                ConnectionNoResults(onClearQuery = { onQueryChange("") }, showClear = query.isNotEmpty())
            }
        }
        items(filtered, key = { it.id }) { connection ->
            ConnectionListItem(
                connection = connection,
                name = names.getValue(connection.id),
                endpoint = endpoints.getValue(connection.id),
                selected = connection.id == selectedId,
                onSelect = { onSelect(connection.id) },
            )
        }
    }
}

@Composable
private fun ConnectionSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        insideMargin = DpSize(12.dp, 10.dp),
        cornerRadius = 14.dp,
        label = localizedString(Res.string.connection_search),
        useLabelAsPlaceholder = true,
        singleLine = true,
        leadingIcon = {
            AppIcon(
                AppSymbol.Search,
                modifier = Modifier.padding(start = 12.dp, end = 6.dp).size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.padding(end = 6.dp).size(28.dp)) {
                    AppIcon(
                        AppSymbol.Close,
                        modifier = Modifier.size(16.dp),
                        description = localizedString(Res.string.clear_search),
                    )
                }
            }
        },
        colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                borderColor = MiuixTheme.colorScheme.primary,
            ),
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
    )
}

@Composable
private fun ConnectionNoResults(
    showClear: Boolean,
    onClearQuery: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = localizedString(Res.string.connection_no_results),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (showClear) {
            TextButton(
                text = localizedString(Res.string.clear_search),
                onClick = onClearQuery,
                colors = ButtonDefaults.textButtonColorsPrimary(),
            )
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
        text = localizedString(status.label),
        style = MiuixTheme.textStyles.footnote1,
        fontWeight = FontWeight.Medium,
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
    val containerColor =
        if (selected) {
            MiuixTheme.colorScheme.secondaryContainer
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = UNSELECTED_SURFACE_ALPHA)
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .squircleSurface(color = containerColor, cornerRadius = 16.dp)
                .selectable(
                    selected = selected,
                    role = Role.Tab,
                    onClick = onSelect,
                ),
        color = Color.Transparent,
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AppIcon(AppSymbol.Connection, modifier = Modifier.size(18.dp))
                Text(
                    text = name,
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.headline1,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ConnectionStatusLabel(connection.runtime.status)
            }
            Text(
                text = endpoint,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
