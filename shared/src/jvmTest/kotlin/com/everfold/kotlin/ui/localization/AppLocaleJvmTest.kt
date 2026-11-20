package com.everfold.kotlin.ui.localization

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLocaleJvmTest {
    @Test
    fun tracksSystemLocaleAndRestoresOverrideWhenCleared() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("en-US", applyJvmResourceLocale(null))
            assertEquals(Locale.US, Locale.getDefault())

            assertEquals("en-US", applyJvmResourceLocale("zh"))
            assertEquals("zh", Locale.getDefault().language)

            assertEquals("en-US", applyJvmResourceLocale(null))
            assertEquals(Locale.US, Locale.getDefault())

            Locale.setDefault(Locale.JAPAN)
            assertEquals("ja-JP", applyJvmResourceLocale(null))
            assertEquals(Locale.JAPAN, Locale.getDefault())
        } finally {
            Locale.setDefault(original)
            applyJvmResourceLocale(null)
        }
    }
}
