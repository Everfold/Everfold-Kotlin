@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.everfold.kotlin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 提供当前页面记录的模糊背景供弹层和底部面板使用。 */
internal val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** 控制是否启用实时图层模糊；无头 Skiko UI 测试关闭以避免 CPU 着色器超时。 */
internal val LocalBackdropBlurEnabled = staticCompositionLocalOf { true }

@Composable
internal fun rememberOptionalLayerBackdrop(backgroundColor: Color = MiuixTheme.colorScheme.surface): LayerBackdrop? =
    if (LocalBackdropBlurEnabled.current) {
        rememberLayerBackdrop {
            drawRect(backgroundColor)
            drawContent()
        }
    } else {
        null
    }

internal fun Modifier.optionalLayerBackdrop(backdrop: LayerBackdrop?): Modifier = if (backdrop != null) layerBackdrop(backdrop) else this
