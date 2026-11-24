package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.theme.parseHexColorOrNull
import com.everfold.core.designsystem.theme.rememberMonetSwatchPalette
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.everfold.core.model.ThemePreferences
import com.everfold.feature.settings.R

private const val DYNAMIC_CARD_CORNER_DP = 20

@Composable
internal fun MonetDynamicCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DYNAMIC_CARD_CORNER_DP.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Wallpaper,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.appearance_monet_dynamic_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text =
                        stringResource(
                            if (enabled) {
                                R.string.appearance_monet_dynamic_subtitle
                            } else {
                                R.string.appearance_monet_dynamic_unsupported
                            },
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
internal fun ManualMonetSections(
    theme: ThemePreferences,
    isDark: Boolean,
    onThemeChange: (ThemePreferences) -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSeedColor = resolveActiveSeedColor(theme)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AccentColorCarousel(
            theme = theme,
            isDark = isDark,
            onSelectPreset = { preset ->
                onThemeChange(
                    theme.copy(
                        colorMode = ThemeColorMode.PRESET,
                        seedColor = preset,
                    ),
                )
            },
            onOpenCustomPicker = onOpenCustomPicker,
        )
        PaletteStyleCarousel(
            activeSeedColor = activeSeedColor,
            selected = theme.paletteStyle,
            colorSpec = theme.colorSpec,
            isDark = isDark,
            onSelect = { style -> onThemeChange(theme.copy(paletteStyle = style)) },
        )
        ColorSpecRow(
            selected = theme.colorSpec,
            selectedStyle = theme.paletteStyle,
            onSelect = { spec -> onThemeChange(theme.copy(colorSpec = spec)) },
        )
    }
}

@Composable
private fun AccentColorCarousel(
    theme: ThemePreferences,
    isDark: Boolean,
    onSelectPreset: (SeedColorPreset) -> Unit,
    onOpenCustomPicker: () -> Unit,
) {
    val isCustomMode = theme.colorMode == ThemeColorMode.CUSTOM
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_accent_source_label))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(SeedColorPreset.entries) { preset ->
                val palette =
                    rememberMonetSwatchPalette(
                        seedColor = preset.toColor(),
                        isDark = isDark,
                        paletteStyle = theme.paletteStyle,
                        colorSpec = theme.colorSpec,
                    )
                val selected = !isCustomMode && theme.seedColor == preset
                MonetOptionTile(
                    label = stringResource(preset.labelRes()),
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    containerColor = palette.surface,
                ) {
                    MonetTriToneDisc(palette = palette, selected = selected)
                }
            }
            item {
                CustomAccentTile(
                    theme = theme,
                    isCustomMode = isCustomMode,
                    isDark = isDark,
                    onClick = onOpenCustomPicker,
                )
            }
        }
    }
}

@Composable
private fun CustomAccentTile(
    theme: ThemePreferences,
    isCustomMode: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
) {
    val customSeed = parseHexColorOrNull(theme.customColorHex) ?: theme.seedColor.toColor()
    val palette =
        rememberMonetSwatchPalette(
            seedColor = customSeed,
            isDark = isDark,
            paletteStyle = theme.paletteStyle,
            colorSpec = theme.colorSpec,
        )
    MonetOptionTile(
        label = stringResource(R.string.theme_color_custom),
        selected = isCustomMode,
        onClick = onClick,
        containerColor =
            if (isCustomMode) {
                palette.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
    ) {
        if (isCustomMode) {
            MonetTriToneDisc(palette = palette, selected = true)
        } else {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteStyleCarousel(
    activeSeedColor: Color,
    selected: ThemePaletteStyle,
    colorSpec: ThemeColorSpec,
    isDark: Boolean,
    onSelect: (ThemePaletteStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_palette_style_label))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(ThemePaletteStyle.entries) { style ->
                val palette =
                    rememberMonetSwatchPalette(
                        seedColor = activeSeedColor,
                        isDark = isDark,
                        paletteStyle = style,
                        colorSpec = colorSpec,
                    )
                val isSelected = style == selected
                MonetOptionTile(
                    label = stringResource(style.labelRes()),
                    selected = isSelected,
                    onClick = { onSelect(style) },
                    containerColor = palette.surface,
                ) {
                    MonetTriToneDisc(palette = palette, selected = isSelected)
                }
            }
        }
        Text(
            text = stringResource(selected.descriptionRes()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColorSpecRow(
    selected: ThemeColorSpec,
    selectedStyle: ThemePaletteStyle,
    onSelect: (ThemeColorSpec) -> Unit,
) {
    val specs = ThemeColorSpec.entries
    val hintRes =
        when {
            selected == ThemeColorSpec.SPEC_2025 && !selectedStyle.supportsSpec2025 -> {
                R.string.appearance_color_spec_2025_fallback_hint
            }

            selected == ThemeColorSpec.SPEC_2025 -> {
                R.string.appearance_color_spec_2025_hint
            }

            else -> {
                R.string.appearance_color_spec_2021_hint
            }
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_color_spec_label))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            specs.forEachIndexed { index, spec ->
                SegmentedButton(
                    selected = selected == spec,
                    onClick = { onSelect(spec) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = specs.size),
                    label = { Text(stringResource(spec.labelRes())) },
                )
            }
        }
        Text(
            text = stringResource(hintRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolveActiveSeedColor(theme: ThemePreferences): Color =
    if (theme.colorMode == ThemeColorMode.CUSTOM) {
        parseHexColorOrNull(theme.customColorHex) ?: theme.seedColor.toColor()
    } else {
        theme.seedColor.toColor()
    }
