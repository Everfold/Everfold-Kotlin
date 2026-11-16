package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberFilePicker(): FilePicker {
    val presenter = LocalUIViewController.current
    return remember(presenter) { FilePicker(IosFilePicker(presenter)::launch) }
}

@OptIn(ExperimentalForeignApi::class)
private class IosFilePicker(
    private val presenter: UIViewController,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    private var callback: ((Result<List<AttachmentSource>>) -> Unit)? = null

    fun launch(onResult: (Result<List<AttachmentSource>>) -> Unit) {
        if (callback != null) return
        callback = onResult
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
        picker.allowsMultipleSelection = true
        picker.delegate = this
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        finish(
            runCatching {
                didPickDocumentsAtURLs.filterIsInstance<NSURL>().map { url ->
                    AttachmentSource(url.lastPathComponent.orEmpty(), url.absoluteString.orEmpty())
                }
            },
        )
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        finish(Result.success(emptyList()))
    }

    private fun finish(result: Result<List<AttachmentSource>>) {
        val onResult = callback
        callback = null
        onResult?.invoke(result)
    }
}
