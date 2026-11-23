package com.everfold.feature.sessions

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.LoopState
import com.everfold.feature.sessions.component.ObservedItemRow
import com.everfold.feature.sessions.component.SessionComposerBar
import org.koin.androidx.compose.koinViewModel

@Composable
fun SessionDetailScreen(
    sessionId: String?,
    onOpenDrawer: () -> Unit,
    onSessionCreate: (String) -> Unit,
    onNewSession: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(sessionId) {
        viewModel.bindSession(sessionId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = state.detail
    val isRunning =
        detail?.summary?.loopState == LoopState.PLANNING ||
            detail?.summary?.loopState == LoopState.EXECUTING_ACTION

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            SessionDetailTopBar(
                detail = detail,
                defaultModelName = state.defaultModelName,
                onOpenDrawer = onOpenDrawer,
                onNewSession = onNewSession,
            )
        },
        bottomBar = {
            SessionComposerBar(
                draft = state.draft,
                isRunning = isRunning,
                onDraftChange = viewModel::updateDraft,
                onSend = { viewModel.sendDraft(onSessionCreate) },
                onCancel = viewModel::cancelActiveLoop,
            )
        },
    ) { innerPadding ->
        SessionDetailBody(
            state = state,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}

@Composable
private fun SessionDetailBody(
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val detail = state.detail
    if (detail == null || detail.items.isEmpty()) {
        EverfoldEmptyState(
            title =
                stringResource(
                    if (detail == null) {
                        R.string.session_detail_empty_new_title
                    } else {
                        R.string.session_detail_empty_existing_title
                    },
                ),
            subtitle = stringResource(R.string.session_detail_empty_subtitle),
            modifier = modifier,
        )
        return
    }

    val currentDensity = LocalDensity.current
    val scaledDensity =
        Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale * state.chatTextSize.scale,
        )
    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(detail.items, key = { it.id }) { item ->
                ObservedItemRow(
                    item = item,
                    autoCollapseLongMessages = state.autoCollapseLongMessages,
                    expandRunningActionTraces = state.expandRunningActionTraces,
                    expandCompletedActionTraces = state.expandCompletedActionTraces,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionDetailTopBar(
    detail: AgentSessionDetail?,
    defaultModelName: String,
    onOpenDrawer: () -> Unit,
    onNewSession: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(R.string.session_detail_open_menu),
                )
            }
        },
        title = {
            Column {
                Text(
                    text = detail?.summary?.title ?: stringResource(R.string.session_detail_new_title),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                )
                val subtitle =
                    if (detail != null) {
                        stringResource(R.string.session_detail_host_label, detail.summary.loopHostNodeName)
                    } else {
                        defaultModelName.ifBlank { stringResource(R.string.session_detail_new_subtitle) }
                    }
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        actions = {
            if (detail != null) {
                EverfoldStatusPill(text = stringResource(detail.summary.loopState.labelRes()))
                IconButton(onClick = onNewSession) {
                    Icon(
                        imageVector = Icons.Rounded.AddComment,
                        contentDescription = stringResource(R.string.session_detail_new_action),
                    )
                }
            }
        },
    )
}

@StringRes
private fun LoopState.labelRes(): Int =
    when (this) {
        LoopState.IDLE -> R.string.loop_state_idle
        LoopState.PLANNING -> R.string.loop_state_planning
        LoopState.EXECUTING_ACTION -> R.string.loop_state_executing
        LoopState.AWAITING_INPUT -> R.string.loop_state_awaiting_input
        LoopState.COMPLETED -> R.string.loop_state_completed
        LoopState.FAILED -> R.string.loop_state_failed
    }
