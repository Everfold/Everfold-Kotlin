package com.everfold.android

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun EverfoldDrawerSheet(
    sessions: ImmutableList<AgentSessionSummary>,
    isNewSessionSelected: Boolean,
    activeSessionId: String?,
    isNodesSelected: Boolean,
    isSettingsSelected: Boolean,
    onNewSession: () -> Unit,
    onSelectSession: (String) -> Unit,
    onOpenNodes: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        NavigationDrawerItem(
            icon = { Icon(Icons.Rounded.AddComment, contentDescription = null) },
            label = { Text(stringResource(R.string.drawer_new_session)) },
            selected = isNewSessionSelected,
            onClick = onNewSession,
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerSessionsList(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onSelectSession = onSelectSession,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        DrawerBottomDestinations(
            isNodesSelected = isNodesSelected,
            isSettingsSelected = isSettingsSelected,
            onOpenNodes = onOpenNodes,
            onOpenSettings = onOpenSettings,
        )
    }
}

@Composable
private fun DrawerSessionsList(
    sessions: ImmutableList<AgentSessionSummary>,
    activeSessionId: String?,
    onSelectSession: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.drawer_sessions_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        if (sessions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.drawer_sessions_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        } else {
            items(sessions, key = { it.id }) { summary ->
                DrawerSessionItem(
                    summary = summary,
                    selected = activeSessionId == summary.id,
                    onClick = { onSelectSession(summary.id) },
                )
            }
        }
    }
}

@Composable
private fun DrawerSessionItem(
    summary: AgentSessionSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val isRunning =
        summary.loopState == LoopState.PLANNING ||
            summary.loopState == LoopState.EXECUTING_ACTION
    NavigationDrawerItem(
        icon = { Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null) },
        label = {
            Text(
                text = summary.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        badge =
            if (isRunning) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                null
            },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
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
