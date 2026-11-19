package com.everfold.kotlin.ui.components

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.TooltipAnchorPosition
import top.yukonga.miuix.kmp.basic.TooltipBox

/** 共享文字提示封装标签由调用处本地化后传入 */
@Composable
internal fun AppTooltip(
    label: String,
    content: @Composable () -> Unit,
) {
    TooltipBox(
        text = label,
        positioning = TooltipAnchorPosition.Below,
        content = content,
    )
}
