package com.everfold.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.everfold.feature.models.ModelsScreen
import com.everfold.feature.nodes.NodesScreen
import com.everfold.feature.sessions.SessionsScreen
import com.everfold.feature.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
private sealed interface TopLevelDestination : NavKey {
    @Serializable
    data object Sessions : TopLevelDestination

    @Serializable
    data object Nodes : TopLevelDestination

    @Serializable
    data object Models : TopLevelDestination

    @Serializable
    data object Settings : TopLevelDestination
}

private enum class EverfoldTab(
    val route: TopLevelDestination,
    val label: String,
    val icon: ImageVector,
) {
    SESSIONS(TopLevelDestination.Sessions, "会话", Icons.Rounded.ChatBubbleOutline),
    NODES(TopLevelDestination.Nodes, "节点", Icons.Rounded.Hub),
    MODELS(TopLevelDestination.Models, "模型", Icons.Rounded.Memory),
    SETTINGS(TopLevelDestination.Settings, "设置", Icons.Rounded.Tune),
}

@Composable
internal fun EverfoldAppShell(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(TopLevelDestination.Sessions)
    val currentRoute = backStack.lastOrNull() as? TopLevelDestination ?: TopLevelDestination.Sessions

    NavigationSuiteScaffold(
        modifier = modifier.fillMaxSize(),
        navigationSuiteItems = {
            EverfoldTab.entries.forEach { tab ->
                item(
                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
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
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeAt(backStack.lastIndex)
                }
            },
            entryProvider =
                entryProvider {
                    entry<TopLevelDestination.Sessions> { SessionsScreen() }
                    entry<TopLevelDestination.Nodes> { NodesScreen() }
                    entry<TopLevelDestination.Models> { ModelsScreen() }
                    entry<TopLevelDestination.Settings> { SettingsScreen() }
                },
        )
    }
}
