package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.attach_files
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal actual fun rememberFilePicker(onResult: (Result<List<AttachmentSource>>) -> Unit): FilePicker {
    val title = localizedString(Res.string.attach_files)
    val latestOnResult by rememberUpdatedState(onResult)
    val inFlight = remember { AtomicBoolean(false) }
    return remember(title) {
        FilePicker {
            if (!inFlight.compareAndSet(false, true)) return@FilePicker
            EventQueue.invokeLater {
                val result =
                    try {
                        runCatching {
                            val owner =
                                Frame.getFrames().firstOrNull { it.isActive && it.isDisplayable }
                                    ?: Frame.getFrames().firstOrNull { it.isDisplayable }
                            val dialog = FileDialog(owner, title, FileDialog.LOAD)
                            try {
                                dialog.isMultipleMode = true
                                dialog.isVisible = true
                                dialog.files.filter { it.isFile }.map { AttachmentSource(it.name, it.toURI().toString()) }
                            } finally {
                                dialog.dispose()
                            }
                        }
                    } finally {
                        inFlight.set(false)
                    }
                latestOnResult(result)
            }
        }
    }
}
