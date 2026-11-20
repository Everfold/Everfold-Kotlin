package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.everfold.kotlin.feature.connections.Connection
import com.everfold.kotlin.feature.connections.label
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.SelectionChoice
import com.everfold.kotlin.ui.components.SelectionMenu
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_connection
import everfold.shared.generated.resources.choose_workspace
import everfold.shared.generated.resources.connection_back
import everfold.shared.generated.resources.connection_manage
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.conversation_menu
import everfold.shared.generated.resources.longtask_inject_io_fault
import everfold.shared.generated.resources.longtask_inject_throttle
import everfold.shared.generated.resources.menu_connection
import everfold.shared.generated.resources.menu_workspace
import everfold.shared.generated.resources.preview_agent_states
import everfold.shared.generated.resources.workspace_unavailable

private enum class MenuPage { Overview, Connections, Workspaces, Preview }

private const val FAULT_THROTTLE_ID = "fault:throttle"
private const val FAULT_IO_ID = "fault:io"

/** 标题栏共用一个菜单，详情与选择在同一个面板内切换。 */
@Composable
internal fun ConversationMenu(
    state: ConversationUiState,
    connections: List<Connection>,
    options: ConversationOptions,
    canSelect: Boolean,
    onSelectConnection: (String) -> Unit,
    onOpenConnection: () -> Unit,
    onPreviewState: ((AgentRunState) -> Unit)? = null,
    onInjectFault: ((DeviceTaskFault) -> Unit)? = null,
) {
    var page by remember(state.id) { mutableStateOf<MenuPage?>(null) }
    val currentPage = page ?: MenuPage.Overview
    val menuLabel = localizedString(Res.string.conversation_menu)
    val canInjectFault =
        onInjectFault != null &&
            state.messages
                .lastOrNull()
                ?.streamBlocks
                ?.isNotEmpty() == true
    Box {
        AppTooltip(menuLabel) {
            IconButton(onClick = { page = MenuPage.Overview }) { AppIcon(AppSymbol.More, description = menuLabel) }
        }
        SelectionMenu(
            title = localizedString(currentPage.title),
            choices = menuChoices(currentPage, state, connections, options, canSelect, onPreviewState != null, canInjectFault),
            expanded = page != null,
            onDismiss = { page = null },
        ) { id ->
            if (id == "back") {
                page = MenuPage.Overview
            } else if (currentPage == MenuPage.Overview) {
                page = resolveOverviewTarget(id, canSelect, onOpenConnection)
            } else {
                page = null
                dispatchMenuSelection(
                    currentPage,
                    id,
                    state,
                    options,
                    canSelect,
                    onSelectConnection,
                    onOpenConnection,
                    onPreviewState,
                    onInjectFault,
                )
            }
        }
    }
}

private fun resolveOverviewTarget(
    id: String,
    canSelect: Boolean,
    onOpenConnection: () -> Unit,
): MenuPage? =
    when (id) {
        "connection" -> {
            if (canSelect) {
                MenuPage.Connections
            } else {
                onOpenConnection()
                null
            }
        }

        "workspace" -> {
            MenuPage.Workspaces
        }

        "preview" -> {
            MenuPage.Preview
        }

        else -> {
            null
        }
    }

private fun dispatchMenuSelection(
    currentPage: MenuPage,
    id: String,
    state: ConversationUiState,
    options: ConversationOptions,
    canSelect: Boolean,
    onSelectConnection: (String) -> Unit,
    onOpenConnection: () -> Unit,
    onPreviewState: ((AgentRunState) -> Unit)?,
    onInjectFault: ((DeviceTaskFault) -> Unit)?,
) {
    if (currentPage == MenuPage.Connections) {
        if (id == "manage") onOpenConnection() else onSelectConnection(id)
    } else if (currentPage == MenuPage.Workspaces) {
        if (canSelect) options.selectWorkspace(state.id, id)
    } else if (currentPage == MenuPage.Preview) {
        when (id) {
            FAULT_THROTTLE_ID -> onInjectFault?.invoke(DeviceTaskFault.PowerSaverThrottle)
            FAULT_IO_ID -> onInjectFault?.invoke(DeviceTaskFault.StorageIoInterrupt)
            else -> onPreviewState?.invoke(AgentRunState.valueOf(id))
        }
    }
}

private val MenuPage.title
    get() =
        when (this) {
            MenuPage.Overview -> Res.string.conversation_menu
            MenuPage.Connections -> Res.string.choose_connection
            MenuPage.Workspaces -> Res.string.choose_workspace
            MenuPage.Preview -> Res.string.preview_agent_states
        }

@Composable
private fun menuChoices(
    page: MenuPage,
    state: ConversationUiState,
    connections: List<Connection>,
    options: ConversationOptions,
    canSelect: Boolean,
    preview: Boolean,
    canInjectFault: Boolean,
): List<SelectionChoice> {
    val choices =
        when (page) {
            MenuPage.Overview -> {
                overviewChoices(state, connections.find { it.id == state.connectionId }, canSelect, preview)
            }

            MenuPage.Connections -> {
                connections.map {
                    SelectionChoice(
                        it.id,
                        "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}",
                        selected = it.id == state.connectionId,
                    )
                } + SelectionChoice("manage", localizedString(Res.string.connection_manage), searchable = false)
            }

            MenuPage.Workspaces -> {
                options.workspaces(state.connectionId).map {
                    SelectionChoice(it.id, it.name.resolve(), it.path, it.id == state.workspace?.id)
                }
            }

            MenuPage.Preview -> {
                val baseChoices =
                    AgentRunState.entries.map {
                        SelectionChoice(it.name, localizedString(it.label), selected = it == state.runState)
                    }
                if (canInjectFault) {
                    baseChoices +
                        listOf(
                            SelectionChoice(FAULT_THROTTLE_ID, localizedString(Res.string.longtask_inject_throttle), searchable = false),
                            SelectionChoice(FAULT_IO_ID, localizedString(Res.string.longtask_inject_io_fault), searchable = false),
                        )
                } else {
                    baseChoices
                }
            }
        }
    return if (page ==
        MenuPage.Overview
    ) {
        choices
    } else {
        listOf(SelectionChoice("back", localizedString(Res.string.connection_back), searchable = false)) + choices
    }
}

@Composable
private fun overviewChoices(
    state: ConversationUiState,
    connection: Connection?,
    canSelect: Boolean,
    preview: Boolean,
): List<SelectionChoice> =
    buildList {
        add(
            SelectionChoice(
                "connection",
                localizedString(Res.string.menu_connection),
                connection?.let { "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}" }
                    ?: localizedString(Res.string.connection_removed),
            ),
        )
        add(
            SelectionChoice(
                "workspace",
                localizedString(Res.string.menu_workspace),
                state.workspace?.let { "${it.name.resolve()}\n${it.path}" } ?: localizedString(Res.string.workspace_unavailable),
                readOnly = !canSelect || state.workspace == null,
            ),
        )
        if (preview) add(SelectionChoice("preview", localizedString(Res.string.preview_agent_states)))
    }
