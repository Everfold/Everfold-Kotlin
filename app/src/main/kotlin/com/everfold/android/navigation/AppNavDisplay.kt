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
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.scaleDuration
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionDetailScreen
import com.everfold.feature.sessions.archived.ArchivedSessionsScreen
import com.everfold.feature.sessions.search.SessionSearchScreen
import com.everfold.feature.settings.SettingsScreen
import com.everfold.feature.settings.screen.AboutSettingsScreen
import com.everfold.feature.settings.screen.AppearanceSettingsScreen
import com.everfold.feature.settings.screen.LanguageSettingsScreen
import com.everfold.feature.settings.screen.PermissionsSettingsScreen
import com.everfold.feature.settings.screen.SessionSettingsScreen

private const val FADE_THROUGH_OUT_MS = 90
private const val FADE_THROUGH_IN_MS = 210
private const val FADE_THROUGH_INITIAL_SCALE = 0.92f
private const val NAV_ENTER_DURATION_MS = 200
private const val NAV_EXIT_DURATION_MS = 140
private const val NAV_POP_DURATION_MS = 180
private const val SLIDE_DIVISOR = 12

@Composable
internal fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    activeSessionId: String?,
    onSessionCreate: (String) -> Unit,
    onNewSession: () -> Unit,
    onSelectSessionFromSubPage: (String) -> Unit,
    onOpenSessionMenu: (AgentSessionSummary) -> Unit,
    onDismissIme: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val animationSpeed = LocalAnimationSpeed.current
    val popBack: () -> Unit = {
        onDismissIme()
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
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
            if (backStack.size > 1) forwardSlideTransition(animationSpeed) else peerFadeThroughTransition(animationSpeed)
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
                        onOpenSessionMenu = onOpenSessionMenu,
                    )
                }
                entry<AppDestination.SessionSearch> {
                    SessionSearchScreen(onBack = popBack, onSelectSession = onSelectSessionFromSubPage)
                }
                entry<AppDestination.ArchivedSessions> {
                    ArchivedSessionsScreen(onBack = popBack, onSelectSession = onSelectSessionFromSubPage)
                }
                entry<AppDestination.Nodes> { NodesScreen(onOpenDrawer = onOpenDrawer) }
                settingsEntries(backStack = backStack, onOpenDrawer = onOpenDrawer, onDismissIme = onDismissIme, onBack = popBack)
            },
    )
}

private fun EntryProviderScope<NavKey>.settingsEntries(
    backStack: NavBackStack<NavKey>,
    onOpenDrawer: () -> Unit,
    onDismissIme: () -> Unit,
    onBack: () -> Unit,
) {
    entry<AppDestination.Settings> {
        SettingsScreen(
            onOpenDrawer = onOpenDrawer,
            onOpenSection = { section ->
                onDismissIme()
                backStack.add(section.toDestination())
            },
        )
    }
    entry<AppDestination.SettingsAppearance> { AppearanceSettingsScreen(onBack = onBack) }
    entry<AppDestination.SettingsLanguage> { LanguageSettingsScreen(onBack = onBack) }
    entry<AppDestination.SettingsSessions> {
        SessionSettingsScreen(
            onBack = onBack,
            onOpenArchivedSessions = {
                onDismissIme()
                backStack.add(AppDestination.ArchivedSessions)
            },
        )
    }
    entry<AppDestination.SettingsPermissions> { PermissionsSettingsScreen(onBack = onBack) }
    entry<AppDestination.SettingsAbout> { AboutSettingsScreen(onBack = onBack) }
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
