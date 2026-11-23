package com.everfold.feature.sessions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.ui.ActionTraceGroupCard
import com.everfold.core.ui.MarkdownMessageContent

private const val LONG_MESSAGE_CHAR_LIMIT = 240
private const val COLLAPSED_PREVIEW_CHARS = 160

@Composable
internal fun SessionDetailPane(
    detail: AgentSessionDetail,
    draft: String,
    chatTextSize: ChatTextSize,
    autoCollapseLongMessages: Boolean,
    showNodeBadgesInline: Boolean,
    onBack: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning =
        detail.summary.loopState == LoopState.PLANNING ||
            detail.summary.loopState == LoopState.EXECUTING_ACTION

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = { SessionDetailTopBar(detail = detail, onBack = onBack) },
        bottomBar = {
            SessionComposerBar(
                draft = draft,
                isRunning = isRunning,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onCancel = onCancel,
            )
        },
    ) { innerPadding ->
        if (detail.items.isEmpty()) {
            EverfoldEmptyState(
                title = "开始一次新的 Agent Loop",
                subtitle = "输入任意指令，Authority 将自动调度最合适的节点执行动作。",
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            val currentDensity = LocalDensity.current
            val scaledDensity =
                Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * chatTextSize.scale,
                )
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(detail.items, key = { it.id }) { item ->
                        ObservedItemRow(
                            item = item,
                            autoCollapseLongMessages = autoCollapseLongMessages,
                            showNodeBadgesInline = showNodeBadgesInline,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailTopBar(
    detail: AgentSessionDetail,
    onBack: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
        },
        title = {
            Column {
                Text(
                    text = detail.summary.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Loop · ${detail.summary.loopHostNodeName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            EverfoldStatusPill(
                text = detail.summary.loopState.toLabel(),
                modifier = Modifier.padding(end = 12.dp),
            )
        },
    )
}

@Composable
private fun ObservedItemRow(
    item: ObservedItem,
    autoCollapseLongMessages: Boolean,
    showNodeBadgesInline: Boolean,
) {
    when (item) {
        is ObservedItem.UserMessage -> {
            UserMessageBubble(
                content = item.content,
                autoCollapseLongMessages = autoCollapseLongMessages,
            )
        }

        is ObservedItem.ActionGroup -> {
            ActionTraceGroupCard(
                actions = item.actions,
                showNodeBadgesInline = showNodeBadgesInline,
            )
        }

        is ObservedItem.AssistantMessage -> {
            AssistantMessageCard(
                item = item,
                autoCollapseLongMessages = autoCollapseLongMessages,
            )
        }
    }
}

@Composable
private fun UserMessageBubble(
    content: String,
    autoCollapseLongMessages: Boolean,
) {
    val isLong = autoCollapseLongMessages && content.length > LONG_MESSAGE_CHAR_LIMIT
    var expanded by rememberSaveable(content, autoCollapseLongMessages) { mutableStateOf(!isLong) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = if (expanded) content else "${content.take(COLLAPSED_PREVIEW_CHARS)}…",
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (isLong) {
                    TextButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(if (expanded) "收起" else "展开全文")
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageCard(
    item: ObservedItem.AssistantMessage,
    autoCollapseLongMessages: Boolean,
) {
    val isLong =
        autoCollapseLongMessages &&
            !item.isStreaming &&
            item.markdown.length > LONG_MESSAGE_CHAR_LIMIT
    var expanded by rememberSaveable(item.id, autoCollapseLongMessages) { mutableStateOf(!isLong) }
    val rendered =
        if (expanded || item.isStreaming) {
            item.markdown
        } else {
            "${item.markdown.take(COLLAPSED_PREVIEW_CHARS)}…"
        }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MarkdownMessageContent(content = rendered)
            if (isLong) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (expanded) "收起" else "展开全文")
                }
            }
        }
    }
}

@Composable
private fun SessionComposerBar(
    draft: String,
    isRunning: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text("向智能体发送指令…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            if (isRunning) {
                FilledIconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.StopCircle, contentDescription = "停止执行")
                }
            } else {
                FilledIconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "发送")
                }
            }
        }
    }
}
