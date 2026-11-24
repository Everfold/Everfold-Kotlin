package com.everfold.core.ui.appearance

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.component.MonetTriToneDisc
import com.everfold.core.designsystem.theme.rememberMonetSwatchPalette
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import com.everfold.core.model.ThemePreferences
import com.everfold.core.ui.R

private const val TILE_WIDTH_DP = 76
private const val TILE_CORNER_DP = 18
private const val UNSELECTED_BORDER_ALPHA = 0.45f

@Composable
fun ThemeModeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_theme_mode_label))
        EverfoldConnectedButtonGroup(
            options = ThemeMode.entries,
            selected = selected,
            onSelect = onSelect,
            label = { mode -> stringResource(mode.labelRes()) },
            icon = { mode ->
                Icon(
                    imageVector = mode.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
    }
}

@Composable
fun MonetDynamicCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    EverfoldGroupedItem(
        index = 0,
        count = 1,
        modifier = modifier,
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
fun MonetOptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    val border =
        if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = UNSELECTED_BORDER_ALPHA),
            )
        }
    Surface(
        onClick = onClick,
        modifier = modifier.width(TILE_WIDTH_DP.dp),
        shape = RoundedCornerShape(TILE_CORNER_DP.dp),
        color = containerColor,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun AccentColorCarousel(
    theme: ThemePreferences,
    isDark: Boolean,
    onSelectPreset: (SeedColorPreset) -> Unit,
    modifier: Modifier = Modifier,
    trailingItem: (@Composable () -> Unit)? = null,
) {
    val isCustomMode = theme.colorMode == ThemeColorMode.CUSTOM
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
            if (trailingItem != null) {
                item { trailingItem() }
            }
        }
    }
}

private fun ThemeMode.icon(): ImageVector =
    when (this) {
        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
        ThemeMode.LIGHT -> Icons.Rounded.LightMode
        ThemeMode.DARK -> Icons.Rounded.DarkMode
    }

@StringRes
fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }

@StringRes
fun SeedColorPreset.labelRes(): Int =
    when (this) {
        SeedColorPreset.BASELINE -> R.string.seed_baseline
        SeedColorPreset.BLUE -> R.string.seed_blue
        SeedColorPreset.TEAL -> R.string.seed_teal
        SeedColorPreset.GREEN -> R.string.seed_green
        SeedColorPreset.AMBER -> R.string.seed_amber
        SeedColorPreset.ORANGE -> R.string.seed_orange
        SeedColorPreset.ROSE -> R.string.seed_rose
        SeedColorPreset.VIOLET -> R.string.seed_violet
    }
