package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.jump_to_latest
import everfold.shared.generated.resources.model_unavailable
import kotlinx.coroutines.launch

@Immutable
private data class MessageDisplayConfig(
    val textSize: ConversationTextSize,
    val collapsePrompts: Boolean,
    val sideMargins: Boolean,
    val canRetry: Boolean,
)

/** 倒序消息列表按用户意愿跟随最新回复并保留长消息折叠时的位置 */
@Composable
internal fun ConversationMessages(
    state: ConversationUiState,
    scrollRequest: Int,
    connectionName: String,
    onJumpToLatest: () -> Unit,
    onRetry: () -> Unit,
    canRetry: Boolean,
    modifier: Modifier = Modifier,
    textSize: ConversationTextSize = ConversationTextSize.System,
    collapsePrompts: Boolean = true,
    sideMargins: Boolean = true,
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var followLatest by rememberSaveable { mutableStateOf(true) }
    val atLatest by remember { derivedStateOf { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 } }
    ConversationFollowEffects(
        list = list,
        state = state,
        scrollRequest = scrollRequest,
        followLatest = followLatest,
        onFollowChange = { followLatest = it },
    )
    val config = MessageDisplayConfig(textSize, collapsePrompts, sideMargins, canRetry)
    Box(modifier.fillMaxWidth()) {
        ConversationMessageList(
            list = list,
            state = state,
            connectionName = connectionName,
            config = config,
            onRetry = onRetry,
            onPromptHeightChange = { messageId, heightChange ->
                followLatest = false
                anchorCollapsedPrompt(list, messageId, heightChange)
            },
        )
        ConversationScrollbar(list, Modifier.align(Alignment.CenterEnd).padding(vertical = 8.dp))
        if (!atLatest) {
            FilledTonalButton(
                onClick = {
                    scope.launch {
                        list.scrollToItem(0)
                        followLatest = true
                        onJumpToLatest()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            ) {
                Text(localizedString(Res.string.jump_to_latest))
            }
        }
    }
}

@Composable
private fun ConversationFollowEffects(
    list: LazyListState,
    state: ConversationUiState,
    scrollRequest: Int,
    followLatest: Boolean,
    onFollowChange: (Boolean) -> Unit,
) {
    val currentOnFollowChange by rememberUpdatedState(onFollowChange)
    // 仅用户滚动改变跟随模式流式回复引起的布局变化不能强制读者回到最新消息
    LaunchedEffect(list) {
        snapshotFlow { if (list.isScrollInProgress) list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 else null }
            .collect { atBottom -> if (atBottom != null) currentOnFollowChange(atBottom) }
    }
    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0) {
            currentOnFollowChange(true)
            list.scrollToItem(0)
        }
    }
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.id, state.runState) {
        if (followLatest && !list.isScrollInProgress) list.scrollToItem(0)
    }
}

@Composable
private fun ConversationMessageList(
    list: LazyListState,
    state: ConversationUiState,
    connectionName: String,
    config: MessageDisplayConfig,
    onRetry: () -> Unit,
    onPromptHeightChange: (String, Int) -> Unit,
) {
    val latest = state.messages.lastOrNull()
    LazyColumn(
        state = list,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (latest?.role != MessageRole.Assistant && state.runState != AgentRunState.Idle) {
            item(key = "run-status") {
                MessageColumn(config.sideMargins) { AgentStatusBar(state.runState, onRetry, canRetry = config.canRetry) }
            }
        }
        items(state.messages.asReversed(), key = { it.id }) { message ->
            MessageColumn(config.sideMargins) {
                MessageItem(
                    message,
                    authorName = message.authorName(connectionName, state.model),
                    modifier = Modifier.fillMaxWidth(),
                    textSize = config.textSize,
                    collapsePrompts = config.collapsePrompts,
                    runState = if (message.id == latest?.id) state.runState else AgentRunState.Idle,
                    onRetry = onRetry,
                    canRetry = config.canRetry,
                    onPromptHeightChange = { heightChange -> onPromptHeightChange(message.id, heightChange) },
                )
            }
        }
    }
}

private fun anchorCollapsedPrompt(
    list: LazyListState,
    messageId: String,
    heightChange: Int,
) {
    val info = list.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == messageId } ?: return
    // 倒序列表从底部定位折叠时保留消息顶部或将其带回可视区域
    val top = (info.viewportEndOffset - item.offset - item.size).coerceAtLeast(info.beforeContentPadding)
    list.requestScrollToItem(item.index, item.size + heightChange - info.viewportEndOffset + top)
}

/** 与输入区共用最大宽度，滚动条仍保留在窗口边缘。 */
private val ConversationContentMaxWidth = 840.dp

internal fun Modifier.conversationContentWidth(sideMargins: Boolean): Modifier =
    (if (sideMargins) widthIn(max = ConversationContentMaxWidth) else this).fillMaxWidth()

@Composable
private fun MessageColumn(
    sideMargins: Boolean,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.conversationContentWidth(sideMargins).padding(horizontal = 24.dp)) {
            content()
        }
    }
}

@Composable
private fun ChatMessage.authorName(
    connectionName: String,
    fallbackModel: ConversationModel?,
): String =
    if (role == MessageRole.User) {
        (model ?: fallbackModel)?.name?.resolve() ?: localizedString(Res.string.model_unavailable)
    } else {
        connectionName
    }

@Composable
internal expect fun ConversationScrollbar(
    state: LazyListState,
    modifier: Modifier = Modifier,
)
