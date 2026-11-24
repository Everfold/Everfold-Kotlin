package com.everfold.core.gateway

import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.model.ThemePreferences
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsStore {
    val preferences: StateFlow<AppPreferences>

    fun updateTheme(theme: ThemePreferences)

    fun updateLanguage(language: AppLanguage)

    fun updateAnimationSpeed(speed: AnimationSpeed)

    fun updateChatTextSize(size: ChatTextSize)

    fun updateAutoCollapseLongMessages(enabled: Boolean)

    fun updateActionTraceExpansion(
        expandRunning: Boolean,
        expandCompleted: Boolean,
    )

    fun updateNotifications(notifications: NotificationPreferences)

    fun markOnboardingCompleted()
}
