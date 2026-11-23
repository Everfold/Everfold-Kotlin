package com.everfold.core.gateway

import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsStore {
    val preferences: StateFlow<AppPreferences>

    fun updateThemeMode(mode: ThemeMode)

    fun updateThemeColorMode(mode: ThemeColorMode)

    fun updateSeedColor(preset: SeedColorPreset)

    fun updateCustomColorHex(hex: String)

    fun updateLanguage(language: AppLanguage)

    fun updateChatTextSize(size: ChatTextSize)

    fun updateAutoCollapseLongMessages(enabled: Boolean)

    fun updateShowNodeBadgesInline(enabled: Boolean)
}
