package com.everfold.feature.settings

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.parseHexColorOrNull
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_CHANNEL = 255

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceCard(
    prefs: AppPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onThemeColorModeChange: (ThemeColorMode) -> Unit,
    onSeedColorChange: (SeedColorPreset) -> Unit,
    onCustomColorHexChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_theme_mode_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(stringResource(mode.labelRes())) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.appearance_theme_color_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeColorMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.themeColorMode == mode,
                        onClick = { onThemeColorModeChange(mode) },
                        label = { Text(stringResource(mode.labelRes())) },
                    )
                }
            }

            when (prefs.themeColorMode) {
                ThemeColorMode.SYSTEM_DYNAMIC -> {
                    Text(
                        text = stringResource(R.string.appearance_dynamic_color_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ThemeColorMode.PRESET -> {
                    PresetColorSwatches(
                        selected = prefs.seedColor,
                        onSelect = onSeedColorChange,
                    )
                }

                ThemeColorMode.CUSTOM -> {
                    CustomColorPicker(
                        hex = prefs.customColorHex,
                        fallbackColor = prefs.seedColor.toColor(),
                        onHexChange = onCustomColorHexChange,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetColorSwatches(
    selected: SeedColorPreset,
    onSelect: (SeedColorPreset) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SeedColorPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect(preset) },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(preset.toColor())
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                )
                Text(
                    text = stringResource(preset.labelRes()),
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
    }
}

@Composable
private fun CustomColorPicker(
    hex: String,
    fallbackColor: Color,
    onHexChange: (String) -> Unit,
) {
    val parsed = parseHexColorOrNull(hex) ?: fallbackColor
    val red = (parsed.red * MAX_CHANNEL).roundToInt()
    val green = (parsed.green * MAX_CHANNEL).roundToInt()
    val blue = (parsed.blue * MAX_CHANNEL).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parsed)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            OutlinedTextField(
                value = hex,
                onValueChange = onHexChange,
                label = { Text(stringResource(R.string.appearance_custom_hex_label)) },
                singleLine = true,
                isError = parseHexColorOrNull(hex) == null,
                modifier = Modifier.weight(1f),
            )
        }
        ChannelSlider(label = "R", value = red) { r ->
            onHexChange(formatHex(r, green, blue))
        }
        ChannelSlider(label = "G", value = green) { g ->
            onHexChange(formatHex(red, g, blue))
        }
        ChannelSlider(label = "B", value = blue) { b ->
            onHexChange(formatHex(red, green, b))
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "$label $value", style = MaterialTheme.typography.labelSmall)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..MAX_CHANNEL.toFloat(),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatHex(
    red: Int,
    green: Int,
    blue: Int,
): String = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)

@StringRes
internal fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.SYSTEM -> R.string.theme_mode_system
        ThemeMode.LIGHT -> R.string.theme_mode_light
        ThemeMode.DARK -> R.string.theme_mode_dark
    }

@StringRes
internal fun ThemeColorMode.labelRes(): Int =
    when (this) {
        ThemeColorMode.SYSTEM_DYNAMIC -> R.string.theme_color_system_dynamic
        ThemeColorMode.PRESET -> R.string.theme_color_preset
        ThemeColorMode.CUSTOM -> R.string.theme_color_custom
    }

@StringRes
private fun SeedColorPreset.labelRes(): Int =
    when (this) {
        SeedColorPreset.INDIGO -> R.string.seed_indigo
        SeedColorPreset.TEAL -> R.string.seed_teal
        SeedColorPreset.TERRACOTTA -> R.string.seed_terracotta
        SeedColorPreset.FOREST -> R.string.seed_forest
        SeedColorPreset.PLUM -> R.string.seed_plum
    }
