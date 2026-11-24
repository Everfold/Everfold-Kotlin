package com.everfold.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.rememberDynamicColorScheme

data class MonetSwatchPalette(
    val top: Color,
    val bottomLeft: Color,
    val bottomRight: Color,
    val indicator: Color,
    val onIndicator: Color,
    val surface: Color,
)

fun computeMonetSwatchPalette(
    seedColor: Color,
    isDark: Boolean,
    paletteStyle: ThemePaletteStyle,
    colorSpec: ThemeColorSpec,
): MonetSwatchPalette {
    val scheme =
        dynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = paletteStyle.toMaterialKolorStyle(),
            specVersion = colorSpec.toColorSpecVersion(),
        )
    return MonetSwatchPalette(
        top = scheme.primary,
        bottomLeft = scheme.secondaryContainer,
        bottomRight = scheme.tertiary,
        indicator = scheme.primary,
        onIndicator = scheme.onPrimary,
        surface = scheme.surfaceContainerHigh,
    )
}

@Composable
fun rememberMonetSwatchPalette(
    seedColor: Color,
    isDark: Boolean,
    paletteStyle: ThemePaletteStyle,
    colorSpec: ThemeColorSpec,
): MonetSwatchPalette {
    val scheme =
        rememberDynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            style = paletteStyle.toMaterialKolorStyle(),
            specVersion = colorSpec.toColorSpecVersion(),
        )
    return MonetSwatchPalette(
        top = scheme.primary,
        bottomLeft = scheme.secondaryContainer,
        bottomRight = scheme.tertiary,
        indicator = scheme.primary,
        onIndicator = scheme.onPrimary,
        surface = scheme.surfaceContainerHigh,
    )
}
