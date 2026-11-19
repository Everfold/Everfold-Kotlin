package com.everfold.kotlin.preferences

import androidx.compose.ui.graphics.Color
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
                onboardingCompleted = true,
            )
        original.update { expected }
        val restored = UserPreferencesState(store)
        assertEquals(expected, restored.value)
        restored.update { it.copy(sideMargins = true) }
        assertEquals(expected.copy(sideMargins = true), UserPreferencesState(store).value)
    }

    @Test
    fun appearancePreferencesAndOnboardingPersistDirectlyThroughUserPreferencesState() {
        val store = MemoryPreferenceStore()
        val state = UserPreferencesState(store)
        state.setTheme(ThemeMode.Dark)
        state.selectThemePreset(ThemePreset.Teal)
        state.applyCustomThemeColor(Color(0xFF123456))
        state.useSystemThemeColor()
        state.markOnboardingCompleted()

        val restored = UserPreferencesState(store)
        assertEquals(ThemeMode.Dark, restored.themeMode)
        assertEquals(ThemeColorMode.System, restored.themeColor.mode)
        assertEquals(ThemePreset.Teal, restored.themeColor.preset)
        assertEquals(Color(0xFF123456), restored.themeColor.customSeed)
        assertTrue(restored.value.onboardingCompleted)
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
        assertFalse(restored.onboardingCompleted)
    }

    @Test
    fun unknownOrUnsupportedSchemaVersionFallsBackToDefaults() {
        assertEquals(UserPreferences(), UserPreferences.decode("version=99\nsideMargins=false"))
        assertEquals(UserPreferences(), UserPreferences.decode("version=invalid\nsideMargins=false"))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun asyncPersistenceConflatesRapidUpdatesAndWritesOnIoDispatcher() {
        val writes = mutableListOf<String>()
        val backing = MemoryPreferenceStore()
        val store =
            object : PreferenceStore {
                override fun read(): String? = backing.read()

                override fun write(value: String) {
                    writes += value
                    backing.write(value)
                }
            }
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        try {
            val state = UserPreferencesState(store, persistScope = scope, ioDispatcher = dispatcher)
            state.setTheme(ThemeMode.Dark)
            state.selectThemePreset(ThemePreset.Blue)
            state.markOnboardingCompleted()
            assertNull(backing.read())
            assertTrue(writes.isEmpty())

            scope.runCurrent()
            assertEquals(1, writes.size)
            val restored = UserPreferencesState(backing)
            assertEquals(ThemeMode.Dark, restored.themeMode)
            assertEquals(ThemePreset.Blue, restored.themeColor.preset)
            assertTrue(restored.value.onboardingCompleted)
        } finally {
            scope.cancel()
        }
    }
}
