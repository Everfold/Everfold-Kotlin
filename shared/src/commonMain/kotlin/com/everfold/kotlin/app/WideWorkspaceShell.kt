package com.everfold.kotlin.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.feature.connections.ConnectionsScreen
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.horizontalResizePointer
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.resize_sessions
import top.yukonga.miuix.kmp.basic.NavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.VerticalDivider
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun WideWorkspaceShell(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    maxSessionWidth: Float,
) {
    val focusManager = LocalFocusManager.current
    val navigateBack: () -> Unit = {
        if (state.destination != AppDestination.Connections || !state.connectionNavigation.back()) {
            state.destination = AppDestination.Sessions
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.destination != AppDestination.Sessions,
        onBackCompleted = navigateBack,
    )
    Scaffold(
        modifier =
            Modifier.fillMaxSize().imePadding().onPreviewKeyEvent { event ->
                handleWideKeyEvent(
                    event = event,
                    canGoBack = state.destination != AppDestination.Sessions,
                    onBack = navigateBack,
                    onNewConversation = state::startNewConversation,
                    onFindSessions = {
                        state.focusComposer = false
                        state.destination = AppDestination.Sessions
                        state.sessionsVisible = true
                        focusManager.clearFocus()
                        state.searchRequest++
                    },
                    onToggleSessions = {
                        state.focusComposer = true
                        state.sessionsVisible = !state.sessionsVisible
                    },
                )
            },
        contentWindowInsets = WindowInsets(0),
    ) {
        Row(Modifier.fillMaxSize()) {
            NavigationRail(expanded = false, showDivider = true) {
                AppDestination.entries.forEach { item ->
                    NavigationRailItem(
                        selected = state.destination == item,
                        onClick = { state.destination = item },
                        icon = item.icon.imageVector,
                        label = localizedString(item.label),
                    )
                }
            }
            WideWorkspaceContent(deps, state, maxSessionWidth, navigateBack, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun WideWorkspaceContent(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    maxSessionWidth: Float,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        when (state.destination) {
            AppDestination.Sessions -> {
                WideSessionsWorkspace(deps, state, maxSessionWidth)
            }

            AppDestination.Connections -> {
                state.connectionPageStates.SaveableStateProvider("connections") {
                    ConnectionsScreen(
                        connections = deps.workspace.connections,
                        navigation = state.connectionNavigation,
                        onNewConversation = { id ->
                            deps.workspace.selectNewConversationConnection(id)
                            state.startNewConversation()
                        },
                        onBack = navigateBack,
                        overviewExtra = deps.connectionOverviewExtra,
                    )
                }
            }

            AppDestination.Settings -> {
                state.connectionPageStates.SaveableStateProvider("settings") {
                    WideSettingsHost(deps = deps)
                }
            }
        }
    }
}

@Composable
private fun WideSessionsWorkspace(
    deps: WorkspaceShellDeps,
    state: WorkspaceShellState,
    maxSessionWidth: Float,
) {
    Row(Modifier.fillMaxSize()) {
        if (state.sessionsVisible) {
            Surface(
                modifier =
                    Modifier
                        .width(state.sessionWidth.coerceIn(MIN_SESSION_WIDTH, maxSessionWidth).dp)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical)),
                color = MiuixTheme.colorScheme.surfaceContainer,
            ) {
                CompositionLocalProvider(LocalAppBackdrop provides null) {
                    SessionsPanel(
                        deps = deps,
                        state = state,
                        onSelect = {
                            state.focusComposer = true
                            state.selectedId = it
                            state.destination = AppDestination.Sessions
                        },
                        onCreate = state::startNewConversation,
                        showHeader = true,
                        contentPadding = PaddingValues(0.dp),
                    )
                }
            }
            ResizableSessionsDivider(
                sessionWidth = state.sessionWidth,
                maxSessionWidth = maxSessionWidth,
                onSessionWidthChange = { state.sessionWidth = it },
            )
        }
        ConversationPaneHost(
            deps = deps,
            state = state,
            compactNavigation = false,
            onToggleSessions = {
                state.focusComposer = true
                state.sessionsVisible = !state.sessionsVisible
            },
            onNewConversation = state::startNewConversation,
            onOpenConnection = { connId ->
                if (deps.workspace.connections.find(connId) != null) {
                    state.connectionNavigation.open(connId)
                } else {
                    state.connectionNavigation.showList()
                }
                state.destination = AppDestination.Connections
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ResizableSessionsDivider(
    sessionWidth: Float,
    maxSessionWidth: Float,
    onSessionWidthChange: (Float) -> Unit,
) {
    val resizeLabel = localizedString(Res.string.resize_sessions)
    val density = LocalDensity.current.density
    Box(
        Modifier
            .width(if (desktopUi) 10.dp else 1.dp)
            .fillMaxHeight()
            .horizontalResizePointer()
            .draggable(
                rememberDraggableState { delta ->
                    val base = sessionWidth.coerceAtMost(maxSessionWidth)
                    onSessionWidthChange((base + delta / density).coerceIn(MIN_SESSION_WIDTH, maxSessionWidth))
                },
                Orientation.Horizontal,
            ).semantics { contentDescription = resizeLabel }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)) {
                    val delta = if (event.key == Key.DirectionLeft) -KEYBOARD_RESIZE_STEP else KEYBOARD_RESIZE_STEP
                    onSessionWidthChange((sessionWidth + delta).coerceIn(MIN_SESSION_WIDTH, maxSessionWidth))
                    true
                } else {
                    false
                }
            }.focusable(),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDivider()
    }
}

private fun handleWideKeyEvent(
    event: KeyEvent,
    canGoBack: Boolean,
    onBack: () -> Unit,
    onNewConversation: () -> Unit,
    onFindSessions: () -> Unit,
    onToggleSessions: () -> Unit,
): Boolean {
    val shortcutKey = event.type == KeyEventType.KeyDown && (event.isCtrlPressed || event.isMetaPressed)
    return when {
        event.type == KeyEventType.KeyDown && event.key == Key.Escape && canGoBack -> {
            onBack()
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

        shortcutKey && event.key == Key.B -> {
            onToggleSessions()
            true
        }

        else -> {
            false
        }
    }
}
