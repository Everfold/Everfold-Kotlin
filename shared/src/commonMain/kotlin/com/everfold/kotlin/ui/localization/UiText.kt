package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource

/** 界面文案字面量保留原样资源引用在渲染时按当前语言解析 */
sealed interface UiText {
    /** 原始或用户输入的文案不进行翻译 */
    data class Literal(
        val value: String,
    ) : UiText

    /** 保存资源引用而非已翻译字符串中文文案见资源声明处 */
    data class Resource(
        val value: StringResource,
    ) : UiText
}

@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Literal -> value
        is UiText.Resource -> localizedString(value)
    }
