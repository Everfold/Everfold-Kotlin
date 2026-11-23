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
import com.everfold.core.model.ThemeMode
import com.materialkolor.rememberDynamicColorScheme

private const val COLOR_INDIGO = 0xFF3F51B5
private const val COLOR_TEAL = 0xFF00796B
private const val COLOR_TERRACOTTA = 0xFFBF5B3B
private const val COLOR_FOREST = 0xFF2E7D32
private const val COLOR_PLUM = 0xFF7B3FA0
private const val HEX_RGB_LENGTH = 6
private const val HEX_RADIX = 16
private const val OPAQUE_ALPHA_MASK = 0xFF000000

fun SeedColorPreset.toColor(): Color =
    when (this) {
        SeedColorPreset.INDIGO -> Color(COLOR_INDIGO)
        SeedColorPreset.TEAL -> Color(COLOR_TEAL)
        SeedColorPreset.TERRACOTTA -> Color(COLOR_TERRACOTTA)
        SeedColorPreset.FOREST -> Color(COLOR_FOREST)
        SeedColorPreset.PLUM -> Color(COLOR_PLUM)
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
    val isDark =
        when (preferences.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme =
        when {
            preferences.themeColorMode == ThemeColorMode.SYSTEM_DYNAMIC &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            preferences.themeColorMode == ThemeColorMode.CUSTOM -> {
                val seed =
                    parseHexColorOrNull(preferences.customColorHex)
                        ?: preferences.seedColor.toColor()
                rememberDynamicColorScheme(seedColor = seed, isDark = isDark)
            }

            else -> {
                rememberDynamicColorScheme(
                    seedColor = preferences.seedColor.toColor(),
                    isDark = isDark,
                )
            }
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
