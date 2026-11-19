package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.connection_select_hint
import everfold.shared.generated.resources.nav_connections

/** 宽屏并列展示列表和详情窄屏逐级进入同一详情状态随连接 ID 保存 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionsScreen(
    connections: ConnectionManager,
    navigation: ConnectionNavigationState,
    onNewConversation: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    overviewExtra: @Composable (String) -> Unit = {},
) {
    val pages = rememberSaveableStateHolder()
    var query by rememberSaveable { mutableStateOf("") }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val selected = connections.find(navigation.selectedId.orEmpty())
        val showingDetail = navigation.adding || selected != null
        Column {
            TopAppBar(
                title = { Text(localizedString(Res.string.nav_connections)) },
                navigationIcon = {
                    if (!wide || navigation.canGoBack) {
                        IconButton(onClick = onBack) {
                            AppIcon(AppSymbol.Back, description = localizedString(Res.string.connection_back))
                        }
                    }
                },
                actions = {
                    if (!navigation.adding) {
                        TextButton(onClick = navigation::add) {
                            AppIcon(AppSymbol.Plus)
                            Text(localizedString(Res.string.connection_add), Modifier.padding(start = 8.dp))
                        }
                    }
                },
            )
            Row(
                Modifier
                    .weight(1f)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
            ) {
                if (wide || !showingDetail) {
                    pages.SaveableStateProvider("list") {
                        ConnectionList(
                            connections.connections,
                            selectedId = if (navigation.adding) null else navigation.selectedId,
                            query = query,
                            onQueryChange = { query = it },
                            onSelect = navigation::open,
                            modifier = if (wide) Modifier.width(300.dp) else Modifier.weight(1f),
                        )
                    }
                    if (wide) VerticalDivider()
                }
                if (wide || showingDetail) {
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        ConnectionPane(connections, navigation, pages, selected, onNewConversation, overviewExtra)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionPane(
    connections: ConnectionManager,
    navigation: ConnectionNavigationState,
    pages: SaveableStateHolder,
    selected: Connection?,
    onNewConversation: (String) -> Unit,
    overviewExtra: @Composable (String) -> Unit,
) {
    when {
        navigation.adding -> {
            pages.SaveableStateProvider("add") {
                ConnectionEditor(
                    profile = null,
                    onSave = { draft, connect ->
                        val id = connections.add(draft.name.orEmpty(), draft.setup ?: ClusterSetup(), draft.autoConnect)
                        if (id != null) {
                            if (connect) connections.connect(id)
                            navigation.open(id)
                            pages.removeState("add")
                        }
                        id != null
                    },
                    onCancel = { navigation.back() },
                )
            }
        }

        selected != null -> {
            pages.SaveableStateProvider(selected.id) {
                ConnectionDetails(
                    connection = selected,
                    onConnect = { connections.connect(selected.id) },
                    onDisconnect = { connections.disconnect(selected.id) },
                    onSave = { draft -> connections.save(selected.id, draft.name, draft.setup, draft.autoConnect) },
                    onRemove = {
                        connections.remove(selected.id)
                        navigation.back()
                        pages.removeState(selected.id)
                    },
                    onNewConversation = { onNewConversation(selected.id) },
                    overviewExtra = { overviewExtra(selected.id) },
                )
            }
        }

        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    localizedString(Res.string.connection_select_hint),
                    Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
