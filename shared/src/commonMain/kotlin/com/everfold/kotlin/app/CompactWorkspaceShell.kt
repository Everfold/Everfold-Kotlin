package com.everfold.kotlin.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.app.navigation.EverfoldRoute
import com.everfold.kotlin.feature.connections.ConnectionList
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.new_session
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.blur.LayerBackdrop
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
                active = navController.backStack.lastOrNull() is EverfoldRoute.Home,
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
        entry<EverfoldRoute.Conversation> {
            CompactConversationEntry(
                deps = deps,
                state = state,
                navController = navController,
                active = navController.backStack.lastOrNull() is EverfoldRoute.Conversation,
                onOpenConversation = openConversation,
            )
        }
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
    active: Boolean,
    onOpenConversation: (String?) -> Unit,
) {
    val routeModifier = if (active) Modifier else Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {}
    ConversationPaneHost(
        deps = deps,
        state = state,
        compactNavigation = true,
        onToggleSessions = { navController.pop() },
        onNewConversation = { onOpenConversation(null) },
        onOpenConnection = { connId ->
            if (deps.workspace.connections.find(connId) != null) {
                state.connectionNavigation.open(connId)
                navController.pushUnique(EverfoldRoute.ConnectionDetail(connId))
            } else {
                state.destination = AppDestination.Connections
                navController.popUntil { it is EverfoldRoute.Home }
            }
        },
        modifier = Modifier.fillMaxSize().then(routeModifier),
    )
}

@Composable
private fun CompactHomeTabScaffold(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    active: Boolean,
    onSelectDestination: (AppDestination) -> Unit,
    onOpenConversation: (String?) -> Unit,
    onOpenConnectionDetail: (String) -> Unit,
    onOpenConnectionAdd: () -> Unit,
) {
    val homeBackdrop = rememberOptionalLayerBackdrop()
    val sessionsScroll = MiuixScrollBehavior()
    val connectionsScroll = MiuixScrollBehavior()
    val settingsScroll = MiuixScrollBehavior()
    val activeScroll =
        when (state.destination) {
            AppDestination.Sessions -> sessionsScroll
            AppDestination.Connections -> connectionsScroll
            AppDestination.Settings -> settingsScroll
        }
    val navItems =
        AppDestination.entries.map { item ->
            GlassNavigationItem(icon = item.icon.imageVector, label = localizedString(item.label))
        }
    val routeModifier = if (active) Modifier else Modifier.focusProperties { canFocus = false }.clearAndSetSemantics {}
    Scaffold(
        modifier = Modifier.fillMaxSize().then(routeModifier),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CompactHomeTopBar(
                destination = state.destination,
                scrollBehavior = activeScroll,
                backdrop = homeBackdrop,
                onNewSession = {
                    state.sessionQuery = ""
                    onOpenConversation(null)
                },
                onAddConnection = onOpenConnectionAdd,
            )
        },
        bottomBar = {
            GlassNavigationBar(
                items = navItems,
                selectedIndex = state.destination.ordinal,
                onSelect = { index -> onSelectDestination(AppDestination.entries[index]) },
                backdrop = homeBackdrop,
            )
        },
    ) { innerPadding ->
        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val listPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = maxOf(innerPadding.calculateBottomPadding(), imeBottom),
            )
        CompositionLocalProvider(LocalAppBackdrop provides homeBackdrop) {
            Box(Modifier.fillMaxSize().optionalLayerBackdrop(homeBackdrop)) {
                CompactHomeTabContent(
                    deps = deps,
                    state = state,
                    contentPadding = listPadding,
                    onOpenConversation = onOpenConversation,
                    onOpenConnectionDetail = onOpenConnectionDetail,
                    modifier = Modifier.fillMaxSize().nestedScroll(activeScroll.nestedScrollConnection),
                )
            }
        }
    }
}

@Composable
private fun CompactHomeTopBar(
    destination: AppDestination,
    scrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
    onNewSession: () -> Unit,
    onAddConnection: () -> Unit,
) {
    GlassTopAppBar(
        title = localizedString(destination.label),
        expandable = true,
        progressiveBlur = true,
        backdrop = backdrop,
        scrollBehavior = scrollBehavior,
        actions = {
            when (destination) {
                AppDestination.Sessions -> {
                    GlassTopBarAddButton(
                        description = localizedString(Res.string.new_session),
                        onClick = onNewSession,
                    )
                }

                AppDestination.Connections -> {
                    GlassTopBarAddButton(
                        description = localizedString(Res.string.connection_add),
                        onClick = onAddConnection,
                    )
                }

                AppDestination.Settings -> {}
            }
        },
    )
}

@Composable
private fun GlassTopBarAddButton(
    description: String,
    onClick: () -> Unit,
) {
    GlassIconButton(onClick = onClick) {
        AppIcon(
            AppSymbol.Plus,
            modifier = Modifier.size(28.dp),
            description = description,
            tint = MiuixTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompactHomeTabContent(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    contentPadding: PaddingValues,
    onOpenConversation: (String?) -> Unit,
    onOpenConnectionDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
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
                showHeader = false,
                contentPadding = contentPadding,
                modifier = modifier,
            )
        }

        AppDestination.Connections -> {
            state.connectionPageStates.SaveableStateProvider("connections") {
                ConnectionList(
                    connections = deps.workspace.connections.connections,
                    selectedId = null,
                    query = state.connectionQuery,
                    onQueryChange = { state.connectionQuery = it },
                    onSelect = onOpenConnectionDetail,
                    contentPadding = contentPadding,
                    modifier = modifier,
                )
            }
        }

        AppDestination.Settings -> {
            state.connectionPageStates.SaveableStateProvider("settings") {
                CompactSettingsContent(
                    deps = deps,
                    contentPadding = contentPadding,
                    modifier = modifier,
                )
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
