package com.everfold.kotlin.ui.components

import androidx.compose.ui.Modifier

internal actual val desktopUi: Boolean = false

internal actual val macOsUi: Boolean = false

internal actual fun Modifier.horizontalResizePointer(): Modifier = this
