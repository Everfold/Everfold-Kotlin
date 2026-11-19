package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import com.everfold.kotlin.feature.conversation.AttachmentSource
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSURL
import platform.UIKit.UIAdaptivePresentationControllerDelegateProtocol
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIPresentationController
import platform.UIKit.UIViewController
import platform.UIKit.presentationController
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun rememberFilePicker(onResult: (Result<List<AttachmentSource>>) -> Unit): FilePicker {
    val presenter = LocalUIViewController.current
    val latestOnResult by rememberUpdatedState(onResult)
    val picker = remember(presenter) { IosFilePicker(presenter) { latestOnResult(it) } }
    return remember(picker) { FilePicker(picker::launch) }
}

@OptIn(ExperimentalForeignApi::class)
private class IosFilePicker(
    private val presenter: UIViewController,
    private val onResult: (Result<List<AttachmentSource>>) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol,
    UIAdaptivePresentationControllerDelegateProtocol {
    private var inFlight = false

    fun launch() {
        if (inFlight) return
        val target = presenter.topPresenter()
        if (target.isBeingPresented() || target.isBeingDismissed()) {
            onResult(Result.failure(IllegalStateException("Presenter is busy")))
            return
        }
        inFlight = true
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeItem), asCopy = true)
        picker.allowsMultipleSelection = true
        picker.delegate = this
        picker.presentationController?.delegate = this
        target.presentViewController(picker, animated = true, completion = null)
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

    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
        finish(Result.success(emptyList()))
    }

    private fun finish(result: Result<List<AttachmentSource>>) {
        if (!inFlight) return
        inFlight = false
        onResult(result)
    }
}

private tailrec fun UIViewController.topPresenter(): UIViewController = presentedViewController?.topPresenter() ?: this
