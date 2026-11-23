package com.everfold.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.everfold.core.gateway.SessionGateway
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionDetailScreen
import com.everfold.feature.settings.AboutSettingsScreen
import com.everfold.feature.settings.AppearanceSettingsScreen
import com.everfold.feature.settings.LanguageSettingsScreen
import com.everfold.feature.settings.PermissionsSettingsScreen
import com.everfold.feature.settings.SessionSettingsScreen
import com.everfold.feature.settings.SettingsScreen
import com.everfold.feature.settings.SettingsSection
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
private sealed interface AppDestination : NavKey {
    @Serializable
    data class Session(
        val sessionId: String? = null,
    ) : AppDestination

    @Serializable
    data object Nodes : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data object SettingsAppearance : AppDestination

    @Serializable
    data object SettingsLanguage : AppDestination

    @Serializable
    data object SettingsSessions : AppDestination

    @Serializable
    data object SettingsPermissions : AppDestination

    @Serializable
    data object SettingsAbout : AppDestination
}

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

    val currentSessionRoute = currentRoute as? AppDestination.Session
    val isRootRoute =
        currentRoute is AppDestination.Session ||
            currentRoute is AppDestination.Nodes ||
            currentRoute is AppDestination.Settings

    fun openRoot(target: AppDestination) {
        scope.launch {
            drawerState.close()
            if (currentRoute != target) {
                backStack.clear()
                backStack.add(target)
            }
        }
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
            onOpenDrawer = { scope.launch { drawerState.open() } },
        )
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    onOpenDrawer: () -> Unit,
) {
    val popBack: () -> Unit = {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = popBack,
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

private fun SettingsSection.toDestination(): AppDestination =
    when (this) {
        SettingsSection.APPEARANCE -> AppDestination.SettingsAppearance
        SettingsSection.LANGUAGE -> AppDestination.SettingsLanguage
        SettingsSection.SESSIONS -> AppDestination.SettingsSessions
        SettingsSection.PERMISSIONS -> AppDestination.SettingsPermissions
        SettingsSection.ABOUT -> AppDestination.SettingsAbout
    }
