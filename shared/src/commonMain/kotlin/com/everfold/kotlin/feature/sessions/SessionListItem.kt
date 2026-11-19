package com.everfold.kotlin.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.label
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.pinned_session
import everfold.shared.generated.resources.session_actions
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SessionListItem(
    session: ConversationUiState,
    connectionName: String,
    selected: Boolean,
    menuExpanded: Boolean,
    onSelect: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val containerColor =
        if (selected) {
            MiuixTheme.colorScheme.secondaryContainer
        } else {
            MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
        }
    Surface(
        modifier = Modifier.fillMaxWidth().squircleSurface(containerColor, 16.dp).sessionItemGestures(session.id, onSelect, onOpenMenu),
        color = Color.Transparent,
    ) {
        val textEdgeInset = 6.dp
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SessionItemHeader(
                title = session.title.resolve(),
                selected = selected,
                menuExpanded = menuExpanded,
                isPinned = session.isPinned,
                textEdgeInset = textEdgeInset,
                onOpenMenu = onOpenMenu,
                onDismissMenu = onDismissMenu,
                onRename = onRename,
                onTogglePin = onTogglePin,
                onDelete = onDelete,
            )
            SessionItemMetadataRow(session, connectionName, textEdgeInset)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
private fun Modifier.sessionItemGestures(
    sessionId: String,
    onSelect: () -> Unit,
    onOpenMenu: () -> Unit,
): Modifier =
    combinedClickable(onClick = onSelect, onLongClick = onOpenMenu)
        .pointerInput(sessionId) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        onOpenMenu()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }

@Composable
private fun SessionItemHeader(
    title: String,
    selected: Boolean,
    menuExpanded: Boolean,
    isPinned: Boolean,
    textEdgeInset: Dp,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(start = textEdgeInset),
            style = MiuixTheme.textStyles.headline1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box {
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(32.dp)) {
                AppIcon(
                    AppSymbol.More,
                    modifier = Modifier.size(20.dp),
                    description = localizedString(Res.string.session_actions),
                )
            }
            SessionActionsPopup(
                expanded = menuExpanded,
                isPinned = isPinned,
                onDismiss = onDismissMenu,
                onRename = onRename,
                onTogglePin = onTogglePin,
                onDelete = onDelete,
            )
        }
    }
}

@Composable
private fun SessionItemMetadataRow(
    session: ConversationUiState,
    connectionName: String,
    textEdgeInset: Dp,
) {
    val status = localizedString(session.runState.label)
    val metadata =
        listOfNotNull(
            if (session.isPinned) localizedString(Res.string.pinned_session) else null,
            connectionName,
            status.takeIf { session.runState !in listOf(AgentRunState.Idle, AgentRunState.Completed) },
        ).joinToString(" · ")
    val statusColor =
        when (session.runState) {
            AgentRunState.Failed -> MiuixTheme.colorScheme.error
            AgentRunState.Running -> MiuixTheme.colorScheme.primary
            else -> MiuixTheme.colorScheme.onSurfaceVariantSummary
        }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = metadata,
            modifier = Modifier.weight(1f).padding(start = textEdgeInset),
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = statusColor,
        )
        Text(
            text = session.updatedLabel.resolve(),
            modifier = Modifier.padding(end = textEdgeInset),
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
