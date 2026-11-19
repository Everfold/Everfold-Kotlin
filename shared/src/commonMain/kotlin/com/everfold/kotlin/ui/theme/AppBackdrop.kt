@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.everfold.kotlin.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import top.yukonga.miuix.kmp.blur.Backdrop

/** 提供当前页面记录的模糊背景供弹层和底部面板使用。 */
internal val LocalAppBackdrop = staticCompositionLocalOf<Backdrop?> { null }
