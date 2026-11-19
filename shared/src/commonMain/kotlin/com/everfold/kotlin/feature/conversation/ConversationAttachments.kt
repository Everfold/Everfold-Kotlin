package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.attach_files
import everfold.shared.generated.resources.attachment_failed
import everfold.shared.generated.resources.attachment_remove
import everfold.shared.generated.resources.attachment_retry
import everfold.shared.generated.resources.attachment_uploading
import everfold.shared.generated.resources.dismiss_file_error
import everfold.shared.generated.resources.file_picker_failed

@Composable
internal fun DraftAttachments(
    state: ConversationUiState,
    options: ConversationOptions,
) {
    if (state.attachments.isEmpty()) return
    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.attachments, key = { it.id }) { file ->
            AttachmentCard(
                file,
                onRetry = { options.retryAttachment(state.id, file.id) },
                onRemove = { options.removeAttachment(state.id, file.id) },
            )
        }
    }
}

@Composable
private fun AttachmentCard(
    file: ConversationAttachment,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.widthIn(max = 280.dp).width(IntrinsicSize.Max).padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        file.source.name,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    val label =
                        when (file.status) {
                            AttachmentStatus.Uploading -> Res.string.attachment_uploading
                            AttachmentStatus.Ready -> null
                            AttachmentStatus.Failed -> Res.string.attachment_failed
                        }
                    if (label != null) {
                        Text(
                            localizedString(label),
                            style = MaterialTheme.typography.labelSmall,
                            color =
                                if (file.status ==
                                    AttachmentStatus.Failed
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
                if (file.status == AttachmentStatus.Failed) {
                    IconButton(
                        onClick = onRetry,
                    ) { AppIcon(AppSymbol.Retry, description = localizedString(Res.string.attachment_retry, file.source.name)) }
                }
                IconButton(
                    onClick = onRemove,
                ) { AppIcon(AppSymbol.Close, description = localizedString(Res.string.attachment_remove, file.source.name)) }
            }
            if (file.status ==
                AttachmentStatus.Uploading
            ) {
                LinearProgressIndicator(progress = { file.progress }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
internal fun AttachmentButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = localizedString(Res.string.attach_files)
    AppTooltip(label) {
        IconButton(onClick = onClick, enabled = enabled) { AppIcon(AppSymbol.Attachment, description = label) }
    }
}

@Composable
internal fun FilePickerError(onDismiss: () -> Unit) {
    Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            localizedString(Res.string.file_picker_failed),
            Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        IconButton(onClick = onDismiss) { AppIcon(AppSymbol.Close, description = localizedString(Res.string.dismiss_file_error)) }
    }
}

@Composable
internal fun MessageAttachments(files: List<ConversationAttachment>) {
    files.forEach { file ->
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppIcon(AppSymbol.Attachment)
                Text(file.source.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
