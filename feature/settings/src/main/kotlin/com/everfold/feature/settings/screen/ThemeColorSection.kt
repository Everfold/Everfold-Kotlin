package com.everfold.feature.settings.screen

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.parseHexColorOrNull
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeColorMode
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.everfold.core.model.ThemePreferences
import com.everfold.feature.settings.R
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_CHANNEL = 255
private const val LUMINANCE_THRESHOLD = 0.5f

@Composable
internal fun ThemeColorCard(
    theme: ThemePreferences,
    onThemeChange: (ThemePreferences) -> Unit,
    modifier: Modifier = Modifier,
) {
    val supportsDynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isUsingSystemDynamic = supportsDynamicColor && theme.colorMode == ThemeColorMode.SYSTEM_DYNAMIC

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MonetDynamicToggleRow(
                checked = isUsingSystemDynamic,
                enabled = supportsDynamicColor,
                onCheckedChange = { enabled ->
                    val nextMode =
                        if (enabled) {
                            ThemeColorMode.SYSTEM_DYNAMIC
                        } else {
                            ThemeColorMode.PRESET
                        }
                    onThemeChange(theme.copy(colorMode = nextMode))
                },
            )
            AnimatedVisibility(visible = !isUsingSystemDynamic) {
                ManualThemeControls(theme = theme, onThemeChange = onThemeChange)
            }
        }
    }
}

@Composable
private fun MonetDynamicToggleRow(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_monet_dynamic_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
            )
        }
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
}

@Composable
private fun ManualThemeControls(
    theme: ThemePreferences,
    onThemeChange: (ThemePreferences) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        AccentSourceSection(theme = theme, onThemeChange = onThemeChange)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PaletteStyleSection(
            selected = theme.paletteStyle,
            onSelect = { style -> onThemeChange(theme.copy(paletteStyle = style)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        ColorSpecSection(
            selected = theme.colorSpec,
            selectedStyle = theme.paletteStyle,
            onSelect = { spec -> onThemeChange(theme.copy(colorSpec = spec)) },
        )
    }
}

@Composable
private fun AccentSourceSection(
    theme: ThemePreferences,
    onThemeChange: (ThemePreferences) -> Unit,
) {
    val sourceOptions = listOf(ThemeColorMode.PRESET, ThemeColorMode.CUSTOM)
    val effectiveSource =
        if (theme.colorMode == ThemeColorMode.CUSTOM) {
            ThemeColorMode.CUSTOM
        } else {
            ThemeColorMode.PRESET
        }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.appearance_accent_source_label),
            style = MaterialTheme.typography.titleSmall,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            sourceOptions.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = effectiveSource == mode,
                    onClick = { onThemeChange(theme.copy(colorMode = mode)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = sourceOptions.size),
                    label = { Text(stringResource(mode.labelRes())) },
                )
            }
        }
        if (effectiveSource == ThemeColorMode.CUSTOM) {
            CustomColorPicker(
                hex = theme.customColorHex,
                fallbackColor = theme.seedColor.toColor(),
                onHexChange = { hex -> onThemeChange(theme.copy(customColorHex = hex)) },
            )
        } else {
            PresetColorSwatches(
                selected = theme.seedColor,
                onSelect = { preset ->
                    onThemeChange(
                        theme.copy(
                            colorMode = ThemeColorMode.PRESET,
                            seedColor = preset,
                        ),
                    )
                },
            )
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SeedColorPreset.entries.forEach { preset ->
            val isSelected = preset == selected
            val swatchColor = preset.toColor()
            val borderColor =
                if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
            val checkTint =
                if (swatchColor.luminance() > LUMINANCE_THRESHOLD) Color.Black else Color.White
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onSelect(preset) },
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = borderColor,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = checkTint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
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
        ChannelSlider(label = "R", value = red) { r -> onHexChange(formatHex(r, green, blue)) }
        ChannelSlider(label = "G", value = green) { g -> onHexChange(formatHex(red, g, blue)) }
        ChannelSlider(label = "B", value = blue) { b -> onHexChange(formatHex(red, green, b)) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteStyleSection(
    selected: ThemePaletteStyle,
    onSelect: (ThemePaletteStyle) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.appearance_palette_style_label),
            style = MaterialTheme.typography.titleSmall,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemePaletteStyle.entries.forEach { style ->
                FilterChip(
                    selected = selected == style,
                    onClick = { onSelect(style) },
                    label = { Text(stringResource(style.labelRes())) },
                )
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
private fun ColorSpecSection(
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
        Text(
            text = stringResource(R.string.appearance_color_spec_label),
            style = MaterialTheme.typography.titleSmall,
        )
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

private fun formatHex(
    red: Int,
    green: Int,
    blue: Int,
): String = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)
