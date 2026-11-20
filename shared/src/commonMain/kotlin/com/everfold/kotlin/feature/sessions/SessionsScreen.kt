package com.everfold.kotlin.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.label
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.clear_search
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.delete_session
import everfold.shared.generated.resources.delete_session_notice
import everfold.shared.generated.resources.empty_sessions
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.no_sessions
import everfold.shared.generated.resources.pin_session
import everfold.shared.generated.resources.pinned_session
import everfold.shared.generated.resources.rename_session
import everfold.shared.generated.resources.save_session
import everfold.shared.generated.resources.search_sessions
import everfold.shared.generated.resources.session_actions
import everfold.shared.generated.resources.session_name
import everfold.shared.generated.resources.unpin_session

private val HiddenRunStates = setOf(AgentRunState.Idle, AgentRunState.Completed)
private val MenuButtonSize = 32.dp
private val MenuIconSize = 20.dp

private class SessionItemCallbacks(
    val onSelect: (String) -> Unit,
    val onMenuOpen: (String) -> Unit,
    val onMenuDismiss: () -> Unit,
    val onRequestRename: (String, String) -> Unit,
    val onTogglePin: (String) -> Unit,
    val onRequestDelete: (String) -> Unit,
)

/** 会话侧栏常驻标题搜索两行列表项以及重命名置顶和删除操作 */
@Composable
fun SessionsScreen(
    sessions: List<ConversationUiState>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchRequest: Int = 0,
    connectionNames: Map<String, UiText> = emptyMap(),
) {
    val searchFocus = remember { FocusRequester() }
    SessionSearchFocusEffect(searchRequest, searchFocus)
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var menuId by remember { mutableStateOf<String?>(null) }
    val titles = sessions.map { it.title.resolve() }
    val names = connectionNames.mapValues { it.value.resolve() }
    // 同时按标题和连接名称搜索
    val filtered =
        remember(sessions, titles, names, query) {
            sessions.filterIndexed { index, session ->
                titles[index].contains(query, ignoreCase = true) ||
                    names[session.connectionId]?.contains(query, ignoreCase = true) == true
            }
        }
    val callbacks =
        SessionItemCallbacks(
            onSelect = onSelect,
            onMenuOpen = { menuId = it },
            onMenuDismiss = { menuId = null },
            onRequestRename = { id, currentTitle ->
                name = currentTitle
                renaming = id
                menuId = null
            },
            onTogglePin = { id ->
                onTogglePin(id)
                menuId = null
            },
            onRequestDelete = { id ->
                deleting = id
                menuId = null
            },
        )
    Column(modifier.padding(horizontal = 16.dp)) {
        SessionSearchField(query, onQueryChange, searchFocus)
        SessionListContent(filtered, selectedId, menuId, names, query, onQueryChange, onCreate, callbacks, Modifier.weight(1f))
    }
    SessionDialogs(
        renamingId = renaming,
        deletingId = deleting,
        name = name,
        onNameChange = { name = it },
        onDismissRename = { renaming = null },
        onConfirmRename = { id ->
            onRename(id, name)
            renaming = null
        },
        onDismissDelete = { deleting = null },
        onConfirmDelete = { id ->
            onDelete(id)
            deleting = null
        },
    )
}

@Composable
private fun SessionSearchFocusEffect(
    searchRequest: Int,
    searchFocus: FocusRequester,
) {
    var handledSearchRequest by rememberSaveable { mutableIntStateOf(0) }
    // 等布局完成后响应新的搜索焦点请求避免切换布局时重复抢焦点
    LaunchedEffect(searchRequest) {
        if (searchRequest > handledSearchRequest) {
            withFrameNanos { }
            searchFocus.requestFocus()
            handledSearchRequest = searchRequest
        }
    }
}

@Composable
private fun SessionDialogs(
    renamingId: String?,
    deletingId: String?,
    name: String,
    onNameChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmRename: (String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    renamingId?.let { id ->
        RenameSessionDialog(name, onNameChange, onDismissRename) { onConfirmRename(id) }
    }
    deletingId?.let { id ->
        DeleteSessionDialog(onDismissDelete) { onConfirmDelete(id) }
    }
}

@Composable
private fun SessionSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(localizedString(Res.string.search_sessions)) },
        leadingIcon = { AppIcon(AppSymbol.Search) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text(localizedString(Res.string.clear_search))
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).focusRequester(focusRequester),
    )
}

