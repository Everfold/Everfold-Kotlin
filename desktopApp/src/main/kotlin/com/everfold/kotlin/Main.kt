package com.everfold.kotlin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.everfold.kotlin.ui.theme.EverfoldTheme
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.LocalMiuixWindowInfo
import top.yukonga.miuix.kmp.window.LocalWindowState
import top.yukonga.miuix.kmp.window.MiuixCaptionBar
import top.yukonga.miuix.kmp.window.MiuixWindow
import top.yukonga.miuix.kmp.window.MiuixWindowBackgroundType
import top.yukonga.miuix.kmp.window.MiuixWindowProperties
import top.yukonga.miuix.kmp.window.WindowDraggableArea

@OptIn(ExperimentalComposeUiApi::class)
fun main() =
    application {
        val windowState =
            rememberWindowState(
                size = DpSize(1200.dp, 820.dp),
                position = WindowPosition.Aligned(Alignment.Center),
            )
        var themeMode by remember { mutableStateOf(ThemeMode.System) }
        var themeColor by remember { mutableStateOf(ThemeColorSelection()) }
        EverfoldTheme(mode = themeMode, color = themeColor) {
            val isDark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
            val logo = everfoldLogoPainter()
            MiuixWindow(
                state = windowState,
                onCloseRequest = ::exitApplication,
                title = "Everfold",
                icon = logo,
                properties = desktopWindowProperties(isDark),
            ) {
                DesktopWindowContent(logo) { mode, color ->
                    themeMode = mode
                    themeColor = color
                }
            }
        }
    }

@Composable
private fun desktopWindowProperties(isDark: Boolean): MiuixWindowProperties<ComposeWindow> =
    MiuixWindowProperties.default<ComposeWindow>(
        minSize = DpSize(380.dp, 600.dp),
        captionButtonIsDarkTheme = isDark,
        backgroundIsDarkTheme = isDark,
        backgroundType =
            when {
                MiuixWindowBackgroundType.Vibrancy.isSupported() -> MiuixWindowBackgroundType.Vibrancy
                MiuixWindowBackgroundType.Mica.isSupported() -> MiuixWindowBackgroundType.Mica
                else -> MiuixWindowBackgroundType.None
            },
    )

@Composable
private fun DesktopWindowContent(
    logo: Painter,
    onAppearanceChange: (ThemeMode, ThemeColorSelection) -> Unit,
) {
    val isFullscreen = LocalWindowState.current.placement == WindowPlacement.Fullscreen
    val captionBarHeight = LocalMiuixWindowInfo.current.captionBarHeight
    Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface)) {
        if (!isFullscreen && captionBarHeight > 0.dp) {
            Box(Modifier.fillMaxWidth().height(captionBarHeight).background(MiuixTheme.colorScheme.surface)) {
                WindowDraggableArea()
                MiuixCaptionBar {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.width(8.dp))
                        Image(painter = logo, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "Everfold", style = MiuixTheme.textStyles.body2, maxLines = 1)
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            App(onAppearanceChange = onAppearanceChange)
        }
    }
}
