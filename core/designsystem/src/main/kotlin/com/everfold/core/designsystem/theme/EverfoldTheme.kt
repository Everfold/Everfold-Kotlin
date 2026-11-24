package com.everfold.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemeMode
import com.everfold.core.model.ThemePaletteStyle
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

private const val COLOR_BASELINE = 0xFF6750A4
private const val COLOR_BLUE = 0xFF1A73E8
private const val COLOR_TEAL = 0xFF00796B
private const val COLOR_GREEN = 0xFF2E7D32
private const val COLOR_AMBER = 0xFFD97706
private const val COLOR_ORANGE = 0xFFBF5B3B
private const val COLOR_ROSE = 0xFFC2185B
private const val COLOR_VIOLET = 0xFF7B3FA0
private const val HEX_RGB_LENGTH = 6
private const val HEX_RADIX = 16
private const val OPAQUE_ALPHA_MASK = 0xFF000000

fun SeedColorPreset.toColor(): Color =
    when (this) {
        SeedColorPreset.BASELINE -> Color(COLOR_BASELINE)
        SeedColorPreset.BLUE -> Color(COLOR_BLUE)
        SeedColorPreset.TEAL -> Color(COLOR_TEAL)
        SeedColorPreset.GREEN -> Color(COLOR_GREEN)
        SeedColorPreset.AMBER -> Color(COLOR_AMBER)
        SeedColorPreset.ORANGE -> Color(COLOR_ORANGE)
        SeedColorPreset.ROSE -> Color(COLOR_ROSE)
        SeedColorPreset.VIOLET -> Color(COLOR_VIOLET)
    }

fun ThemePaletteStyle.toMaterialKolorStyle(): PaletteStyle =
    when (this) {
        ThemePaletteStyle.TONAL_SPOT -> PaletteStyle.TonalSpot
        ThemePaletteStyle.NEUTRAL -> PaletteStyle.Neutral
        ThemePaletteStyle.VIBRANT -> PaletteStyle.Vibrant
        ThemePaletteStyle.EXPRESSIVE -> PaletteStyle.Expressive
        ThemePaletteStyle.RAINBOW -> PaletteStyle.Rainbow
        ThemePaletteStyle.FRUIT_SALAD -> PaletteStyle.FruitSalad
        ThemePaletteStyle.MONOCHROME -> PaletteStyle.Monochrome
        ThemePaletteStyle.FIDELITY -> PaletteStyle.Fidelity
        ThemePaletteStyle.CONTENT -> PaletteStyle.Content
    }

fun ThemeColorSpec.toColorSpecVersion(): ColorSpec.SpecVersion =
    when (this) {
        ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
        ThemeColorSpec.SPEC_2025 -> ColorSpec.SpecVersion.SPEC_2025
    }

fun parseHexColorOrNull(hex: String): Color? =
    hex
        .trim()
        .removePrefix("#")
        .takeIf { it.length == HEX_RGB_LENGTH }
        ?.toLongOrNull(HEX_RADIX)
        ?.let { Color(OPAQUE_ALPHA_MASK or it) }

@Composable
fun EverfoldTheme(
    preferences: AppPreferences = AppPreferences(),
    content: @Composable () -> Unit,
) {
    val theme = preferences.theme
    val isDark =
        when (theme.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme =
        if (theme.colorMode == ThemeColorMode.SYSTEM_DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            val seed =
                if (theme.colorMode == ThemeColorMode.CUSTOM) {
                    parseHexColorOrNull(theme.customColorHex) ?: theme.seedColor.toColor()
                } else {
                    theme.seedColor.toColor()
                }
            rememberDynamicColorScheme(
                seedColor = seed,
                isDark = isDark,
                style = theme.paletteStyle.toMaterialKolorStyle(),
                specVersion = theme.colorSpec.toColorSpecVersion(),
            )
        }

    ProvideAppLocale(language = preferences.language) {
        CompositionLocalProvider(LocalAnimationSpeed provides preferences.animationSpeed) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = Typography(),
                content = content,
            )
        }
    }
}