@Composable
private fun SessionListContent(
    filtered: List<ConversationUiState>,
    selectedId: String?,
    menuId: String?,
    names: Map<String, String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onCreate: () -> Unit,
    callbacks: SessionItemCallbacks,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier, verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
        items(filtered, key = { it.id }) { session ->
            SessionItemCard(
                session = session,
                selected = session.id == selectedId,
                menuExpanded = menuId == session.id,
                connectionName = names[session.connectionId],
                callbacks = callbacks,
            )
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    localizedString(if (query.isNotBlank()) Res.string.no_sessions else Res.string.empty_sessions),
                    Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { if (query.isBlank()) onCreate() else onQueryChange("") }) {
                    Text(localizedString(if (query.isBlank()) Res.string.new_session else Res.string.clear_search))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun SessionItemCard(
    session: ConversationUiState,
    selected: Boolean,
    menuExpanded: Boolean,
    connectionName: String?,
    callbacks: SessionItemCallbacks,
) {
    Surface(
        modifier =
            Modifier
                .combinedClickable(onClick = { callbacks.onSelect(session.id) }, onLongClick = { callbacks.onMenuOpen(session.id) })
                .pointerInput(session.id) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                callbacks.onMenuOpen(session.id)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val textEdgeInset = (MenuButtonSize - MenuIconSize) / 2
        val title = session.title.resolve()
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    Modifier.weight(1f).padding(start = textEdgeInset),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SessionActionMenu(session, title, menuExpanded, callbacks)
            }
            SessionItemMetadataRow(session, connectionName, Modifier.padding(horizontal = textEdgeInset))
        }
    }
}

@Composable
private fun SessionActionMenu(
    session: ConversationUiState,
    title: String,
    expanded: Boolean,
    callbacks: SessionItemCallbacks,
) {
    Box {
        // 只预留紧凑视觉空间Compose 可在布局外扩展触摸目标
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides MenuButtonSize) {
            IconButton(
                onClick = { callbacks.onMenuOpen(session.id) },
                modifier = Modifier.size(MenuButtonSize),
            ) {
                AppIcon(
                    AppSymbol.More,
                    modifier = Modifier.size(MenuIconSize),
                    description = localizedString(Res.string.session_actions),
                )
            }
        }
        DropdownMenu(expanded, onDismissRequest = callbacks.onMenuDismiss) {
            DropdownMenuItem(
                text = { Text(localizedString(Res.string.rename_session)) },
                onClick = { callbacks.onRequestRename(session.id, title) },
            )
            DropdownMenuItem(
                text = { Text(localizedString(if (session.isPinned) Res.string.unpin_session else Res.string.pin_session)) },
                onClick = { callbacks.onTogglePin(session.id) },
            )
            DropdownMenuItem(
                text = { Text(localizedString(Res.string.delete_session), color = MaterialTheme.colorScheme.error) },
                onClick = { callbacks.onRequestDelete(session.id) },
            )
        }
    }
}

@Composable
private fun SessionItemMetadataRow(
    session: ConversationUiState,
    connectionName: String?,
    modifier: Modifier = Modifier,
) {
    val status = localizedString(session.runState.label)
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            listOfNotNull(
                if (session.isPinned) localizedString(Res.string.pinned_session) else null,
                connectionName ?: localizedString(Res.string.connection_removed),
                status.takeIf { session.runState !in HiddenRunStates },
            ).joinToString(" · "),
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color =
                when (session.runState) {
                    AgentRunState.Failed -> MaterialTheme.colorScheme.error
                    AgentRunState.Running -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            session.updatedLabel.resolve(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RenameSessionDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedString(Res.string.rename_session)) },
        text = {
            OutlinedTextField(
                name,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(localizedString(Res.string.session_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text(localizedString(Res.string.save_session))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localizedString(Res.string.cancel)) } },
    )
}

@Composable
private fun DeleteSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedString(Res.string.delete_session)) },
        text = { Text(localizedString(Res.string.delete_session_notice)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(localizedString(Res.string.delete_session), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localizedString(Res.string.cancel)) } },
    )
}
