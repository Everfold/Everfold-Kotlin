package com.everfold.kotlin.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.app.navigation.EverfoldRoute
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionDetails
import com.everfold.kotlin.feature.connections.ConnectionEditor
import com.everfold.kotlin.feature.connections.ConnectionsScreen
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.nav_connections
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassNavigationBar
import top.yukonga.miuix.kmp.glass.GlassNavigationItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.nav.core.NavController
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavController
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CompactWorkspaceShell(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
) {
    val focusManager = LocalFocusManager.current
    val navController = rememberNavController<EverfoldRoute>(EverfoldRoute.Home)
    val openConversation: (String?) -> Unit = { targetId ->
        state.focusComposer = true
        state.selectedId = targetId
        if (targetId == null) state.composerFocusRequest++
        navController.openConversation()
    }
    val cornerRadius = rememberNavSystemCornerRadius()
    val backdropColor = MiuixTheme.colorScheme.surface
    val effects =
        remember(cornerRadius, backdropColor) { NavDisplayEffects(cornerClipRadius = cornerRadius, backdropColor = backdropColor) }

    NavDisplay(
        navController = navController,
        modifier = compactNavModifier(navController, state, openConversation),
        transition = NavTransitions.MiuixDefault,
        effects = effects,
    ) {
        entry<EverfoldRoute.Home>(swipeDismiss = NavSwipeDirection.None) {
            CompactHomeTabScaffold(
                deps = deps,
                state = state,
                onSelectDestination = {
                    focusManager.clearFocus()
                    state.destination = it
                },
                onOpenConversation = openConversation,
                onOpenConnectionDetail = { id ->
                    state.connectionNavigation.open(id)
                    navController.pushUnique(EverfoldRoute.ConnectionDetail(id))
                },
                onOpenConnectionAdd = { navController.pushUnique(EverfoldRoute.ConnectionAdd) },
            )
        }
        entry<EverfoldRoute.Conversation> { CompactConversationEntry(deps, state, navController, openConversation) }
        entry<EverfoldRoute.ConnectionDetail> { route ->
            CompactConnectionDetailPage(route, deps, state, navController) { id ->
                deps.workspace.selectNewConversationConnection(id)
                state.sessionQuery = ""
                openConversation(null)
            }
        }
        entry<EverfoldRoute.ConnectionAdd> { CompactConnectionAddPage(deps, state, navController) }
    }
}

@Composable
private fun compactNavModifier(
    navController: NavController,
    state: WorkspaceShellState,
    openConversation: (String?) -> Unit,
): Modifier {
    val focusManager = LocalFocusManager.current
    return Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        handleCompactKeyEvent(
            event = event,
            canPop = navController.backStack.size > 1,
            onPop = { navController.pop() },
            onNewConversation = {
                state.sessionQuery = ""
                openConversation(null)
            },
            onFindSessions = {
                state.focusComposer = false
                state.destination = AppDestination.Sessions
                navController.popUntil { it is EverfoldRoute.Home }
                focusManager.clearFocus()
                state.searchRequest++
            },
        )
    }
}

