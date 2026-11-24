package com.everfold.feature.onboarding.step

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.feature.onboarding.R

@Composable
internal fun OnboardingQuickSettingsStep(
    preferences: AppPreferences,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(R.string.onboarding_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LanguageSelectionCard(selected = preferences.language, onSelect = onLanguageChange)
        ThemeModeCard(selected = preferences.themeMode, onSelect = onThemeModeChange)
        ColorSelectionCard(preferences = preferences, onColorChange = onColorChange)
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
    Card(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun ThemeModeCard(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_setup_theme), style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                        label = { Text(stringResource(mode.labelRes())) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSelectionCard(
    preferences: AppPreferences,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = stringResource(R.string.onboarding_setup_color), style = MaterialTheme.typography.titleSmall)
            FilterChip(
                selected = preferences.themeColorMode == ThemeColorMode.SYSTEM_DYNAMIC,
                onClick = { onColorChange(ThemeColorMode.SYSTEM_DYNAMIC, null) },
                label = { Text(stringResource(R.string.onboarding_setup_color_dynamic)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeedColorPreset.entries.forEach { preset ->
                    val isSelected =
                        preferences.themeColorMode == ThemeColorMode.PRESET &&
                            preferences.seedColor == preset
                    val borderColor =
                        if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(preset.toColor())
                                .border(width = if (isSelected) 3.dp else 1.dp, color = borderColor, shape = CircleShape)
                                .clickable { onColorChange(ThemeColorMode.PRESET, preset) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
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
