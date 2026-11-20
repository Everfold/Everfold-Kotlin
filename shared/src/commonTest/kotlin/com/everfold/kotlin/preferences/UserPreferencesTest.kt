package com.everfold.kotlin.preferences

import androidx.compose.ui.graphics.Color
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.ProcessVisibility
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserPreferencesTest {
    @Test
    fun freshStateDefaultsToExpandWhileRunningAndCollapseAfterCompletion() {
        val state = UserPreferencesState()
        assertEquals(UserPreferences(), state.value)
        assertTrue(state.value.processVisibility.expandWhileRunning)
        assertFalse(state.value.processVisibility.expandAfterCompletion)
        assertFalse(state.value.onboardingCompleted)
    }

    @Test
    fun updateAppliesConversationAndProcessVisibilityChangesInMemory() {
        val state = UserPreferencesState()
        val expected =
            UserPreferences(
                language = AppLanguage.English,
                textSize = ConversationTextSize.Large,
                shortcut = SendShortcut.Enter,
                processVisibility = ProcessVisibility(expandWhileRunning = false, expandAfterCompletion = true),
                collapsePrompts = false,
                sideMargins = false,
                themeMode = ThemeMode.Dark,
                themeColor = ThemeColorSelection(ThemeColorMode.Custom, ThemePreset.Teal, Color(0xFF214365)),
                onboardingCompleted = true,
            )
        state.update { expected }
        assertEquals(expected, state.value)
    }

    @Test
    fun appearanceAndOnboardingHelpersMutateCurrentSessionStateOnly() {
        val state = UserPreferencesState()
        state.setTheme(ThemeMode.Dark)
        state.selectThemePreset(ThemePreset.Teal)
        state.applyCustomThemeColor(Color(0xFF123456))
        state.useSystemThemeColor()
        state.markOnboardingCompleted()

        assertEquals(ThemeMode.Dark, state.themeMode)
        assertEquals(ThemeColorMode.System, state.themeColor.mode)
        assertEquals(ThemePreset.Teal, state.themeColor.preset)
        assertEquals(Color(0xFF123456), state.themeColor.customSeed)
        assertTrue(state.value.onboardingCompleted)
        assertFalse(UserPreferencesState().value.onboardingCompleted)
    }
}
