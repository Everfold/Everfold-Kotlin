package com.everfold.kotlin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIUserInterfaceStyle

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun SystemBarsAppearance(dark: Boolean) {
    val viewController = LocalUIViewController.current
    val style = if (dark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    SideEffect {
        viewController.overrideUserInterfaceStyle = style
        viewController.viewIfLoaded?.window?.overrideUserInterfaceStyle = style
    }
}
