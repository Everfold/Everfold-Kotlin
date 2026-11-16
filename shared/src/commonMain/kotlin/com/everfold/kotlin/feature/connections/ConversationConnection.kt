package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.SelectionButton
import com.everfold.kotlin.ui.components.SelectionChoice
import com.everfold.kotlin.ui.components.SelectionMenu
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_connection
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
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val connection = connections.firstOrNull { it.id == connectionId }
    val label =
        connection?.let { "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}" }
            ?: localizedString(Res.string.connection_removed)
    Box(modifier) {
        SelectionButton(
            title = localizedString(Res.string.choose_connection),
            label = label,
            onClick = { if (canSelect) expanded = true else onOpen() },
            showArrow = canSelect,
            leadingIcon = {
                Box(
                    Modifier.size(8.dp).background(
                        if (connection?.isConnected == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        CircleShape,
                    ),
                )
            },
        )
        val choices =
            connections.map {
                SelectionChoice(
                    it.id,
                    "${it.profile.name.resolve()} · ${localizedString(it.runtime.status.label)}",
                    selected =
                        it.id == connectionId,
                )
            } + SelectionChoice("manage", localizedString(Res.string.connection_manage))
        SelectionMenu(localizedString(Res.string.choose_connection), choices, expanded, { expanded = false }) {
            expanded = false
            if (it == "manage") onOpen() else onSelect(it)
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
