package com.everfold.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.ui.connection.summaryLine
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ConnectionSummaryCard(
    isLocalConnected: Boolean,
    connectedProfiles: ImmutableList<ConnectionProfile>,
    onDisconnectAll: () -> Unit,
    onConnectLocal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalCount = connectedProfiles.size + if (isLocalConnected) 1 else 0
    val isConnected = totalCount > 0
    val (title, subtitle) =
        connectionSummaryTexts(
            isLocalConnected = isLocalConnected,
            connectedProfiles = connectedProfiles,
            totalCount = totalCount,
        )
    val containerColor =
        if (isConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val subtitleColor =
        if (isConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(onClick = if (isConnected) onDisconnectAll else onConnectLocal) {
                Text(
                    stringResource(
                        if (isConnected) R.string.nodes_action_disconnect_all else R.string.nodes_action_use_local,
                    ),
                )
            }
        }
    }
}

@Composable
private fun connectionSummaryTexts(
    isLocalConnected: Boolean,
    connectedProfiles: ImmutableList<ConnectionProfile>,
    totalCount: Int,
): Pair<String, String> {
    val title =
        when {
            totalCount == 0 -> stringResource(R.string.nodes_disconnected)
            isLocalConnected && connectedProfiles.isEmpty() -> stringResource(R.string.nodes_mode_local)
            else -> pluralStringResource(R.plurals.nodes_connected_count, totalCount, totalCount)
        }
    val names =
        buildList {
            if (isLocalConnected) add(stringResource(R.string.nodes_mode_local))
            connectedProfiles.forEach { add(it.name) }
        }
    val subtitle =
        when {
            totalCount == 0 -> stringResource(R.string.nodes_disconnected_subtitle)
            isLocalConnected && connectedProfiles.isEmpty() -> stringResource(R.string.nodes_local_subtitle)
            else -> names.joinToString(" · ")
        }
    return title to subtitle
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SavedProfileCard(
    index: Int,
    count: Int,
    profile: ConnectionProfile,
    isActive: Boolean,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EverfoldGroupedItem(
        index = index,
        count = count,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SavedProfileHeader(profile = profile, isActive = isActive, onOpenMenu = onOpenMenu)
            Text(
                text = profile.transport.summaryLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (profile.note.isNotBlank()) {
                Text(
                    text = profile.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isActive) {
                    OutlinedButton(onClick = onDisconnect) { Text(stringResource(R.string.nodes_action_disconnect)) }
                } else {
                    FilledTonalButton(onClick = onConnect, enabled = !isConnecting) {
                        if (isConnecting) {
                            LoadingIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Text(stringResource(R.string.nodes_action_connect))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedProfileHeader(
    profile: ConnectionProfile,
    isActive: Boolean,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isActive) {
            EverfoldStatusPill(text = stringResource(R.string.nodes_profile_connected_badge))
        }
        IconButton(onClick = onOpenMenu) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = stringResource(R.string.nodes_action_more),
            )
        }
    }
}
