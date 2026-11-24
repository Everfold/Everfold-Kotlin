package com.everfold.android.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.android.R
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import kotlinx.collections.immutable.ImmutableList
import com.everfold.core.ui.R as CoreUiR

@Composable
internal fun EverfoldDrawerSheet(
    sessions: ImmutableList<AgentSessionSummary>,
    isNewSessionSelected: Boolean,
    activeSessionId: String?,
    isArchivedSelected: Boolean,
    isNodesSelected: Boolean,
    isSettingsSelected: Boolean,
    onOpenSearch: () -> Unit,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onSessionLongClick: (AgentSessionSummary) -> Unit,
    onOpenArchive: () -> Unit,
    onOpenNodes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(
        modifier = modifier,
        drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        DrawerTopActionRow(
            isNewSessionSelected = isNewSessionSelected,
            onOpenSearch = onOpenSearch,
            onNewSession = onNewSession,
        )
        Spacer(modifier = Modifier.height(8.dp))
        DrawerSessionsList(
            sessions = sessions,
            activeSessionId = activeSessionId,
            isArchivedSelected = isArchivedSelected,
            onSelectSession = onSelectSession,
            onSessionLongClick = onSessionLongClick,
            onOpenArchive = onOpenArchive,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DrawerBottomDestinations(
            isNodesSelected = isNodesSelected,
            isSettingsSelected = isSettingsSelected,
            onOpenNodes = onOpenNodes,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun DrawerTopActionRow(
    isNewSessionSelected: Boolean,
    onOpenSearch: () -> Unit,
    onNewSession: () -> Unit,
) {
    val newSessionColors =
        if (isNewSessionSelected) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f).height(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            onClick = onOpenSearch,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.drawer_search_sessions),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        FilledTonalIconButton(
            onClick = onNewSession,
            modifier = Modifier.size(48.dp),
            colors = newSessionColors,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddComment,
                contentDescription = stringResource(CoreUiR.string.common_new_session),
            )
        }
    }
}

@Composable
private fun DrawerSessionsList(
    sessions: ImmutableList<AgentSessionSummary>,
    activeSessionId: String?,
    isArchivedSelected: Boolean,
    onSelectSession: (String) -> Unit,
    onSessionLongClick: (AgentSessionSummary) -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSessions = sessions.filterNot { it.isArchived }
    val pinnedSessions = activeSessions.filter { it.isPinned }
    val recentSessions = activeSessions.filterNot { it.isPinned }
    val archivedCount = sessions.count { it.isArchived }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (pinnedSessions.isNotEmpty()) {
            item(key = "pinned-header") {
                DrawerSectionLabel(stringResource(R.string.drawer_sessions_pinned_header))
            }
            items(pinnedSessions, key = { it.id }) { summary ->
                DrawerSessionItem(
                    summary = summary,
                    selected = activeSessionId == summary.id,
                    onClick = { onSelectSession(summary.id) },
                    onLongClick = { onSessionLongClick(summary) },
                )
            }
        }
        item(key = "recent-header") {
            DrawerSectionLabel(stringResource(R.string.drawer_sessions_header))
        }
        if (activeSessions.isEmpty()) {
            item(key = "sessions-empty") {
                Text(
                    text = stringResource(R.string.drawer_sessions_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(recentSessions, key = { it.id }) { summary ->
                DrawerSessionItem(
                    summary = summary,
                    selected = activeSessionId == summary.id,
                    onClick = { onSelectSession(summary.id) },
                    onLongClick = { onSessionLongClick(summary) },
                )
            }
        }
        if (archivedCount > 0) {
            item(key = "archived-entry") {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Inventory2, contentDescription = null) },
                    label = { Text(stringResource(R.string.drawer_archived_sessions)) },
                    badge = { Text(archivedCount.toString()) },
                    selected = isArchivedSelected,
                    onClick = onOpenArchive,
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
        }
    }
}

@Composable
private fun DrawerSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerSessionItem(
    summary: AgentSessionSummary,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val containerColor =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(NavigationDrawerItemDefaults.ItemPadding)
                .height(56.dp)
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    },
                ),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        DrawerSessionRowContent(summary = summary)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DrawerSessionRowContent(summary: AgentSessionSummary) {
    val isRunning =
        summary.loopState == LoopState.PLANNING ||
            summary.loopState == LoopState.EXECUTING_ACTION
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = if (summary.isPinned) Icons.Rounded.PushPin else Icons.Rounded.ChatBubbleOutline,
            contentDescription = null,
        )
        Box(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isRunning) {
            LoadingIndicator(modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun DrawerBottomDestinations(
    isNodesSelected: Boolean,
    isSettingsSelected: Boolean,
    onOpenNodes: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        NavigationDrawerItem(
            icon = { Icon(Icons.Rounded.Hub, contentDescription = null) },
            label = { Text(stringResource(R.string.drawer_nodes)) },
            selected = isNodesSelected,
            onClick = onOpenNodes,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
            label = { Text(stringResource(R.string.drawer_settings)) },
            selected = isSettingsSelected,
            onClick = onOpenSettings,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}
