package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.SessionAttachment
import com.everfold.core.ui.ActionTraceGroupCard
import com.everfold.core.ui.MarkdownMessageContent
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableList

private const val LONG_MESSAGE_CHAR_LIMIT = 240
private const val COLLAPSED_PREVIEW_CHARS = 160

@Composable
internal fun ObservedItemRow(
    item: ObservedItem,
    isPaused: Boolean,
    autoCollapseLongMessages: Boolean,
    expandRunningActionTraces: Boolean,
    expandCompletedActionTraces: Boolean,
) {
    when (item) {
        is ObservedItem.UserMessage -> {
            UserMessageBubble(
                content = item.content,
                attachments = item.attachments,
                autoCollapseLongMessages = autoCollapseLongMessages,
            )
        }

        is ObservedItem.ActionGroup -> {
            ActionTraceGroupCard(
                actions = item.actions,
                expandWhenRunning = expandRunningActionTraces,
                expandWhenCompleted = expandCompletedActionTraces,
                isPaused = isPaused,
            )
        }

        is ObservedItem.AssistantMessage -> {
            AssistantMessageCard(
                item = item,
                autoCollapseLongMessages = autoCollapseLongMessages,
            )
        }
    }
}

@Composable
private fun UserMessageBubble(
    content: String,
    attachments: ImmutableList<SessionAttachment>,
    autoCollapseLongMessages: Boolean,
) {
    val isLong = autoCollapseLongMessages && content.length > LONG_MESSAGE_CHAR_LIMIT
    var expanded by rememberSaveable(content, autoCollapseLongMessages) { mutableStateOf(!isLong) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.widthIn(max = 320.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (attachments.isNotEmpty()) {
                    UserMessageAttachmentPills(attachments = attachments)
                }
                if (content.isNotBlank()) {
                    Text(
                        text = if (expanded) content else "${content.take(COLLAPSED_PREVIEW_CHARS)}…",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (isLong) {
                        MessageCollapseToggle(expanded = expanded, onToggle = { expanded = !expanded })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UserMessageAttachmentPills(attachments: ImmutableList<SessionAttachment>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        attachments.forEach { attachment ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = if (attachment.isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = formatAttachmentLabel(attachment),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMessageCard(
    item: ObservedItem.AssistantMessage,
    autoCollapseLongMessages: Boolean,
) {
    val isLong =
        autoCollapseLongMessages &&
            !item.isStreaming &&
            item.markdown.length > LONG_MESSAGE_CHAR_LIMIT
    var expanded by rememberSaveable(item.id, autoCollapseLongMessages) { mutableStateOf(!isLong) }
    val rendered =
        if (expanded || item.isStreaming) {
            item.markdown
        } else {
            "${item.markdown.take(COLLAPSED_PREVIEW_CHARS)}…"
        }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            MarkdownMessageContent(content = rendered)
            if (isLong) {
                MessageCollapseToggle(expanded = expanded, onToggle = { expanded = !expanded })
            }
        }
    }
}

@Composable
private fun ColumnScope.MessageCollapseToggle(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    TextButton(
        onClick = onToggle,
        modifier = Modifier.align(Alignment.End),
    ) {
        Text(
            text =
                stringResource(
                    if (expanded) {
                        R.string.session_message_collapse
                    } else {
                        R.string.session_message_expand
                    },
                ),
        )
    }
}
