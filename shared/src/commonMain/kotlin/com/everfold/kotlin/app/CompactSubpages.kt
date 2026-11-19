package com.everfold.kotlin.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.app.navigation.EverfoldRoute
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionDetails
import com.everfold.kotlin.feature.connections.ConnectionEditor
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.nav_connections
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CompactConnectionDetailPage(
    route: EverfoldRoute.ConnectionDetail,
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    navController: NavController,
    onNewConversation: (String) -> Unit,
) {
    val connection = deps.workspace.connections.find(route.connectionId)
    val title = connection?.profile?.name?.resolve() ?: localizedString(Res.string.nav_connections)
    val active = navController.backStack.lastOrNull() == route
    CompactSubpageScaffold(title = title, active = active, onBack = { navController.pop() }) { innerPadding ->
        if (connection != null) {
            state.connectionPageStates.SaveableStateProvider(connection.id) {
                ConnectionDetails(
                    connection = connection,
                    onConnect = { deps.workspace.connections.connect(connection.id) },
                    onDisconnect = { deps.workspace.connections.disconnect(connection.id) },
                    onSave = { draft -> deps.workspace.connections.save(connection.id, draft.name, draft.setup, draft.autoConnect) },
                    onRemove = {
                        deps.workspace.connections.remove(connection.id)
                        navController.pop()
                        state.connectionPageStates.removeState(connection.id)
                    },
                    onNewConversation = { onNewConversation(connection.id) },
                    modifier = Modifier.padding(innerPadding),
                    overviewExtra = { deps.connectionOverviewExtra(connection.id) },
                )
            }
        }
    }
}

@Composable
internal fun CompactConnectionAddPage(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    navController: NavController,
) {
    CompactSubpageScaffold(
        title = localizedString(Res.string.connection_add),
        active = navController.backStack.lastOrNull() is EverfoldRoute.ConnectionAdd,
        onBack = { navController.pop() },
    ) { innerPadding ->
        state.connectionPageStates.SaveableStateProvider("add") {
            ConnectionEditor(
                profile = null,
                onSave = { draft, connect ->
                    val id = deps.workspace.connections.add(draft.name.orEmpty(), draft.setup ?: ClusterSetup(), draft.autoConnect)
                    if (id != null) {
                        if (connect) deps.workspace.connections.connect(id)
                        state.connectionNavigation.open(id)
                        navController.replace(EverfoldRoute.ConnectionDetail(id))
                        state.connectionPageStates.removeState("add")
                    }
                    id != null
                },
                modifier = Modifier.padding(innerPadding),
                onCancel = { navController.pop() },
            )
        }
    }
}

@Composable
private fun CompactSubpageScaffold(
    title: String,
    active: Boolean,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberOptionalLayerBackdrop()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val routeModifier = if (active) Modifier else Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {}
    Scaffold(
        modifier = Modifier.fillMaxSize().then(routeModifier).imePadding(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            GlassTopAppBar(
                title = title,
                expandable = false,
                progressiveBlur = true,
                backdrop = backdrop,
                navigationIcon = {
                    GlassIconButton(onClick = onBack) {
                        AppIcon(
                            AppSymbol.Back,
                            modifier = Modifier.size(28.dp),
                            description = localizedString(Res.string.connection_back),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val resolvedPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = if (imeOpen) 0.dp else maxOf(innerPadding.calculateBottomPadding(), navBottom),
            )
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize().optionalLayerBackdrop(backdrop)) {
                content(resolvedPadding)
            }
        }
    }
}
