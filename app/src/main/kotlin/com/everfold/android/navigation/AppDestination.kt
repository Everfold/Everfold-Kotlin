package com.everfold.android.navigation

import androidx.navigation3.runtime.NavKey
import com.everfold.feature.settings.SettingsSection
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface AppDestination : NavKey {
    @Serializable
    data object Session : AppDestination

    @Serializable
    data object SessionSearch : AppDestination

    @Serializable
    data object Nodes : AppDestination

    @Serializable
    data object Settings : AppDestination

    @Serializable
    data object SettingsAppearance : AppDestination

    @Serializable
    data object SettingsLanguage : AppDestination

    @Serializable
    data object SettingsSessions : AppDestination

    @Serializable
    data object SettingsPermissions : AppDestination

    @Serializable
    data object SettingsAbout : AppDestination
}

internal fun SettingsSection.toDestination(): AppDestination =
    when (this) {
        SettingsSection.APPEARANCE -> AppDestination.SettingsAppearance
        SettingsSection.LANGUAGE -> AppDestination.SettingsLanguage
        SettingsSection.SESSIONS -> AppDestination.SettingsSessions
        SettingsSection.PERMISSIONS -> AppDestination.SettingsPermissions
        SettingsSection.ABOUT -> AppDestination.SettingsAbout
    }
