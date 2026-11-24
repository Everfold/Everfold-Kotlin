package com.everfold.feature.onboarding.step

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.designsystem.component.MonetTriToneDisc
import com.everfold.core.designsystem.theme.rememberMonetSwatchPalette
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.feature.onboarding.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OnboardingQuickSettingsStep(
    preferences: AppPreferences,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark =
        when (preferences.theme.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_setup_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
            text = stringResource(R.string.onboarding_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EverfoldGroupedList {
            EverfoldGroupedItem(index = 0, count = 3) {
                LanguageSelectionCard(selected = preferences.language, onSelect = onLanguageChange)
            }
            EverfoldGroupedItem(index = 1, count = 3) {
                ThemeModeCard(selected = preferences.theme.themeMode, onSelect = onThemeModeChange)
            }
            EverfoldGroupedItem(index = 2, count = 3) {
                ColorSelectionCard(
                    preferences = preferences,
                    isDark = isDark,
                    onColorChange = onColorChange,
                )
            }
        }
        Text(
            text = stringResource(R.string.onboarding_setup_later_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageSelectionCard(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_setup_language), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selected == language,
                    onClick = { onSelect(language) },
                    label = { Text(stringResource(language.labelRes())) },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeCard(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_setup_theme), style = MaterialTheme.typography.titleSmall)
        EverfoldConnectedButtonGroup(
            options = ThemeMode.entries,
            selected = selected,
            onSelect = onSelect,
            label = { mode -> stringResource(mode.labelRes()) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSelectionCard(
    preferences: AppPreferences,
    isDark: Boolean,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
) {
    val theme = preferences.theme
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.onboarding_setup_color), style = MaterialTheme.typography.titleSmall)
        FilterChip(
            selected = theme.colorMode == ThemeColorMode.SYSTEM_DYNAMIC,
            onClick = { onColorChange(ThemeColorMode.SYSTEM_DYNAMIC, null) },
            label = { Text(stringResource(R.string.onboarding_setup_color_dynamic)) },
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SeedColorPreset.entries.forEach { preset ->
                val isSelected =
                    theme.colorMode == ThemeColorMode.PRESET && theme.seedColor == preset
                val palette =
                    rememberMonetSwatchPalette(
                        seedColor = preset.toColor(),
                        isDark = isDark,
                        paletteStyle = theme.paletteStyle,
                        colorSpec = theme.colorSpec,
                    )
                Surface(
                    shape = CircleShape,
                    color = palette.surface,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .clickable { onColorChange(ThemeColorMode.PRESET, preset) },
                ) {
                    Box(modifier = Modifier.padding(2.dp)) {
                        MonetTriToneDisc(
                            palette = palette,
                            selected = isSelected,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }
        }
    }
}

@StringRes
private fun AppLanguage.labelRes(): Int =
    when (this) {
        AppLanguage.SYSTEM -> R.string.onboarding_language_system
        AppLanguage.SIMPLIFIED_CHINESE -> R.string.onboarding_language_zh_hans
        AppLanguage.TRADITIONAL_CHINESE -> R.string.onboarding_language_zh_hant
        AppLanguage.ENGLISH -> R.string.onboarding_language_en
    }

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.onboarding_theme_system
        ThemeMode.LIGHT -> R.string.onboarding_theme_light
        ThemeMode.DARK -> R.string.onboarding_theme_dark
    }
