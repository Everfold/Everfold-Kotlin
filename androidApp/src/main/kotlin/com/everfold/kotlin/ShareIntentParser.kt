package com.everfold.kotlin

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.mock.longtask.IncomingShareContent

/** 解析来自系统分享面板的 `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 文本与附件 URI。 */
internal fun parseIncomingShareIntent(
    context: Context,
    intent: Intent?,
): IncomingShareContent? {
    val action = intent?.action
    if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
    val sharedText = extractSharedText(intent)
    val uris =
        if (action == Intent.ACTION_SEND) {
            listOfNotNull(readStreamUri(intent))
        } else {
            readStreamUriList(intent)
        }
    val attachments =
        uris.map { uri ->
            takeReadGrantSafely(context.contentResolver, uri, intent.flags)
            AttachmentSource(
                name = resolveDisplayName(context.contentResolver, uri),
                reference = uri.toString(),
            )
        }
    return IncomingShareContent(text = sharedText, attachments = attachments)
        .takeIf { sharedText != null || attachments.isNotEmpty() }
}

private fun extractSharedText(intent: Intent): String? {
    val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
    val body = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
    return when {
        subject.isNotEmpty() && body.isNotEmpty() && !body.startsWith(subject) -> "$subject\n$body"
        body.isNotEmpty() -> body
        subject.isNotEmpty() -> subject
        else -> null
    }
}

private fun readStreamUri(intent: Intent): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }

private fun readStreamUriList(intent: Intent): List<Uri> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
    }

private fun takeReadGrantSafely(
    resolver: ContentResolver,
    uri: Uri,
    intentFlags: Int,
) {
    if ((intentFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
        runCatching { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
}

private fun resolveDisplayName(
    resolver: ContentResolver,
    uri: Uri,
): String {
    val queried =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
    return queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: "shared-file"
}
