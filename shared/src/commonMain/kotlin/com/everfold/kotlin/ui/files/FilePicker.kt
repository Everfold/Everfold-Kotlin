package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import com.everfold.kotlin.feature.conversation.AttachmentSource

/** 回调属于启动时的调用者，取消选择返回空列表。 */
internal fun interface FilePicker {
    fun launch(onResult: (Result<List<AttachmentSource>>) -> Unit)
}

@Composable
internal expect fun rememberFilePicker(): FilePicker
