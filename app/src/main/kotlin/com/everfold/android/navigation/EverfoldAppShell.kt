package com.everfold.android.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.designsystem.theme.toMotionDurationScale
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionDetailScreen
import com.everfold.feature.sessions.search.SessionSearchScreen
import com.everfold.feature.settings.SettingsScreen
import com.everfold.feature.settings.screen.AboutSettingsScreen
import com.everfold.feature.settings.screen.AppearanceSettingsScreen
import com.everfold.feature.settings.screen.LanguageSettingsScreen
import com.everfold.feature.settings.screen.PermissionsSettingsScreen
import com.everfold.feature.settings.screen.SessionSettingsScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val FADE_THROUGH_OUT_MS = 90
private const val FADE_THROUGH_IN_MS = 210
private const val FADE_THROUGH_INITIAL_SCALE = 0.92f
private const val NAV_ENTER_DURATION_MS = 200
private const val NAV_EXIT_DURATION_MS = 140
private const val NAV_POP_DURATION_MS = 180
private const val SLIDE_DIVISOR = 12

@Composable
internal fun EverfoldAppShell(
    modifier: Modifier = Modifier,
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

    val dismissIme: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) dismissIme()
    }
    LaunchedEffect(currentRoute, activeSessionId) {
        if (currentRoute !is AppDestination.SessionSearch) dismissIme()
    }

    fun openRoot(target: AppDestination) {
        dismissIme()
        backStack.replaceRoot(target)
        scope.launch(animationSpeed.toMotionDurationScale()) { drawerState.close() }
    }

    ModalNavigationDrawer(
        modifier = modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = currentRoute.isRootDestination(),
        drawerContent = {
            EverfoldDrawerContent(
                sessions = sessions,
                currentRoute = currentRoute,
                activeSessionId = activeSessionId,
                onOpenSearch = {
                    dismissIme()
                    if (currentRoute !is AppDestination.SessionSearch) backStack.add(AppDestination.SessionSearch)
                    scope.launch(animationSpeed.toMotionDurationScale()) { drawerState.close() }
                },
                onSelectSession = { sessionId ->
                    activeSessionId = sessionId
                    openRoot(AppDestination.Session)
                },
                onOpenRoot = ::openRoot,
            )
        },
    ) {
        AppNavDisplay(
            backStack = backStack,
            activeSessionId = activeSessionId,
            onSessionCreate = { activeSessionId = it },
            onNewSession = { activeSessionId = null },
            onSelectSessionFromSearch = { sessionId ->
                dismissIme()
                activeSessionId = sessionId
                backStack.replaceRoot(AppDestination.Session)
            },
            onDismissIme = dismissIme,
            onOpenDrawer = {
                dismissIme()
                scope.launch(animationSpeed.toMotionDurationScale()) { drawerState.open() }
            },
        )
    }
}

@Composable
private fun EverfoldDrawerContent(
    sessions: ImmutableList<AgentSessionSummary>,
    currentRoute: AppDestination,
    activeSessionId: String?,
    onOpenSearch: () -> Unit,
    onSelectSession: (String?) -> Unit,
    onOpenRoot: (AppDestination) -> Unit,
) {
    EverfoldDrawerSheet(
        sessions = sessions,
        isNewSessionSelected = currentRoute is AppDestination.Session && activeSessionId == null,
        activeSessionId = if (currentRoute is AppDestination.Session) activeSessionId else null,
        isNodesSelected = currentRoute is AppDestination.Nodes,
        isSettingsSelected = currentRoute is AppDestination.Settings,
        onOpenSearch = onOpenSearch,
        onNewSession = { onSelectSession(null) },
        onSelectSession = { id -> onSelectSession(id) },
        onOpenNodes = { onOpenRoot(AppDestination.Nodes) },
        onOpenSettings = { onOpenRoot(AppDestination.Settings) },
    )
}

private fun NavBackStack<NavKey>.replaceRoot(target: AppDestination) {
    if (lastOrNull() != target) {
        clear()
        add(target)
    }
}

private fun AppDestination.isRootDestination(): Boolean =
    this is AppDestination.Session || this is AppDestination.Nodes || this is AppDestination.Settings

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    activeSessionId: String?,
    onSessionCreate: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectSessionFromSearch: (String) -> Unit,
    onDismissIme: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val animationSpeed = LocalAnimationSpeed.current
    val popBack: () -> Unit = {
        onDismissIme()
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = popBack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        transitionSpec = {
            if (backStack.size > 1) {
                forwardSlideTransition(animationSpeed)
            } else {
                peerFadeThroughTransition(animationSpeed)
            }
        },
        popTransitionSpec = { popSlideTransition(animationSpeed) },
        predictivePopTransitionSpec = { _ -> popSlideTransition(animationSpeed) },
        entryProvider =
            entryProvider {
                entry<AppDestination.Session> {
                    SessionDetailScreen(
                        sessionId = activeSessionId,
                        onOpenDrawer = onOpenDrawer,
                        onSessionCreate = onSessionCreate,
                        onNewSession = onNewSession,
                    )
                }
                entry<AppDestination.SessionSearch> {
                    SessionSearchScreen(
                        onBack = popBack,
                        onSelectSession = onSelectSessionFromSearch,
                    )
                }
                entry<AppDestination.Nodes> { NodesScreen(onOpenDrawer = onOpenDrawer) }
                entry<AppDestination.Settings> {
                    SettingsScreen(
                        onOpenDrawer = onOpenDrawer,
                        onOpenSection = { section ->
                            onDismissIme()
                            backStack.add(section.toDestination())
                        },
                    )
                }
                entry<AppDestination.SettingsAppearance> { AppearanceSettingsScreen(onBack = popBack) }
                entry<AppDestination.SettingsLanguage> { LanguageSettingsScreen(onBack = popBack) }
                entry<AppDestination.SettingsSessions> { SessionSettingsScreen(onBack = popBack) }
                entry<AppDestination.SettingsPermissions> { PermissionsSettingsScreen(onBack = popBack) }
                entry<AppDestination.SettingsAbout> { AboutSettingsScreen(onBack = popBack) }
            },
    )
}

private fun peerFadeThroughTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val outMs = speed.scaleDuration(FADE_THROUGH_OUT_MS)
        val inMs = speed.scaleDuration(FADE_THROUGH_IN_MS)
        val inSpec = tween<Float>(inMs, delayMillis = outMs)
        (fadeIn(inSpec) + scaleIn(inSpec, initialScale = FADE_THROUGH_INITIAL_SCALE)) togetherWith
            fadeOut(tween(outMs))
    }

private fun forwardSlideTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val enterMs = speed.scaleDuration(NAV_ENTER_DURATION_MS)
        val exitMs = speed.scaleDuration(NAV_EXIT_DURATION_MS)
        (fadeIn(tween(enterMs)) + slideInHorizontally(tween(enterMs)) { it / SLIDE_DIVISOR }) togetherWith
            (fadeOut(tween(exitMs)) + slideOutHorizontally(tween(exitMs)) { -it / SLIDE_DIVISOR })
    }

private fun popSlideTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val popMs = speed.scaleDuration(NAV_POP_DURATION_MS)
        (fadeIn(tween(popMs)) + slideInHorizontally(tween(popMs)) { -it / SLIDE_DIVISOR }) togetherWith
            (fadeOut(tween(popMs)) + slideOutHorizontally(tween(popMs)) { it / SLIDE_DIVISOR })
    }
