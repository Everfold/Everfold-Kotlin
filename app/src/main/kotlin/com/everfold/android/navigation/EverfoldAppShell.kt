package com.everfold.android.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionDetailScreen
import com.everfold.feature.settings.SettingsScreen
import com.everfold.feature.settings.screen.AboutSettingsScreen
import com.everfold.feature.settings.screen.AppearanceSettingsScreen
import com.everfold.feature.settings.screen.LanguageSettingsScreen
import com.everfold.feature.settings.screen.PermissionsSettingsScreen
import com.everfold.feature.settings.screen.SessionSettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
    val backStack = rememberNavBackStack(AppDestination.Session(sessionId = null))
    val currentRoute = backStack.lastOrNull() as? AppDestination ?: AppDestination.Session(null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissIme = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) dismissIme()
    }

    val currentSessionRoute = currentRoute as? AppDestination.Session
    val isRootRoute = currentRoute.isRootDestination()

    fun openRoot(target: AppDestination) {
        if (currentRoute != target) {
            backStack.clear()
            backStack.add(target)
        }
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        modifier = modifier.fillMaxSize(),
        drawerState = drawerState,
        gesturesEnabled = isRootRoute,
        drawerContent = {
            EverfoldDrawerSheet(
                sessions = sessions,
                isNewSessionSelected = currentSessionRoute != null && currentSessionRoute.sessionId == null,
                activeSessionId = currentSessionRoute?.sessionId,
                isNodesSelected = currentRoute is AppDestination.Nodes,
                isSettingsSelected = currentRoute is AppDestination.Settings,
                onNewSession = { openRoot(AppDestination.Session(sessionId = null)) },
                onSelectSession = { id -> openRoot(AppDestination.Session(sessionId = id)) },
                onOpenNodes = { openRoot(AppDestination.Nodes) },
                onOpenSettings = { openRoot(AppDestination.Settings) },
            )
        },
    ) {
        AppNavDisplay(
            backStack = backStack,
            onOpenDrawer = {
                dismissIme()
                scope.launch { drawerState.open() }
            },
        )
    }
}

private fun AppDestination.isRootDestination(): Boolean =
    this is AppDestination.Session || this is AppDestination.Nodes || this is AppDestination.Settings

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onOpenDrawer: () -> Unit,
) {
    val animationSpeed = LocalAnimationSpeed.current
    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = popBack,
        transitionSpec = { forwardTransition(animationSpeed) },
        popTransitionSpec = { popTransition(animationSpeed) },
        predictivePopTransitionSpec = { _ -> popTransition(animationSpeed) },
        entryProvider =
            entryProvider {
                entry<AppDestination.Session> { destination ->
                    SessionDetailScreen(
                        sessionId = destination.sessionId,
                        onOpenDrawer = onOpenDrawer,
                        onSessionCreate = { createdId ->
                            if (backStack.isNotEmpty()) {
                                backStack[backStack.lastIndex] = AppDestination.Session(createdId)
                            }
                        },
                        onNewSession = {
                            if (backStack.isNotEmpty()) {
                                backStack[backStack.lastIndex] = AppDestination.Session(null)
                            }
                        },
                    )
                }
                entry<AppDestination.Nodes> { NodesScreen(onOpenDrawer = onOpenDrawer) }
                entry<AppDestination.Settings> {
                    SettingsScreen(
                        onOpenDrawer = onOpenDrawer,
                        onOpenSection = { section -> backStack.add(section.toDestination()) },
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

private fun forwardTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val enterMs = speed.scaleDuration(NAV_ENTER_DURATION_MS)
        val exitMs = speed.scaleDuration(NAV_EXIT_DURATION_MS)
        (fadeIn(tween(enterMs)) + slideInHorizontally(tween(enterMs)) { it / SLIDE_DIVISOR }) togetherWith
            (fadeOut(tween(exitMs)) + slideOutHorizontally(tween(exitMs)) { -it / SLIDE_DIVISOR })
    }

private fun popTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        val popMs = speed.scaleDuration(NAV_POP_DURATION_MS)
        (fadeIn(tween(popMs)) + slideInHorizontally(tween(popMs)) { -it / SLIDE_DIVISOR }) togetherWith
            (fadeOut(tween(popMs)) + slideOutHorizontally(tween(popMs)) { it / SLIDE_DIVISOR })
    }
