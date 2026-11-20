package com.everfold.kotlin.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

/** 界面文案字面量保留原样资源引用在渲染时按当前语言解析 */
@Immutable
sealed interface UiText {
    /** 原始或用户输入的文案不进行翻译 */
    data class Literal(
        val value: String,
    ) : UiText

    /** 保存资源引用及格式化参数而非已翻译字符串中文文案见资源声明处 */
    data class Resource(
        val value: StringResource,
        val args: List<Any> = emptyList(),
    ) : UiText
}

internal fun uiResource(
    value: StringResource,
    vararg args: Any,
): UiText.Resource = UiText.Resource(value, args.toList())

@Composable
fun UiText.resolve(): String =
    when (this) {
        is UiText.Literal -> value
        is UiText.Resource -> if (args.isEmpty()) localizedString(value) else localizedString(value, *args.toTypedArray())
    }
