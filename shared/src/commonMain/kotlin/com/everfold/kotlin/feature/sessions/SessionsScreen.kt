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

/** 会话侧栏常驻标题搜索两行列表项以及重命名置顶和删除操作 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
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
    var handledSearchRequest by rememberSaveable { mutableIntStateOf(0) }
    // 等布局完成后响应新的搜索焦点请求避免切换布局时重复抢焦点
    LaunchedEffect(searchRequest) {
        if (searchRequest > handledSearchRequest) {
            withFrameNanos { }
            searchFocus.requestFocus()
            handledSearchRequest = searchRequest
        }
    }
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
    Column(modifier.padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            // 搜索
            placeholder = { Text(localizedString(Res.string.search_sessions)) },
            leadingIcon = { AppIcon(AppSymbol.Search) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(
                        onClick = { onQueryChange("") },
                    ) {
                        // 清空搜索
                        Text(localizedString(Res.string.clear_search))
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).focusRequester(searchFocus),
        )
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(filtered, key = { it.id }) { session ->
                Surface(
                    modifier =
                        Modifier
                            .combinedClickable(onClick = { onSelect(session.id) }, onLongClick = { menuId = session.id })
                            .pointerInput(session.id) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                            menuId = session.id
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                }
                            },
                    shape = RoundedCornerShape(12.dp),
                    color =
                        if (session.id ==
                            selectedId
                        ) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                ) {
                    // 第一行标题与菜单第二行状态与时间文字边缘按图标内留白对称对齐
                    val menuButtonSize = 32.dp
                    val menuIconSize = 20.dp
                    val textEdgeInset = (menuButtonSize - menuIconSize) / 2
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val title = session.title.resolve()
                            Text(
                                title,
                                Modifier.weight(1f).padding(start = textEdgeInset),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Box {
                                // 只预留紧凑视觉空间Compose 可在布局外扩展触摸目标
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides menuButtonSize) {
                                    IconButton(
                                        onClick = { menuId = session.id },
                                        modifier = Modifier.size(menuButtonSize),
                                    ) {
                                        AppIcon(
                                            AppSymbol.More,
                                            modifier = Modifier.size(menuIconSize),
                                            // 会话操作
                                            description = localizedString(Res.string.session_actions),
                                        )
                                    }
                                }
                                DropdownMenu(menuId == session.id, onDismissRequest = { menuId = null }) {
                                    // 重命名
                                    DropdownMenuItem(text = { Text(localizedString(Res.string.rename_session)) }, onClick = {
                                        name = title
                                        renaming = session.id
                                        menuId = null
                                    })
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                // 取消置顶 / 置顶
                                                localizedString(if (session.isPinned) Res.string.unpin_session else Res.string.pin_session),
                                            )
                                        },
                                        onClick = {
                                            onTogglePin(session.id)
                                            menuId = null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                // 删除
                                                localizedString(Res.string.delete_session),
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        },
                                        onClick = {
                                            deleting = session.id
                                            menuId = null
                                        },
                                    )
                                }
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 状态见 AgentRunState.label
                            val status = localizedString(session.runState.label)
                            Text(
                                // 已置顶
                                listOfNotNull(
                                    if (session.isPinned) localizedString(Res.string.pinned_session) else null,
                                    names[session.connectionId] ?: localizedString(Res.string.connection_removed),
                                    status.takeIf { session.runState !in HiddenRunStates },
                                ).joinToString(" · "),
                                Modifier.weight(1f).padding(start = textEdgeInset),
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
                                Modifier.padding(end = textEdgeInset),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Text(
                        localizedString(
                            // 没有找到会话试试其他关键词 / 还没有会话要做点什么呢
                            if (query.isNotBlank()) Res.string.no_sessions else Res.string.empty_sessions,
                        ),
                        Modifier.padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { if (query.isBlank()) onCreate() else onQueryChange("") }) {
                        // 新建会话 / 清空搜索
                        Text(localizedString(if (query.isBlank()) Res.string.new_session else Res.string.clear_search))
                    }
                }
            }
        }
    }
    renaming?.let { id ->
        AlertDialog(
            onDismissRequest = { renaming = null },
            // 重命名
            title = { Text(localizedString(Res.string.rename_session)) },
            text = {
                OutlinedTextField(
                    name,
                    onValueChange = { name = it },
                    singleLine = true,
                    // 会话名称
                    label = { Text(localizedString(Res.string.session_name)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRename(id, name)
                    renaming = null
                }, enabled = name.isNotBlank()) {
                    // 保存
                    Text(localizedString(Res.string.save_session))
                }
            },
            // 取消
            dismissButton = { TextButton(onClick = { renaming = null }) { Text(localizedString(Res.string.cancel)) } },
        )
    }
    deleting?.let { id ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            // 删除
            title = { Text(localizedString(Res.string.delete_session)) },
            // 删除后本次运行中的会话内容将被移除
            text = { Text(localizedString(Res.string.delete_session_notice)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    deleting = null
                }) {
                    // 删除
                    Text(localizedString(Res.string.delete_session), color = MaterialTheme.colorScheme.error)
                }
            },
            // 取消
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(localizedString(Res.string.cancel)) } },
        )
    }
}