@Composable
private fun CompactConversationEntry(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    navController: NavController,
    openConversation: (String?) -> Unit,
) {
    ConversationPaneHost(
        deps = deps,
        state = state,
        compactNavigation = true,
        onToggleSessions = { navController.pop() },
        onNewConversation = { openConversation(null) },
        onOpenConnection = { connId ->
            if (deps.workspace.connections.find(connId) != null) {
                state.connectionNavigation.open(connId)
                navController.pushUnique(EverfoldRoute.ConnectionDetail(connId))
            } else {
                state.destination = AppDestination.Connections
                navController.popUntil { it is EverfoldRoute.Home }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CompactHomeTabScaffold(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    onSelectDestination: (AppDestination) -> Unit,
    onOpenConversation: (String?) -> Unit,
    onOpenConnectionDetail: (String) -> Unit,
    onOpenConnectionAdd: () -> Unit,
) {
    val homeBackdrop = rememberLayerBackdrop()
    val navItems =
        AppDestination.entries.map { item ->
            GlassNavigationItem(icon = item.icon.imageVector, label = localizedString(item.label))
        }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            GlassNavigationBar(
                items = navItems,
                selectedIndex = state.destination.ordinal,
                onSelect = { index -> onSelectDestination(AppDestination.entries[index]) },
                backdrop = homeBackdrop,
            )
        },
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding()
        CompositionLocalProvider(LocalAppBackdrop provides homeBackdrop) {
            Box(Modifier.fillMaxSize().layerBackdrop(homeBackdrop)) {
                CompactHomeTabContent(
                    deps = deps,
                    state = state,
                    bottomPadding = bottomPadding,
                    onSelectDestination = onSelectDestination,
                    onOpenConversation = onOpenConversation,
                    onOpenConnectionDetail = onOpenConnectionDetail,
                    onOpenConnectionAdd = onOpenConnectionAdd,
                )
            }
        }
    }
}

@Composable
private fun CompactHomeTabContent(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSelectDestination: (AppDestination) -> Unit,
    onOpenConversation: (String?) -> Unit,
    onOpenConnectionDetail: (String) -> Unit,
    onOpenConnectionAdd: () -> Unit,
) {
    when (state.destination) {
        AppDestination.Sessions -> {
            SessionsPanel(
                deps = deps,
                state = state,
                onSelect = { onOpenConversation(it) },
                onCreate = {
                    state.sessionQuery = ""
                    onOpenConversation(null)
                },
                compactMode = true,
                bottomPadding = bottomPadding,
            )
        }

        AppDestination.Connections -> {
            state.connectionPageStates.SaveableStateProvider("connections") {
                ConnectionsScreen(
                    connections = deps.workspace.connections,
                    navigation = state.connectionNavigation,
                    onNewConversation = { id ->
                        deps.workspace.selectNewConversationConnection(id)
                        state.sessionQuery = ""
                        onOpenConversation(null)
                    },
                    onBack = { onSelectDestination(AppDestination.Sessions) },
                    onOpenConnection = onOpenConnectionDetail,
                    onAddConnection = onOpenConnectionAdd,
                    bottomPadding = bottomPadding,
                )
            }
        }

        AppDestination.Settings -> {
            SettingsHost(deps = deps, bottomPadding = bottomPadding)
        }
    }
}

@Composable
private fun CompactConnectionDetailPage(
    route: EverfoldRoute.ConnectionDetail,
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    navController: NavController,
    onNewConversation: (String) -> Unit,
) {
    val connection = deps.workspace.connections.find(route.connectionId)
    val title = connection?.profile?.name?.resolve() ?: localizedString(Res.string.nav_connections)
    CompactSubpageScaffold(title = title, onBack = { navController.pop() }) { innerPadding ->
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
private fun CompactConnectionAddPage(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    navController: NavController,
) {
    CompactSubpageScaffold(
        title = localizedString(Res.string.connection_add),
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
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
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
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                content(innerPadding)
            }
        }
    }
}

private fun handleCompactKeyEvent(
    event: KeyEvent,
    canPop: Boolean,
    onPop: () -> Unit,
    onNewConversation: () -> Unit,
    onFindSessions: () -> Unit,
): Boolean {
    val shortcutKey = event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)
    return when {
        event.type == KeyEventType.KeyDown && event.key == Key.Escape && canPop -> {
            onPop()
            true
        }

        shortcutKey && event.key == Key.N -> {
            onNewConversation()
            true
        }

        shortcutKey && event.key == Key.F -> {
            onFindSessions()
            true
        }

        else -> {
            false
        }
    }
}

private fun NavController.pushUnique(route: EverfoldRoute) {
    if (backStack.lastOrNull() == route) return
    if (route in backStack) {
        popUntil { it == route }
    } else {
        push(route)
    }
}

private fun NavController.openConversation() {
    if (backStack.lastOrNull() is EverfoldRoute.Conversation) return
    popUntil { it is EverfoldRoute.Home }
    push(EverfoldRoute.Conversation)
}
