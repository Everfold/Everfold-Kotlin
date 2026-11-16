package com.everfold.kotlin.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// 在系统无障碍字体缩放之上乘倍率而非覆盖系统设置
enum class ConversationTextSize(
    val scale: Float,
) {
    System(1f),
    Small(0.875f),
    Large(1.125f),
    ExtraLarge(1.25f),
}

/** 发送快捷键规则输入法组合Shift 或 Alt 按下时不触发发送 */
enum class SendShortcut {
    ModifiedEnter,
    Enter,
    ;

    fun matches(
        ctrl: Boolean,
        meta: Boolean,
        shift: Boolean,
        alt: Boolean,
        desktop: Boolean,
        composing: Boolean,
    ): Boolean {
        if (composing || shift || alt) return false
        return when (this) {
            ModifiedEnter -> ctrl || meta
            Enter -> desktop && !ctrl && !meta
        }
    }
}

/** 局部缩放会话正文和输入框保留系统字体缩放不影响其他界面 */
@Composable
internal fun ConversationTextScale(
    size: ConversationTextSize,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * size.scale), content = content)
}
