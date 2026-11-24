package com.everfold.feature.onboarding.step

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.core.ui.appearance.AccentColorCarousel
import com.everfold.core.ui.appearance.MonetDynamicCard
import com.everfold.core.ui.appearance.ThemeModeSection
import com.everfold.core.ui.language.LanguageEntryCard
import com.everfold.core.ui.language.LanguagePickerSheet
import com.everfold.feature.onboarding.R

@Composable
internal fun OnboardingQuickSettingsStep(
    preferences: AppPreferences,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageSheet by remember { mutableStateOf(false) }

    OnboardingQuickSettingsList(
        preferences = preferences,
        onOpenLanguagePicker = { showLanguageSheet = true },
        onThemeModeChange = onThemeModeChange,
        onColorChange = onColorChange,
        modifier = modifier,
    )

    if (showLanguageSheet) {
        LanguagePickerSheet(
            selected = preferences.language,
            onSelect = onLanguageChange,
            onDismiss = { showLanguageSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnboardingQuickSettingsList(
    preferences: AppPreferences,
    onOpenLanguagePicker: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorChange: (ThemeColorMode, SeedColorPreset?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = preferences.theme
    val isDark =
        when (theme.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isUsingSystemDynamic = supportsDynamicColor && theme.colorMode == ThemeColorMode.SYSTEM_DYNAMIC

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
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
        LanguageEntryCard(selected = preferences.language, onClick = onOpenLanguagePicker)
        ThemeModeSection(selected = theme.themeMode, onSelect = onThemeModeChange)
        MonetDynamicCard(
            checked = isUsingSystemDynamic,
            enabled = supportsDynamicColor,
            onCheckedChange = { enabled ->
                onColorChange(
                    if (enabled) ThemeColorMode.SYSTEM_DYNAMIC else ThemeColorMode.PRESET,
                    null,
                )
            },
        )
        AnimatedVisibility(visible = !isUsingSystemDynamic) {
            AccentColorCarousel(
                theme = theme,
                isDark = isDark,
                onSelectPreset = { preset -> onColorChange(ThemeColorMode.PRESET, preset) },
            )
        }
        Text(
            text = stringResource(R.string.onboarding_setup_later_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
