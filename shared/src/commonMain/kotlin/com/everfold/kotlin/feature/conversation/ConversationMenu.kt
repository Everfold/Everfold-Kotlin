package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.connections.Connection
import com.everfold.kotlin.feature.connections.label
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_connection
import everfold.shared.generated.resources.choose_workspace
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.connection_manage
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.conversation_menu
import everfold.shared.generated.resources.menu_connection
import everfold.shared.generated.resources.menu_workspace
import everfold.shared.generated.resources.preview_agent_states
import everfold.shared.generated.resources.workspace_unavailable
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class MenuPage { Overview, Connections, Workspaces, Preview }

private val MenuPage.title
    get() =
        when (this) {
            MenuPage.Overview -> Res.string.conversation_menu
            MenuPage.Connections -> Res.string.choose_connection
            MenuPage.Workspaces -> Res.string.choose_workspace
            MenuPage.Preview -> Res.string.preview_agent_states
        }

@Immutable
private class MenuSheetCallbacks(
    val onNavigate: (MenuPage?) -> Unit,
    val onSelectConnection: (String) -> Unit,
    val onOpenConnection: () -> Unit,
    val onSelectWorkspace: (String) -> Unit,
    val onPreviewState: ((AgentRunState) -> Unit)?,
)

/** 标题栏会话菜单：使用原生 Miuix GlassBottomSheet 展示总览及分级选择。 */
@Composable
internal fun ConversationMenu(
    state: ConversationUiState,
    connections: List<Connection>,
    options: ConversationOptions,
    canSelect: Boolean,
    onSelectConnection: (String) -> Unit,
    onOpenConnection: () -> Unit,
    onPreviewState: ((AgentRunState) -> Unit)? = null,
) {
    var page by remember(state.id) { mutableStateOf<MenuPage?>(null) }
    var displayedPage by remember(state.id) { mutableStateOf(MenuPage.Overview) }
    val navigateTo: (MenuPage?) -> Unit = { target ->
        if (target != null) displayedPage = target
        page = target
    }
    val callbacks =
        remember(state.id, options, onSelectConnection, onOpenConnection, onPreviewState) {
            MenuSheetCallbacks(
                onNavigate = navigateTo,
                onSelectConnection = onSelectConnection,
                onOpenConnection = onOpenConnection,
                onSelectWorkspace = { workspaceId -> options.selectWorkspace(state.id, workspaceId) },
                onPreviewState = onPreviewState,
            )
        }
    val menuLabel = localizedString(Res.string.conversation_menu)
    AppTooltip(menuLabel) {
        GlassIconButton(onClick = { navigateTo(MenuPage.Overview) }) {
            AppIcon(
                AppSymbol.More,
                modifier = Modifier.size(28.dp),
                description = menuLabel,
                tint = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
    ConversationMenuSheet(
        state = state,
        connections = connections,
        options = options,
        canSelect = canSelect,
        show = page != null,
        displayedPage = displayedPage,
        callbacks = callbacks,
    )
}

@Composable
private fun ConversationMenuSheet(
    state: ConversationUiState,
    connections: List<Connection>,
    options: ConversationOptions,
    canSelect: Boolean,
    show: Boolean,
    displayedPage: MenuPage,
    callbacks: MenuSheetCallbacks,
) {
    val backLabel = localizedString(Res.string.connection_back)
    GlassBottomSheet(
        show = show,
        onDismissRequest = { callbacks.onNavigate(null) },
        backdrop = LocalAppBackdrop.current,
        title = localizedString(displayedPage.title),
        startAction =
            if (displayedPage != MenuPage.Overview) {
                {
                    IconButton(onClick = { callbacks.onNavigate(MenuPage.Overview) }) {
                        AppIcon(AppSymbol.Back, modifier = Modifier.size(22.dp), description = backLabel)
                    }
                }
            } else {
                null
            },
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).verticalScroll(rememberScrollState())) {
            when (displayedPage) {
                MenuPage.Overview -> {
                    MenuOverviewPage(state, connections.find { it.id == state.connectionId }, canSelect, callbacks)
                }

                MenuPage.Connections -> {
                    MenuConnectionsPage(connections, state.connectionId, callbacks)
                }

                MenuPage.Workspaces -> {
                    MenuWorkspacesPage(options.workspaces(state.connectionId), state.workspace?.id, canSelect, callbacks)
                }

                MenuPage.Preview -> {
                    MenuPreviewPage(state.runState, callbacks)
                }
            }
        }
    }
}

@Composable
private fun MenuOverviewPage(
    state: ConversationUiState,
    connection: Connection?,
    canSelect: Boolean,
    callbacks: MenuSheetCallbacks,
) {
    val connectionSummary =
        connection?.let { "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}" }
            ?: localizedString(Res.string.connection_removed)
    val workspaceSummary =
        state.workspace?.let { "${it.name.resolve()}\n${it.path}" }
            ?: localizedString(Res.string.workspace_unavailable)
    Card(modifier = Modifier.fillMaxWidth()) {
        ArrowPreference(
            title = localizedString(Res.string.menu_connection),
            summary = connectionSummary,
            onClick = {
                if (canSelect) {
                    callbacks.onNavigate(MenuPage.Connections)
                } else {
                    callbacks.onNavigate(null)
                    callbacks.onOpenConnection()
                }
            },
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        if (canSelect && state.workspace != null) {
            ArrowPreference(
                title = localizedString(Res.string.menu_workspace),
                summary = workspaceSummary,
                onClick = { callbacks.onNavigate(MenuPage.Workspaces) },
            )
        } else {
            BasicComponent(title = localizedString(Res.string.menu_workspace), summary = workspaceSummary)
        }
        if (callbacks.onPreviewState != null) {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ArrowPreference(
                title = localizedString(Res.string.preview_agent_states),
                onClick = { callbacks.onNavigate(MenuPage.Preview) },
            )
        }
    }
}

@Composable
private fun MenuConnectionsPage(
    connections: List<Connection>,
    selectedId: String,
    callbacks: MenuSheetCallbacks,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        connections.forEachIndexed { index, connection ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            RadioButtonPreference(
                title = connection.profile.name.resolve(),
                summary = localizedString(connection.runtime.status.label),
                selected = connection.id == selectedId,
                onClick = {
                    callbacks.onSelectConnection(connection.id)
                    callbacks.onNavigate(null)
                },
            )
        }
        if (connections.isNotEmpty()) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ArrowPreference(
            title = localizedString(Res.string.connection_manage),
            onClick = {
                callbacks.onNavigate(null)
                callbacks.onOpenConnection()
            },
        )
    }
}

@Composable
private fun MenuWorkspacesPage(
    workspaces: List<SessionWorkspace>,
    selectedId: String?,
    canSelect: Boolean,
    callbacks: MenuSheetCallbacks,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        workspaces.forEachIndexed { index, workspace ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            RadioButtonPreference(
                title = workspace.name.resolve(),
                summary = workspace.path,
                selected = workspace.id == selectedId,
                onClick = {
                    if (canSelect) callbacks.onSelectWorkspace(workspace.id)
                    callbacks.onNavigate(null)
                },
            )
        }
    }
}

@Composable
private fun MenuPreviewPage(
    selected: AgentRunState,
    callbacks: MenuSheetCallbacks,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        AgentRunState.entries.forEachIndexed { index, runState ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            RadioButtonPreference(
                title = localizedString(runState.label),
                selected = runState == selected,
                onClick = {
                    callbacks.onPreviewState?.invoke(runState)
                    callbacks.onNavigate(null)
                },
            )
        }
    }
}
