package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.connection_select_hint
import everfold.shared.generated.resources.nav_connections
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 宽屏连接分栏页面：左栏列表 + 右栏详情/新增编辑器，单层 Scaffold + OS4 Backdrop。 */
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
    val backdrop = rememberOptionalLayerBackdrop()
    val selected = connections.find(navigation.selectedId.orEmpty())
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ConnectionsTopBar(
                backdrop = backdrop,
                showBack = navigation.canGoBack,
                adding = navigation.adding,
                onBack = onBack,
                onAdd = { navigation.add() },
            )
        },
    ) { innerPadding ->
        val contentPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), navBottom),
            )
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Row(Modifier.fillMaxSize().optionalLayerBackdrop(backdrop)) {
                pages.SaveableStateProvider("list") {
                    ConnectionList(
                        connections = connections.connections,
                        selectedId = if (navigation.adding) null else navigation.selectedId,
                        query = query,
                        onQueryChange = { query = it },
                        onSelect = { id -> navigation.open(id) },
                        contentPadding = contentPadding,
                        modifier = Modifier.width(300.dp).fillMaxHeight(),
                    )
                }
                VerticalDivider()
                Box(Modifier.weight(1f).fillMaxHeight().padding(contentPadding)) {
                    ConnectionPane(connections, navigation, pages, selected, onNewConversation, overviewExtra)
                }
            }
        }
    }
}

@Composable
private fun ConnectionsTopBar(
    backdrop: LayerBackdrop?,
    showBack: Boolean,
    adding: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
) {
    val addLabel = localizedString(Res.string.connection_add)
    GlassTopAppBar(
        title = localizedString(Res.string.nav_connections),
        expandable = false,
        progressiveBlur = true,
        backdrop = backdrop,
        navigationIcon = {
            if (showBack) {
                GlassIconButton(onClick = onBack) {
                    AppIcon(
                        AppSymbol.Back,
                        modifier = Modifier.size(28.dp),
                        description = localizedString(Res.string.connection_back),
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            if (!adding) {
                GlassIconButton(onClick = onAdd) {
                    AppIcon(
                        AppSymbol.Plus,
                        modifier = Modifier.size(28.dp),
                        description = addLabel,
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    )
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
                    text = localizedString(Res.string.connection_select_hint),
                    modifier = Modifier.padding(24.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
