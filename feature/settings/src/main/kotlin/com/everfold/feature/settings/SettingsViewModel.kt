package com.everfold.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    private val authorityGateway: AuthorityGateway,
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = settingsStore.preferences

    fun setThemeMode(mode: ThemeMode) = settingsStore.updateThemeMode(mode)

    fun setDynamicColor(enabled: Boolean) = settingsStore.updateDynamicColor(enabled)

    fun setSeedColor(preset: SeedColorPreset) = settingsStore.updateSeedColor(preset)

    fun setShowNodeBadgesInline(enabled: Boolean) = settingsStore.updateShowNodeBadgesInline(enabled)

    fun disconnectAuthority() {
        viewModelScope.launch {
            authorityGateway.disconnect()
        }
    }
}
