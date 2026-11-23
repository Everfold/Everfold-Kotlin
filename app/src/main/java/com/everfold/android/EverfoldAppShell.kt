package com.everfold.android

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.everfold.feature.models.ModelsScreen
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionDetailScreen
import com.everfold.feature.sessions.SessionsScreen
import com.everfold.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
private sealed interface AppDestination : NavKey {
    @Serializable
    sealed interface TopLevel : AppDestination {
        @Serializable
        data object Sessions : TopLevel

        @Serializable
        data object Nodes : TopLevel

        @Serializable
        data object Models : TopLevel

        @Serializable
        data object Settings : TopLevel
    }

    @Serializable
    data class SessionDetail(
        val sessionId: String? = null,
    ) : AppDestination
}

private enum class EverfoldTab(
    val route: AppDestination.TopLevel,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    SESSIONS(AppDestination.TopLevel.Sessions, R.string.tab_sessions, Icons.Rounded.ChatBubbleOutline),
    NODES(AppDestination.TopLevel.Nodes, R.string.tab_nodes, Icons.Rounded.Hub),
    MODELS(AppDestination.TopLevel.Models, R.string.tab_models, Icons.Rounded.Memory),
    SETTINGS(AppDestination.TopLevel.Settings, R.string.tab_settings, Icons.Rounded.Tune),
}

@Composable
internal fun EverfoldAppShell(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(AppDestination.TopLevel.Sessions)
    val currentRoute = backStack.lastOrNull() as? AppDestination ?: AppDestination.TopLevel.Sessions
    val defaultLayoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    val layoutType =
        if (currentRoute is AppDestination.TopLevel) {
            defaultLayoutType
        } else {
            NavigationSuiteType.None
        }

    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        layoutType = layoutType,
        navigationSuiteItems = {
            EverfoldTab.entries.forEach { tab ->
                item(
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                        )
                    },
                    label = { Text(stringResource(tab.labelRes)) },
                    selected = currentRoute == tab.route,
                    onClick = {
                        if (currentRoute != tab.route) {
                            backStack.clear()
                            backStack.add(tab.route)
                        }
                    },
                )
            }
        },
    ) {
        AppNavDisplay(backStack = backStack)
    }
}

@Composable
private fun AppNavDisplay(backStack: NavBackStack<NavKey>) {
    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryProvider =
            entryProvider {
                entry<AppDestination.TopLevel.Sessions> {
                    SessionsScreen(
                        onOpenSession = { id ->
                            backStack.add(AppDestination.SessionDetail(sessionId = id))
                        },
                        onNewSession = {
                            backStack.add(AppDestination.SessionDetail(sessionId = null))
                        },
                    )
                }
                entry<AppDestination.SessionDetail> { destination ->
                    SessionDetailScreen(
                        sessionId = destination.sessionId,
                        onBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        },
                        onSessionCreate = { createdId ->
                            if (backStack.isNotEmpty()) {
                                backStack[backStack.lastIndex] =
                                    AppDestination.SessionDetail(sessionId = createdId)
                            }
                        },
                        onNewSession = {
                            if (backStack.isNotEmpty()) {
                                backStack[backStack.lastIndex] =
                                    AppDestination.SessionDetail(sessionId = null)
                            }
                        },
                    )
                }
                entry<AppDestination.TopLevel.Nodes> { NodesScreen() }
                entry<AppDestination.TopLevel.Models> { ModelsScreen() }
                entry<AppDestination.TopLevel.Settings> { SettingsScreen() }
            },
    )
}
