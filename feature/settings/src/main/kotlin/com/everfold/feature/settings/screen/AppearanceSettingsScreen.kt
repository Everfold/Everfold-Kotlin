package com.everfold.feature.settings.screen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpec
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.core.model.ThemePreferences
import com.everfold.core.ui.appearance.MonetDynamicCard
import com.everfold.core.ui.appearance.ThemeModeSection
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val theme = prefs.theme
    val isDark = resolveIsDark(theme.themeMode)
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isUsingSystemDynamic = supportsDynamicColor && theme.colorMode == ThemeColorMode.SYSTEM_DYNAMIC
    var showCustomColorSheet by remember { mutableStateOf(false) }

    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_entry_appearance_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        AppearanceSettingsList(
            prefs = prefs,
            isDark = isDark,
            supportsDynamicColor = supportsDynamicColor,
            isUsingSystemDynamic = isUsingSystemDynamic,
            onThemeChange = viewModel::updateTheme,
            onAnimationSpeedChange = viewModel::setAnimationSpeed,
            onOpenCustomPicker = { showCustomColorSheet = true },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }

    if (showCustomColorSheet) {
        CustomColorBottomSheet(
            hex = theme.customColorHex,
            fallbackColor = theme.seedColor.toColor(),
            isDark = isDark,
            paletteStyle = theme.paletteStyle,
            colorSpec = theme.colorSpec,
            onHexChange = { hex ->
                viewModel.updateTheme(theme.copy(colorMode = ThemeColorMode.CUSTOM, customColorHex = hex))
            },
            onDismiss = { showCustomColorSheet = false },
        )
    }
}

@Composable
private fun AppearanceSettingsList(
    prefs: AppPreferences,
    isDark: Boolean,
    supportsDynamicColor: Boolean,
    isUsingSystemDynamic: Boolean,
    onThemeChange: (ThemePreferences) -> Unit,
    onAnimationSpeedChange: (AnimationSpeed) -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = prefs.theme
    val animationSpeed = LocalAnimationSpeed.current
    val expandSpec = animationSpeed.spatialSpec<IntSize>()
    val fadeSpec = animationSpeed.effectsSpec<Float>()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ThemePreviewCard() }
        item {
            ThemeModeSection(
                selected = theme.themeMode,
                onSelect = { mode -> onThemeChange(theme.copy(themeMode = mode)) },
            )
        }
        item {
            MonetDynamicCard(
                checked = isUsingSystemDynamic,
                enabled = supportsDynamicColor,
                onCheckedChange = { enabled ->
                    val nextMode =
                        if (enabled) ThemeColorMode.SYSTEM_DYNAMIC else ThemeColorMode.PRESET
                    onThemeChange(theme.copy(colorMode = nextMode))
                },
            )
        }
        item {
            AnimatedVisibility(
                visible = !isUsingSystemDynamic,
                enter = expandVertically(animationSpec = expandSpec) + fadeIn(animationSpec = fadeSpec),
                exit = shrinkVertically(animationSpec = expandSpec) + fadeOut(animationSpec = fadeSpec),
            ) {
                ManualMonetSections(
                    theme = theme,
                    isDark = isDark,
                    onThemeChange = onThemeChange,
                    onOpenCustomPicker = onOpenCustomPicker,
                )
            }
        }
        item {
            AnimationSpeedCard(
                speed = prefs.animationSpeed,
                onSpeedChange = onAnimationSpeedChange,
            )
        }
    }
}

@Composable
private fun resolveIsDark(mode: ThemeMode): Boolean =
    when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
