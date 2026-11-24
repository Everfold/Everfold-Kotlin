package com.everfold.core.gateway.mock

import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InMemoryAppSettingsStore : AppSettingsStore {
    private val _preferences = MutableStateFlow(AppPreferences())
    override val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun updateThemeMode(mode: ThemeMode) {
        _preferences.update { it.copy(themeMode = mode) }
    }

    override fun updateThemeColorMode(mode: ThemeColorMode) {
        _preferences.update { it.copy(themeColorMode = mode) }
    }

    override fun updateSeedColor(preset: SeedColorPreset) {
        _preferences.update { it.copy(seedColor = preset) }
    }

    override fun updateCustomColorHex(hex: String) {
        _preferences.update { it.copy(customColorHex = hex) }
    }

    override fun updateLanguage(language: AppLanguage) {
        _preferences.update { it.copy(language = language) }
    }

    override fun updateAnimationSpeed(speed: AnimationSpeed) {
        _preferences.update { it.copy(animationSpeed = speed) }
    }

    override fun updateChatTextSize(size: ChatTextSize) {
        _preferences.update { it.copy(chatTextSize = size) }
    }

    override fun updateAutoCollapseLongMessages(enabled: Boolean) {
        _preferences.update { it.copy(autoCollapseLongMessages = enabled) }
    }

    override fun updateActionTraceExpansion(
        expandRunning: Boolean,
        expandCompleted: Boolean,
    ) {
        _preferences.update {
            it.copy(
                expandRunningActionTraces = expandRunning,
                expandCompletedActionTraces = expandCompleted,
            )
        }
    }

    override fun updateNotifications(notifications: NotificationPreferences) {
        _preferences.update { it.copy(notifications = notifications) }
    }

    override fun markOnboardingCompleted() {
        _preferences.update { it.copy(onboardingCompleted = true) }
    }
}
