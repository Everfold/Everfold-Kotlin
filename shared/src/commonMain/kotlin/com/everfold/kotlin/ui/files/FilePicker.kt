package com.everfold.kotlin.ui.files

import androidx.compose.runtime.Composable
import com.everfold.kotlin.feature.conversation.AttachmentSource

/** 平台文件选择器触发器；选择结果统一交付给注册回调，取消选择返回空列表。 */
internal fun interface FilePicker {
    fun launch()
}

@Composable
internal expect fun rememberFilePicker(onResult: (Result<List<AttachmentSource>>) -> Unit): FilePicker
