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

private const val PEER_ENTER_DURATION_MS = 150
private const val PEER_EXIT_DURATION_MS = 100
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
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val dismissIme = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Open) dismissIme()
    }

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
        gesturesEnabled = currentRoute.isRootDestination(),
        drawerContent = {
            EverfoldDrawerSheet(
                sessions = sessions,
                isNewSessionSelected = currentRoute is AppDestination.Session && activeSessionId == null,
                activeSessionId = if (currentRoute is AppDestination.Session) activeSessionId else null,
                isNodesSelected = currentRoute is AppDestination.Nodes,
                isSettingsSelected = currentRoute is AppDestination.Settings,
                onNewSession = {
                    activeSessionId = null
                    openRoot(AppDestination.Session)
                },
                onSelectSession = { id ->
                    activeSessionId = id
                    openRoot(AppDestination.Session)
                },
                onOpenNodes = { openRoot(AppDestination.Nodes) },
                onOpenSettings = { openRoot(AppDestination.Settings) },
            )
        },
    ) {
        AppNavDisplay(
            backStack = backStack,
            activeSessionId = activeSessionId,
            onSessionCreate = { activeSessionId = it },
            onNewSession = { activeSessionId = null },
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
    activeSessionId: String?,
    onSessionCreate: (String) -> Unit,
    onNewSession: () -> Unit,
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
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        transitionSpec = {
            if (backStack.size > 1) {
                forwardSlideTransition(animationSpeed)
            } else {
                peerCrossfadeTransition(animationSpeed)
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

private fun peerCrossfadeTransition(speed: AnimationSpeed): ContentTransform =
    if (speed == AnimationSpeed.OFF) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        fadeIn(tween(speed.scaleDuration(PEER_ENTER_DURATION_MS))) togetherWith
            fadeOut(tween(speed.scaleDuration(PEER_EXIT_DURATION_MS)))
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
