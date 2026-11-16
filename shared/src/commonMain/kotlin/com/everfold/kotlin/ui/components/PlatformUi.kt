package com.everfold.kotlin.ui.components

import androidx.compose.ui.Modifier

/** 平台交互差异入口桌面判定及侧栏横向调整光标由各平台实现 */
internal expect val desktopUi: Boolean

internal expect fun Modifier.horizontalResizePointer(): Modifier
