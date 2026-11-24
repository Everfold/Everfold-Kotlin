package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.everfold.feature.settings.R

@StringRes
internal fun ThemeColorMode.labelRes(): Int =
    when (this) {
        ThemeColorMode.SYSTEM_DYNAMIC -> R.string.theme_color_system_dynamic
        ThemeColorMode.PRESET -> R.string.theme_color_preset
        ThemeColorMode.CUSTOM -> R.string.theme_color_custom
    }

@StringRes
internal fun ThemePaletteStyle.labelRes(): Int =
    when (this) {
        ThemePaletteStyle.TONAL_SPOT -> R.string.palette_style_tonal_spot
        ThemePaletteStyle.NEUTRAL -> R.string.palette_style_neutral
        ThemePaletteStyle.VIBRANT -> R.string.palette_style_vibrant
        ThemePaletteStyle.EXPRESSIVE -> R.string.palette_style_expressive
        ThemePaletteStyle.RAINBOW -> R.string.palette_style_rainbow
        ThemePaletteStyle.FRUIT_SALAD -> R.string.palette_style_fruit_salad
        ThemePaletteStyle.MONOCHROME -> R.string.palette_style_monochrome
        ThemePaletteStyle.FIDELITY -> R.string.palette_style_fidelity
        ThemePaletteStyle.CONTENT -> R.string.palette_style_content
    }

@StringRes
internal fun ThemePaletteStyle.descriptionRes(): Int =
    when (this) {
        ThemePaletteStyle.TONAL_SPOT -> R.string.palette_style_tonal_spot_desc
        ThemePaletteStyle.NEUTRAL -> R.string.palette_style_neutral_desc
        ThemePaletteStyle.VIBRANT -> R.string.palette_style_vibrant_desc
        ThemePaletteStyle.EXPRESSIVE -> R.string.palette_style_expressive_desc
        ThemePaletteStyle.RAINBOW -> R.string.palette_style_rainbow_desc
        ThemePaletteStyle.FRUIT_SALAD -> R.string.palette_style_fruit_salad_desc
        ThemePaletteStyle.MONOCHROME -> R.string.palette_style_monochrome_desc
        ThemePaletteStyle.FIDELITY -> R.string.palette_style_fidelity_desc
        ThemePaletteStyle.CONTENT -> R.string.palette_style_content_desc
    }

@StringRes
internal fun ThemeColorSpec.labelRes(): Int =
    when (this) {
        ThemeColorSpec.SPEC_2021 -> R.string.color_spec_2021
        ThemeColorSpec.SPEC_2025 -> R.string.color_spec_2025
    }
