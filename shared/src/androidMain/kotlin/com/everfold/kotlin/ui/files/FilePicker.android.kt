package com.everfold.kotlin.ui.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberFilePicker(): FilePicker {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val pending = remember { mutableStateOf<((Result<List<AttachmentSource>>) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            val callback = pending.value
            pending.value = null
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { uris.map { resolver.attachment(it) } } }
                callback?.invoke(result)
            }
        }
    return remember(launcher) {
        FilePicker { callback ->
            if (pending.value == null) {
                pending.value = callback
                runCatching { launcher.launch(arrayOf("*/*")) }.onFailure {
                    pending.value = null
                    callback(Result.failure(it))
                }
            }
        }
    }
}

private fun ContentResolver.attachment(uri: Uri): AttachmentSource {
    val name =
        query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    return AttachmentSource(name ?: uri.lastPathSegment.orEmpty(), uri.toString())
}
