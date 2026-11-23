package com.everfold.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.AppInfo
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    private val authorityGateway: AuthorityGateway,
    val appInfo: AppInfo,
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = settingsStore.preferences

    fun setThemeMode(mode: ThemeMode) = settingsStore.updateThemeMode(mode)

    fun setThemeColorMode(mode: ThemeColorMode) = settingsStore.updateThemeColorMode(mode)

    fun setSeedColor(preset: SeedColorPreset) = settingsStore.updateSeedColor(preset)

    fun setCustomColorHex(hex: String) = settingsStore.updateCustomColorHex(hex)

    fun setLanguage(language: AppLanguage) = settingsStore.updateLanguage(language)

    fun setChatTextSize(size: ChatTextSize) = settingsStore.updateChatTextSize(size)

    fun setAutoCollapseLongMessages(enabled: Boolean) = settingsStore.updateAutoCollapseLongMessages(enabled)

    fun setExpandRunningActionTraces(enabled: Boolean) = settingsStore.updateExpandRunningActionTraces(enabled)

    fun setExpandCompletedActionTraces(enabled: Boolean) = settingsStore.updateExpandCompletedActionTraces(enabled)

    fun disconnectAuthority() {
        viewModelScope.launch {
            authorityGateway.disconnect()
        }
    }
}
