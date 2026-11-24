package com.everfold.feature.sessions.component

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.SessionAttachment
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableList
import java.util.Locale
import java.util.UUID

private const val BYTES_PER_KB = 1024L
private const val BYTES_PER_MB = BYTES_PER_KB * BYTES_PER_KB
private const val ATTACHMENT_CHIP_MAX_DP = 220

internal data class AttachmentLaunchers(
    val launchFiles: () -> Unit,
    val launchImages: () -> Unit,
)

@Composable
internal fun rememberAttachmentLaunchers(onAddAttachments: (List<SessionAttachment>) -> Unit): AttachmentLaunchers {
    val context = LocalContext.current
    val fileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                onAddAttachments(uris.map { readAttachmentFromUri(context, it) })
            }
        }
    val imageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                onAddAttachments(uris.map { readAttachmentFromUri(context, it) })
            }
        }
    return AttachmentLaunchers(
        launchFiles = { fileLauncher.launch(arrayOf("*/*")) },
        launchImages = {
            imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
    )
}

@Composable
internal fun SessionAttachmentStrip(
    attachments: ImmutableList<SessionAttachment>,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.id }) { attachment ->
            AttachmentChipItem(
                attachment = attachment,
                onRemove = { onRemoveAttachment(attachment.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AttachmentChipItem(
    attachment: SessionAttachment,
    onRemove: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.widthIn(max = ATTACHMENT_CHIP_MAX_DP.dp).padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (attachment.isImage) Icons.Rounded.Image else Icons.Rounded.Description,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = formatAttachmentLabel(attachment),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            IconButton(
                onClick = onRemove,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.session_composer_remove_attachment),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

internal fun readAttachmentFromUri(
    context: Context,
    uri: Uri,
): SessionAttachment {
    var displayName: String? = null
    var sizeBytes: Long? = null
    runCatching {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
    }
    val resolvedName = displayName?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty().ifBlank { "attachment" }
    val resolvedMime =
        runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty().ifBlank {
            "application/octet-stream"
        }
    return SessionAttachment(
        id = UUID.randomUUID().toString(),
        displayName = resolvedName,
        mimeType = resolvedMime,
        sizeBytes = sizeBytes,
        uriString = uri.toString(),
    )
}

internal fun formatAttachmentLabel(attachment: SessionAttachment): String {
    val sizeText = formatAttachmentSize(attachment.sizeBytes)
    return if (sizeText.isNotEmpty()) "${attachment.displayName} ($sizeText)" else attachment.displayName
}

internal fun formatAttachmentSize(bytes: Long?): String =
    when {
        bytes == null || bytes < 0L -> ""
        bytes < BYTES_PER_KB -> "${bytes}B"
        bytes < BYTES_PER_MB -> "${bytes / BYTES_PER_KB}KB"
        else -> String.format(Locale.US, "%.1fMB", bytes.toFloat() / BYTES_PER_MB)
    }
