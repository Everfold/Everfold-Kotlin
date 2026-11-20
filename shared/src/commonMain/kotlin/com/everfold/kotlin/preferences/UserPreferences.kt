package com.everfold.kotlin.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.theme.AppearancePreferences
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset

/** 纯内存态界面偏好，不读写任何系统本地持久化存储。 */
internal class UserPreferencesState(
    initial: UserPreferences = UserPreferences(),
) : AppearancePreferences {
    var value by mutableStateOf(initial)
        private set

    override val themeMode: ThemeMode get() = value.themeMode
    override val themeColor: ThemeColorSelection get() = value.themeColor

    override fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    override fun useSystemThemeColor() = update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.System)) }

    override fun selectThemePreset(preset: ThemePreset) =
        update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.Preset, preset = preset)) }

    override fun applyCustomThemeColor(color: Color) =
        update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.Custom, customSeed = color)) }

    fun markOnboardingCompleted() = update { it.copy(onboardingCompleted = true) }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(value)
        if (next != value) {
            value = next
        }
    }
}

internal data class UserPreferences(
    val language: AppLanguage = AppLanguage.System,
    val textSize: ConversationTextSize = ConversationTextSize.System,
    val shortcut: SendShortcut = SendShortcut.ModifiedEnter,
    val collapsePrompts: Boolean = true,
    val sideMargins: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val themeColor: ThemeColorSelection = ThemeColorSelection(),
    val onboardingCompleted: Boolean = false,
)
