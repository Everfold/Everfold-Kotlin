package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.jump_to_latest
import everfold.shared.generated.resources.model_unavailable
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 倒序消息列表：按用户意愿跟随最新回复并保留长消息折叠时的位置 */
@OptIn(ExperimentalScrollBarApi::class)
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
    topPadding: Dp = 0.dp,
) {
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var followLatest by rememberSaveable { mutableStateOf(true) }
    val atLatest by remember { derivedStateOf { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 } }
    ConversationScrollEffects(list, scrollRequest, state, followLatest) { followLatest = it }
    Box(modifier.fillMaxWidth()) {
        ConversationMessagesList(
            list = list,
            state = state,
            connectionName = connectionName,
            textSize = textSize,
            collapsePrompts = collapsePrompts,
            sideMargins = sideMargins,
            topPadding = topPadding,
            canRetry = canRetry,
            onRetry = onRetry,
            onPauseFollow = { followLatest = false },
        )
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(list),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = topPadding + 8.dp, bottom = 8.dp),
            reverseLayout = true,
        )
        if (!atLatest) {
            JumpToLatestButton(
                onClick = {
                    scope.launch {
                        list.scrollToItem(0)
                        followLatest = true
                        onJumpToLatest()
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

@Composable
private fun ConversationScrollEffects(
    list: LazyListState,
    scrollRequest: Int,
    state: ConversationUiState,
    followLatest: Boolean,
    onFollowLatestChange: (Boolean) -> Unit,
) {
    val latestOnFollowLatestChange by rememberUpdatedState(onFollowLatestChange)
    LaunchedEffect(list) {
        snapshotFlow { if (list.isScrollInProgress) list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 else null }
            .collect { atBottom -> if (atBottom != null) latestOnFollowLatestChange(atBottom) }
    }
    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0) {
            latestOnFollowLatestChange(true)
            list.scrollToItem(0)
        }
    }
    LaunchedEffect(state.messages.lastOrNull(), state.runState) {
        if (followLatest && !list.isScrollInProgress) list.scrollToItem(0)
    }
}

@Composable
private fun ConversationMessagesList(
    list: LazyListState,
    state: ConversationUiState,
    connectionName: String,
    textSize: ConversationTextSize,
    collapsePrompts: Boolean,
    sideMargins: Boolean,
    topPadding: Dp,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onPauseFollow: () -> Unit,
) {
    val latest = state.messages.lastOrNull()
    LazyColumn(
        state = list,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = topPadding + 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (latest?.role != MessageRole.Assistant && state.runState != AgentRunState.Idle) {
            item(key = "run-status") {
                MessageColumn(sideMargins) { AgentStatusBar(state.runState, onRetry, canRetry = canRetry) }
            }
        }
        items(state.messages.asReversed(), key = { it.id }) { message ->
            MessageColumn(sideMargins) {
                MessageItem(
                    message = message,
                    authorName = message.authorName(connectionName, state.model),
                    modifier = Modifier.fillMaxWidth(),
                    textSize = textSize,
                    collapsePrompts = collapsePrompts,
                    runState = if (message.id == latest?.id) state.runState else AgentRunState.Idle,
                    onRetry = onRetry,
                    canRetry = canRetry,
                    onPromptHeightChange = { preserveMessagePosition(list, message.id, it, onPauseFollow) },
                )
            }
        }
    }
}

private fun preserveMessagePosition(
    list: LazyListState,
    messageId: String,
    heightChange: Int,
    onDisableFollow: () -> Unit,
) {
    onDisableFollow()
    val info = list.layoutInfo
    val item = info.visibleItemsInfo.firstOrNull { it.key == messageId } ?: return
    val top = (info.viewportEndOffset - item.offset - item.size).coerceAtLeast(info.beforeContentPadding)
    list.requestScrollToItem(item.index, item.size + heightChange - info.viewportEndOffset + top)
}

@Composable
private fun JumpToLatestButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        cornerRadius = 20.dp,
        colors =
            ButtonDefaults.buttonColors(
                color = MiuixTheme.colorScheme.secondaryContainer,
                contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
            ),
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(text = localizedString(Res.string.jump_to_latest), style = MiuixTheme.textStyles.button)
    }
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
