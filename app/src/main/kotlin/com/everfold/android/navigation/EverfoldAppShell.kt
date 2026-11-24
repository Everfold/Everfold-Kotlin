package com.everfold.android.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import com.everfold.android.R
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.toMotionDurationScale
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
internal fun EverfoldAppShell(
    modifier: Modifier = Modifier,
    pendingSessionId: String? = null,
    onConsumePendingSession: () -> Unit = {},
    sessionGateway: SessionGateway = koinInject(),
) {
    val sessions by sessionGateway.sessions.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(AppDestination.Session)
    var activeSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentRoute = backStack.lastOrNull() as? AppDestination ?: AppDestination.Session
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val animationSpeed = LocalAnimationSpeed.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val actionController = rememberSessionActionController()

    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    ShellSideEffects(
        pendingSessionId = pendingSessionId,
        drawerTargetValue = drawerState.targetValue,
        currentRoute = currentRoute,
        activeSessionId = activeSessionId,
        dismissIme = dismissIme,
        onOpenPendingSession = { sessionId ->
            activeSessionId = sessionId
            backStack.replaceRoot(AppDestination.Session)
            onConsumePendingSession()
        },
    )

    Box(modifier = modifier.fillMaxSize()) {
        EverfoldShellDrawer(
            drawerState = drawerState,
            sessions = sessions,
            backStack = backStack,
            currentRoute = currentRoute,
            activeSessionId = activeSessionId,
            onActiveSessionChange = { activeSessionId = it },
            onOpenSessionMenu = actionController::openMenu,
            onDismissIme = dismissIme,
            onToggleDrawer = { open ->
                dismissIme()
                scope.launch(animationSpeed.toMotionDurationScale()) {
                    if (open) drawerState.open() else drawerState.close()
                }
            },
        )
        ShellOverlays(
            snackbarHostState = snackbarHostState,
            actionController = actionController,
            sessionGateway = sessionGateway,
            scope = scope,
            activeSessionId = activeSessionId,
            onActiveSessionChange = { activeSessionId = it },
            undoLabel = stringResource(R.string.drawer_session_undo),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
private fun EverfoldShellDrawer(
    drawerState: DrawerState,
    sessions: ImmutableList<AgentSessionSummary>,
    backStack: NavBackStack<NavKey>,
    currentRoute: AppDestination,
    activeSessionId: String?,
    onActiveSessionChange: (String?) -> Unit,
    onOpenSessionMenu: (AgentSessionSummary) -> Unit,
    onDismissIme: () -> Unit,
    onToggleDrawer: (Boolean) -> Unit,
) {
    fun openRoot(target: AppDestination) {
        backStack.replaceRoot(target)
        onToggleDrawer(false)
    }

    fun pushFromDrawer(target: AppDestination) {
        if (currentRoute != target) backStack.add(target)
        onToggleDrawer(false)
    }

    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = currentRoute.isRootDestination(),
        drawerContent = {
            EverfoldDrawerContent(
                sessions = sessions,
                currentRoute = currentRoute,
                activeSessionId = activeSessionId,
                onOpenSearch = { pushFromDrawer(AppDestination.SessionSearch) },
                onOpenArchive = { pushFromDrawer(AppDestination.ArchivedSessions) },
                onSelectSession = { sessionId ->
                    onActiveSessionChange(sessionId)
                    openRoot(AppDestination.Session)
                },
                onSessionLongClick = onOpenSessionMenu,
                onOpenRoot = ::openRoot,
            )
        },
    ) {
        AppNavDisplay(
            backStack = backStack,
            activeSessionId = activeSessionId,
            onSessionCreate = onActiveSessionChange,
            onNewSession = { onActiveSessionChange(null) },
            onSelectSessionFromSubPage = { sessionId ->
                onDismissIme()
                onActiveSessionChange(sessionId)
                backStack.replaceRoot(AppDestination.Session)
            },
            onOpenSessionMenu = onOpenSessionMenu,
            onOpenNodes = { openRoot(AppDestination.Nodes) },
            onDismissIme = onDismissIme,
            onOpenDrawer = { onToggleDrawer(true) },
        )
    }
}

@Composable
private fun ShellOverlays(
    snackbarHostState: SnackbarHostState,
    actionController: SessionActionController,
    sessionGateway: SessionGateway,
    scope: CoroutineScope,
    activeSessionId: String?,
    onActiveSessionChange: (String?) -> Unit,
    undoLabel: String,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = snackbarHostState, modifier = modifier)
    SessionActionDialogsHost(
        controller = actionController,
        sessionGateway = sessionGateway,
        scope = scope,
        activeSessionId = activeSessionId,
        onActiveSessionChange = onActiveSessionChange,
        onShowArchiveUndo = { message, onUndo ->
            scope.launch {
                val result =
                    snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                if (result == SnackbarResult.ActionPerformed) onUndo()
            }
        },
    )
}

@Composable
private fun EverfoldDrawerContent(
    sessions: ImmutableList<AgentSessionSummary>,
    currentRoute: AppDestination,
    activeSessionId: String?,
    onOpenSearch: () -> Unit,
    onOpenArchive: () -> Unit,
    onSelectSession: (String?) -> Unit,
    onSessionLongClick: (AgentSessionSummary) -> Unit,
    onOpenRoot: (AppDestination) -> Unit,
) {
    EverfoldDrawerSheet(
        sessions = sessions,
        isNewSessionSelected = currentRoute is AppDestination.Session && activeSessionId == null,
        activeSessionId = if (currentRoute is AppDestination.Session) activeSessionId else null,
        isArchivedSelected = currentRoute is AppDestination.ArchivedSessions,
        isNodesSelected = currentRoute is AppDestination.Nodes,
        isSettingsSelected = currentRoute is AppDestination.Settings,
        onOpenSearch = onOpenSearch,
        onNewSession = { onSelectSession(null) },
        onSelectSession = { id -> onSelectSession(id) },
        onSessionLongClick = onSessionLongClick,
        onOpenArchive = onOpenArchive,
        onOpenNodes = { onOpenRoot(AppDestination.Nodes) },
        onOpenSettings = { onOpenRoot(AppDestination.Settings) },
    )
}

@Composable
private fun ShellSideEffects(
    pendingSessionId: String?,
    drawerTargetValue: DrawerValue,
    currentRoute: AppDestination,
    activeSessionId: String?,
    dismissIme: () -> Unit,
    onOpenPendingSession: (String) -> Unit,
) {
    val latestDismissIme by rememberUpdatedState(dismissIme)
    val latestOnOpenPendingSession by rememberUpdatedState(onOpenPendingSession)

    LaunchedEffect(pendingSessionId) {
        if (!pendingSessionId.isNullOrBlank()) {
            latestOnOpenPendingSession(pendingSessionId)
        }
    }
    LaunchedEffect(drawerTargetValue) {
        if (drawerTargetValue == DrawerValue.Open) latestDismissIme()
    }
    LaunchedEffect(currentRoute, activeSessionId) {
        if (currentRoute !is AppDestination.SessionSearch) latestDismissIme()
    }
}

private fun NavBackStack<NavKey>.replaceRoot(target: AppDestination) {
    if (lastOrNull() != target) {
        clear()
        add(target)
    }
}

private fun AppDestination.isRootDestination(): Boolean =
    this is AppDestination.Session || this is AppDestination.Nodes || this is AppDestination.Settings
