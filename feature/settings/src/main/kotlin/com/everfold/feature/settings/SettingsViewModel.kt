package com.everfold.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.AppInfo
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsStore: AppSettingsStore,
    sessionGateway: SessionGateway,
    val appInfo: AppInfo,
) : ViewModel() {
    val preferences: StateFlow<AppPreferences> = settingsStore.preferences

    val archivedSessionCount: StateFlow<Int> =
        sessionGateway.sessions
            .map { sessions -> sessions.count { it.isArchived } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = 0,
            )

    fun setThemeMode(mode: ThemeMode) = settingsStore.updateThemeMode(mode)

    fun setThemeColorMode(mode: ThemeColorMode) = settingsStore.updateThemeColorMode(mode)

    fun setSeedColor(preset: SeedColorPreset) = settingsStore.updateSeedColor(preset)

    fun setCustomColorHex(hex: String) = settingsStore.updateCustomColorHex(hex)

    fun setLanguage(language: AppLanguage) = settingsStore.updateLanguage(language)

    fun setAnimationSpeed(speed: AnimationSpeed) = settingsStore.updateAnimationSpeed(speed)

    fun setChatTextSize(size: ChatTextSize) = settingsStore.updateChatTextSize(size)

    fun setAutoCollapseLongMessages(enabled: Boolean) = settingsStore.updateAutoCollapseLongMessages(enabled)

    fun setExpandRunningActionTraces(enabled: Boolean) =
        settingsStore.updateActionTraceExpansion(
            expandRunning = enabled,
            expandCompleted = preferences.value.expandCompletedActionTraces,
        )

    fun setExpandCompletedActionTraces(enabled: Boolean) =
        settingsStore.updateActionTraceExpansion(
            expandRunning = preferences.value.expandRunningActionTraces,
            expandCompleted = enabled,
        )

    fun setNotifications(notifications: NotificationPreferences) = settingsStore.updateNotifications(notifications)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
