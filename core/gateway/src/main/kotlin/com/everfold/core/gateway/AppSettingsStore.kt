package com.everfold.core.gateway

import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsStore {
    val preferences: StateFlow<AppPreferences>

    fun updateThemeMode(mode: ThemeMode)

    fun updateDynamicColor(enabled: Boolean)

    fun updateSeedColor(preset: SeedColorPreset)

    fun updateShowNodeBadgesInline(enabled: Boolean)
}
