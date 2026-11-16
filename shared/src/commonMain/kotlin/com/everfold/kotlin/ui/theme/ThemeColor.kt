package com.everfold.kotlin.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.custom
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.preset_blue
import everfold.shared.generated.resources.preset_green
import everfold.shared.generated.resources.preset_orange
import everfold.shared.generated.resources.preset_pink
import everfold.shared.generated.resources.preset_purple
import everfold.shared.generated.resources.preset_teal
import org.jetbrains.compose.resources.StringResource

enum class ThemePreset(
    val label: StringResource,
    val seed: Color,
) {
    // 紫色
    Purple(Res.string.preset_purple, Color(0xFF6750A4)),

    // 蓝色
    Blue(Res.string.preset_blue, Color(0xFF0B57D0)),

    // 青色
    Teal(Res.string.preset_teal, Color(0xFF008080)),

    // 绿色
    Green(Res.string.preset_green, Color(0xFF00FF80)),

    // 橙色
    Orange(Res.string.preset_orange, Color(0xFF803F00)),

    // 粉色
    Pink(Res.string.preset_pink, Color(0xFFFF1E80)),
}

enum class ThemeColorMode { System, Preset, Custom }

data class ThemeColorSelection(
    val mode: ThemeColorMode = ThemeColorMode.System,
    val preset: ThemePreset = ThemePreset.Purple,
    val customSeed: Color = ThemePreset.Purple.seed,
) {
    val seed: Color
        get() =
            when (mode) {
                ThemeColorMode.System -> ThemePreset.Purple.seed
                ThemeColorMode.Preset -> preset.seed
                ThemeColorMode.Custom -> customSeed
            }
    val label: StringResource
        get() =
            when (mode) {
                // 跟随系统
                ThemeColorMode.System -> Res.string.follow_system

                ThemeColorMode.Preset -> preset.label

                // 自定义
                ThemeColorMode.Custom -> Res.string.custom
            }
}

internal fun Color.toHexColor(): String = "#" + (toArgb() and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')

/** 接受可选 # 前缀的六位 RGB并补上不透明 alpha非法输入返回 null */
internal fun parseHexColor(value: String): Color? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6 || hex.any { it !in '0'..'9' && it.lowercaseChar() !in 'a'..'f' }) return null
    return Color(0xFF000000.toInt() or (hex.toIntOrNull(16) ?: return null))
}
