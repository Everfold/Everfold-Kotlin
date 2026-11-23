package com.everfold.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    modifier: Modifier = Modifier,
    viewModel: SessionsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.activeDetail

    if (detail != null) {
        SessionDetailPane(
            detail = detail,
            draft = state.draft,
            chatTextSize = state.chatTextSize,
            autoCollapseLongMessages = state.autoCollapseLongMessages,
            showNodeBadgesInline = state.showNodeBadgesInline,
            onBack = viewModel::closeSession,
            onDraftChange = viewModel::updateDraft,
            onSend = viewModel::sendDraft,
            onCancel = viewModel::cancelActiveLoop,
            modifier = modifier,
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = { TopAppBar(title = { Text("会话") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = viewModel::createAndOpenSession) {
                    Icon(Icons.Rounded.Add, contentDescription = "新建智能体会话")
                }
            },
        ) { innerPadding ->
            if (state.sessions.isEmpty()) {
                EverfoldEmptyState(
                    title = "还没有活跃的智能体会话",
                    subtitle = "点击右下角新建按钮，向 Authority 发起一个新的 Agent Loop。",
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.sessions, key = { it.id }) { summary ->
                        SessionSummaryCard(
                            summary = summary,
                            onClick = { viewModel.openSession(summary.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSummaryCard(
    summary: AgentSessionSummary,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                EverfoldStatusPill(text = summary.loopState.toLabel())
            }
            Text(
                text = summary.lastMessagePreview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Loop · ${summary.loopHostNodeName} · ${summary.updatedAtLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun LoopState.toLabel(): String =
    when (this) {
        LoopState.IDLE -> "待命"
        LoopState.PLANNING -> "规划中"
        LoopState.EXECUTING_ACTION -> "执行动作"
        LoopState.AWAITING_INPUT -> "等待输入"
        LoopState.COMPLETED -> "已完成"
        LoopState.FAILED -> "异常"
    }
