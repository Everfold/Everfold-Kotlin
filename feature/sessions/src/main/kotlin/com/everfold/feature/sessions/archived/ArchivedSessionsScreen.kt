package com.everfold.feature.sessions.archived

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldEmptyState
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionSummary
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.component.DeleteSessionDialog
import com.everfold.feature.sessions.labelRes
import org.koin.androidx.compose.koinViewModel
import com.everfold.core.ui.R as CoreUiR

private const val MAX_PREVIEW_LINES = 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArchivedSessionsScreen(
    onBack: () -> Unit,
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedSessionsViewModel = koinViewModel(),
) {
    val sessions by viewModel.archivedSessions.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<AgentSessionSummary?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.common_back),
                        )
                    }
                },
                title = { Text(stringResource(R.string.archived_sessions_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        ArchivedSessionsContent(
            sessions = sessions,
            onSelectSession = onSelectSession,
            onRestoreSession = viewModel::restoreSession,
            onRequestDelete = { pendingDelete = it },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }

    pendingDelete?.let { target ->
        DeleteSessionDialog(
            summary = target,
            onDismiss = { pendingDelete = null },
            onConfirm = {
                pendingDelete = null
                viewModel.deleteSession(target.id)
            },
        )
    }
}

@Composable
private fun ArchivedSessionsContent(
    sessions: List<AgentSessionSummary>,
    onSelectSession: (String) -> Unit,
    onRestoreSession: (String) -> Unit,
    onRequestDelete: (AgentSessionSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sessions.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            EverfoldEmptyState(
                title = stringResource(R.string.archived_sessions_empty_title),
                subtitle = stringResource(R.string.archived_sessions_empty_subtitle),
                icon = Icons.Rounded.Inventory2,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(sessions, key = { _, summary -> summary.id }) { index, summary ->
                ArchivedSessionCard(
                    index = index,
                    count = sessions.size,
                    summary = summary,
                    onOpen = { onSelectSession(summary.id) },
                    onRestore = { onRestoreSession(summary.id) },
                    onRequestDelete = { onRequestDelete(summary) },
                )
            }
        }
    }
}

@Composable
private fun ArchivedSessionCard(
    index: Int,
    count: Int,
    summary: AgentSessionSummary,
    onOpen: () -> Unit,
    onRestore: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    EverfoldGroupedItem(
        index = index,
        count = count,
        onClick = onOpen,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = MAX_PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = summary.updatedAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (summary.lastMessagePreview.isNotBlank()) {
                Text(
                    text = summary.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = MAX_PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ArchivedMetadataAndActions(
                summary = summary,
                onRestore = onRestore,
                onRequestDelete = onRequestDelete,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArchivedMetadataAndActions(
    summary: AgentSessionSummary,
    onRestore: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArchivedTag(icon = Icons.Rounded.FolderOpen, label = summary.workspaceName)
            ArchivedTag(icon = Icons.Rounded.Hub, label = summary.loopHostNodeName)
            EverfoldStatusPill(text = stringResource(summary.loopState.labelRes()))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onRequestDelete,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.archived_sessions_delete))
            }
            FilledTonalButton(
                onClick = onRestore,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.archived_sessions_restore))
            }
        }
    }
}

@Composable
private fun ArchivedTag(
    icon: ImageVector,
    label: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
