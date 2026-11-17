package com.everfold.kotlin.preferences

import androidx.compose.ui.graphics.Color
import com.everfold.kotlin.feature.settings.ConversationTextSize
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
    fun changedPreferencesSurviveCreatingANewState() {
        val store = MemoryPreferenceStore()
        val original = UserPreferencesState(store)
        val expected =
            UserPreferences(
                language = AppLanguage.English,
                textSize = ConversationTextSize.Large,
                shortcut = SendShortcut.Enter,
                collapsePrompts = false,
                sideMargins = false,
                themeMode = ThemeMode.Dark,
                themeColor = ThemeColorSelection(ThemeColorMode.Custom, ThemePreset.Teal, Color(0xFF214365)),
            )
        original.update { expected }
        val restored = UserPreferencesState(store)
        assertEquals(expected, restored.value)
        restored.update { it.copy(sideMargins = true) }
        assertEquals(expected.copy(sideMargins = true), UserPreferencesState(store).value)
    }

    @Test
    fun missingOrUnknownValuesUseDefaultsWithoutDiscardingValidPreferences() {
        assertEquals(UserPreferences(), UserPreferences.decode(null))
        val restored =
            UserPreferences.decode(
                "textSize=Unknown\nshortcut=Unknown\nsideMargins=false\ncollapsePrompts=garbled\ncustomColor=invalid\nthemeMode=Dark\nbroken",
            )
        assertEquals(ConversationTextSize.System, restored.textSize)
        assertEquals(SendShortcut.ModifiedEnter, restored.shortcut)
        assertFalse(restored.sideMargins)
        assertTrue(restored.collapsePrompts)
        assertEquals(ThemeMode.Dark, restored.themeMode)
        assertEquals(ThemePreset.Purple.seed, restored.themeColor.customSeed)
    }
}
