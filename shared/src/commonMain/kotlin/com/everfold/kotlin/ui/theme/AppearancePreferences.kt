package com.everfold.kotlin.ui.theme

import androidx.compose.ui.graphics.Color

/** 引导与设置共享的外观偏好实现负责发布可观察状态 */
internal interface AppearancePreferences {
    val themeMode: ThemeMode
    val themeColor: ThemeColorSelection

    fun setTheme(mode: ThemeMode)

    fun useSystemThemeColor()

    fun selectThemePreset(preset: ThemePreset)

    fun applyCustomThemeColor(color: Color)
}
