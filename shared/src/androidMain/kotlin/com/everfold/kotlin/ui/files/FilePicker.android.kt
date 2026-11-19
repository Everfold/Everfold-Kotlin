package com.everfold.kotlin.ui.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal actual fun rememberFilePicker(onResult: (Result<List<AttachmentSource>>) -> Unit): FilePicker {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val latestOnResult by rememberUpdatedState(onResult)
    var inFlight by rememberSaveable { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            inFlight = false
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { uris.map { resolver.attachment(it) } } }
                latestOnResult(result)
            }
        }
    return remember(launcher) {
        FilePicker {
            if (!inFlight) {
                inFlight = true
                runCatching { launcher.launch(arrayOf("*/*")) }.onFailure {
                    inFlight = false
                    latestOnResult(Result.failure(it))
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
