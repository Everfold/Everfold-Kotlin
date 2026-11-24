package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.MonetTriToneDisc
import com.everfold.core.designsystem.theme.parseHexColorOrNull
import com.everfold.core.designsystem.theme.rememberMonetSwatchPalette
import com.everfold.core.model.ThemeColorSpec
import com.everfold.core.model.ThemePaletteStyle
import com.everfold.feature.settings.R
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_CHANNEL = 255
private const val COLOR_RED_TRACK = 0xFFD32F2F
private const val COLOR_GREEN_TRACK = 0xFF388E3C
private const val COLOR_BLUE_TRACK = 0xFF1976D2
private const val CHANNEL_LABEL_WIDTH_DP = 44

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomColorBottomSheet(
    hex: String,
    fallbackColor: Color,
    isDark: Boolean,
    paletteStyle: ThemePaletteStyle,
    colorSpec: ThemeColorSpec,
    onHexChange: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = parseHexColorOrNull(hex) ?: fallbackColor
    val palette =
        rememberMonetSwatchPalette(
            seedColor = parsed,
            isDark = isDark,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
    val red = (parsed.red * MAX_CHANNEL).roundToInt()
    val green = (parsed.green * MAX_CHANNEL).roundToInt()
    val blue = (parsed.blue * MAX_CHANNEL).roundToInt()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHeaderRow(onDismiss = onDismiss)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MonetTriToneDisc(palette = palette, selected = true)
                OutlinedTextField(
                    value = hex,
                    onValueChange = onHexChange,
                    label = { Text(stringResource(R.string.appearance_custom_hex_label)) },
                    singleLine = true,
                    isError = parseHexColorOrNull(hex) == null,
                    modifier = Modifier.weight(1f),
                )
            }
            ChannelSlider(label = "R", value = red, accentColor = Color(COLOR_RED_TRACK)) { r ->
                onHexChange(formatHex(r, green, blue))
            }
            ChannelSlider(label = "G", value = green, accentColor = Color(COLOR_GREEN_TRACK)) { g ->
                onHexChange(formatHex(red, g, blue))
            }
            ChannelSlider(label = "B", value = blue, accentColor = Color(COLOR_BLUE_TRACK)) { b ->
                onHexChange(formatHex(red, green, b))
            }
        }
    }
}

@Composable
private fun SheetHeaderRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.appearance_custom_color_sheet_title),
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onDismiss) {
            Text(text = stringResource(R.string.appearance_custom_color_done))
        }
    }
}

@Composable
private fun ChannelSlider(
    label: String,
    value: Int,
    accentColor: Color,
    onValueChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "$label $value",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(CHANNEL_LABEL_WIDTH_DP.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..MAX_CHANNEL.toFloat(),
            colors =
                SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

private fun formatHex(
    red: Int,
    green: Int,
    blue: Int,
): String = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)
