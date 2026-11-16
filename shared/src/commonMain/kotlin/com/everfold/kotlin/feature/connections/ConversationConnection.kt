package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_manage
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.connection_session_unavailable

/** 新草稿可选择目标已有会话只打开其绑定连接不会被列表选择影响 */
@Composable
internal fun ConversationConnection(
    connections: List<Connection>,
    connectionId: String,
    canSelect: Boolean,
    onSelect: (String) -> Unit,
    onOpen: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val connection = connections.firstOrNull { it.id == connectionId }
    val label =
        connection?.let { "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}" }
            ?: localizedString(Res.string.connection_removed)
    Box {
        TextButton(onClick = { if (canSelect) expanded = true else onOpen() }) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            connections.forEach { item ->
                DropdownMenuItem(
                    text = { Text("${item.profile.name.resolve()} · ${localizedString(item.runtime.status.label)}") },
                    onClick = {
                        onSelect(item.id)
                        expanded = false
                    },
                )
            }
            DropdownMenuItem(text = { Text(localizedString(Res.string.connection_manage)) }, onClick = {
                expanded = false
                onOpen()
            })
        }
    }
}

@Composable
internal fun ConnectionUnavailableNotice(
    connection: Connection?,
    onOpen: () -> Unit,
) {
    if (connection?.isConnected == true) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            localizedString(Res.string.connection_session_unavailable),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onOpen) { Text(localizedString(Res.string.connection_manage)) }
    }
}
