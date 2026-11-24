package com.everfold.core.designsystem.theme

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.ui.graphics.Color
import com.everfold.core.designsystem.component.groupedItemShape
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test
    fun monetSwatchPaletteReflectsStyleAndColorSpec() {
        val seed = SeedColorPreset.BLUE.toColor()
        val monochrome =
            computeMonetSwatchPalette(
                seedColor = seed,
                isDark = false,
                paletteStyle = ThemePaletteStyle.MONOCHROME,
                colorSpec = ThemeColorSpec.SPEC_2021,
            )
        val vibrant =
            computeMonetSwatchPalette(
                seedColor = seed,
                isDark = false,
                paletteStyle = ThemePaletteStyle.VIBRANT,
                colorSpec = ThemeColorSpec.SPEC_2021,
            )
        val expressive2021 =
            computeMonetSwatchPalette(
                seedColor = seed,
                isDark = false,
                paletteStyle = ThemePaletteStyle.EXPRESSIVE,
                colorSpec = ThemeColorSpec.SPEC_2021,
            )
        val expressive2025 =
            computeMonetSwatchPalette(
                seedColor = seed,
                isDark = false,
                paletteStyle = ThemePaletteStyle.EXPRESSIVE,
                colorSpec = ThemeColorSpec.SPEC_2025,
            )

        assertNotEquals(monochrome, vibrant)
        assertNotEquals(expressive2021, expressive2025)
    }

    @Test
    fun groupedItemShapeRoundsOuterEdgesAndSoftensInnerEdges() {
        val single = groupedItemShape(index = 0, count = 1)
        assertEquals(single.topStart, single.bottomStart)

        val first = groupedItemShape(index = 0, count = 3)
        val middle = groupedItemShape(index = 1, count = 3)
        val last = groupedItemShape(index = 2, count = 3)

        assertEquals(first.topStart, last.bottomStart)
        assertEquals(first.bottomStart, middle.topStart)
        assertEquals(middle.bottomStart, last.topStart)
        assertNotEquals(first.topStart, first.bottomStart)
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Test
    fun animationSpeedMapsToExpressiveMotionSpecsAndSnapWhenOff() {
        assertTrue(AnimationSpeed.OFF.spatialSpecFor<Float>() is SnapSpec)
        assertTrue(AnimationSpeed.OFF.effectsSpecFor<Float>() is SnapSpec)
        assertTrue(AnimationSpeed.DEFAULT.spatialSpecFor<Float>() is SpringSpec)
        assertTrue(AnimationSpeed.FAST.spatialSpecFor<Float>() is SpringSpec)
        assertTrue(AnimationSpeed.SLOW.effectsSpecFor<Float>() is SpringSpec)
    }
}
