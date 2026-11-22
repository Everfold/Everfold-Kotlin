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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.ui.ActionTraceGroupCard
import com.everfold.core.ui.MarkdownMessageContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionDetailPane(
    detail: AgentSessionDetail,
    draft: String,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(detail.items, key = { it.id }) { item ->
                    ObservedItemRow(
                        item = item,
                        showNodeBadgesInline = showNodeBadgesInline,
                    )
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
    showNodeBadgesInline: Boolean,
) {
    when (item) {
        is ObservedItem.UserMessage -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.widthIn(max = 320.dp),
                ) {
                    Text(
                        text = item.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }

        is ObservedItem.ActionGroup -> {
            ActionTraceGroupCard(
                actions = item.actions,
                showNodeBadgesInline = showNodeBadgesInline,
            )
        }

        is ObservedItem.AssistantMessage -> {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MarkdownMessageContent(
                    content = item.markdown,
                    modifier = Modifier.padding(16.dp),
                )
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
