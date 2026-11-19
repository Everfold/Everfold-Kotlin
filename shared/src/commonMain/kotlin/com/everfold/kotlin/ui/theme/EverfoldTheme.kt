package com.everfold.kotlin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.theme_dark
import everfold.shared.generated.resources.theme_light
import org.jetbrains.compose.resources.StringResource
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

enum class ThemeMode(
    val label: StringResource,
) {
    // 跟随系统
    System(Res.string.follow_system),

    // 浅色
    Light(Res.string.theme_light),

    // 深色
    Dark(Res.string.theme_dark),
}

@Composable
fun EverfoldTheme(
    mode: ThemeMode = ThemeMode.System,
    color: ThemeColorSelection = ThemeColorSelection(),
    content: @Composable () -> Unit,
) {
    val colorSchemeMode =
        when (mode) {
            ThemeMode.System -> ColorSchemeMode.MonetSystem
            ThemeMode.Light -> ColorSchemeMode.MonetLight
            ThemeMode.Dark -> ColorSchemeMode.MonetDark
        }
    val keyColor =
        when (color.mode) {
            ThemeColorMode.System -> null
            ThemeColorMode.Preset, ThemeColorMode.Custom -> color.seed
        }
    val controller =
        remember(colorSchemeMode, keyColor) {
            ThemeController(
                colorSchemeMode = colorSchemeMode,
                keyColor = keyColor,
            )
        }
    MiuixTheme(controller = controller, content = content)
}
