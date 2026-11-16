package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.attach_files
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame

@Composable
internal actual fun rememberFilePicker(): FilePicker {
    val title = localizedString(Res.string.attach_files)
    return remember(title) {
        FilePicker { onResult ->
            EventQueue.invokeLater {
                onResult(
                    runCatching {
                        val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
                        try {
                            dialog.isMultipleMode = true
                            dialog.isVisible = true
                            dialog.files.filter { it.isFile }.map { AttachmentSource(it.name, it.toURI().toString()) }
                        } finally {
                            dialog.dispose()
                        }
                    },
                )
            }
        }
    }
}
