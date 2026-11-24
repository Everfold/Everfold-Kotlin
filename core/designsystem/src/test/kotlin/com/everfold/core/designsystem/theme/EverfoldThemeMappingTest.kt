package com.everfold.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val M3_BASELINE_HEX = "#6750A4"
private const val M3_BASELINE_ARGB = 0xFF6750A4

class EverfoldThemeMappingTest {
    @Test
    fun seedPresetsMapToDistinctOpaqueColorsStartingWithBaseline() {
        assertEquals(Color(M3_BASELINE_ARGB), SeedColorPreset.BASELINE.toColor())
        val colors = SeedColorPreset.entries.map { it.toColor() }
        assertEquals(8, colors.size)
        assertEquals(colors.size, colors.toSet().size)
        assertTrue(colors.all { it.alpha == 1f })
    }

    @Test
    fun allNinePaletteStylesMapToDistinctMaterialKolorStyles() {
        val mapped = ThemePaletteStyle.entries.map { it.toMaterialKolorStyle() }
        assertEquals(PaletteStyle.entries.size, mapped.size)
        assertEquals(PaletteStyle.entries.size, mapped.toSet().size)
    }

    @Test
    fun colorSpecSupportMatchesMaterialColorUtilitiesSpec2025() {
        assertEquals(ColorSpec.SpecVersion.SPEC_2021, ThemeColorSpec.SPEC_2021.toColorSpecVersion())
        assertEquals(ColorSpec.SpecVersion.SPEC_2025, ThemeColorSpec.SPEC_2025.toColorSpecVersion())

        assertTrue(ThemePaletteStyle.TONAL_SPOT.supportsSpec2025)
        assertTrue(ThemePaletteStyle.NEUTRAL.supportsSpec2025)
        assertTrue(ThemePaletteStyle.VIBRANT.supportsSpec2025)
        assertTrue(ThemePaletteStyle.EXPRESSIVE.supportsSpec2025)

        assertFalse(ThemePaletteStyle.RAINBOW.supportsSpec2025)
        assertFalse(ThemePaletteStyle.FRUIT_SALAD.supportsSpec2025)
        assertFalse(ThemePaletteStyle.MONOCHROME.supportsSpec2025)
        assertFalse(ThemePaletteStyle.FIDELITY.supportsSpec2025)
        assertFalse(ThemePaletteStyle.CONTENT.supportsSpec2025)
    }

    @Test
    fun parseHexColorHandlesValidAndInvalidInputs() {
        assertEquals(Color(M3_BASELINE_ARGB), parseHexColorOrNull(M3_BASELINE_HEX))
        assertEquals(Color(M3_BASELINE_ARGB), parseHexColorOrNull(" 6750a4 "))
        assertNull(parseHexColorOrNull("#12345"))
        assertNull(parseHexColorOrNull("ZZZZZZ"))
    }
}
