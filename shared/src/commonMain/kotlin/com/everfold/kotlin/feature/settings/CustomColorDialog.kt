package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.parseHexColor
import com.everfold.kotlin.ui.theme.toHexColor
import com.materialkolor.rememberDynamicColorScheme
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.apply_color
import everfold.shared.generated.resources.blue_channel
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.custom_color_title
import everfold.shared.generated.resources.dark_preview
import everfold.shared.generated.resources.green_channel
import everfold.shared.generated.resources.hex_color
import everfold.shared.generated.resources.hex_error
import everfold.shared.generated.resources.hex_help
import everfold.shared.generated.resources.light_preview
import everfold.shared.generated.resources.red_channel
import everfold.shared.generated.resources.start_chat
import kotlin.math.roundToInt

/** 自定义主题色HEX 输入与 RGB 滑块联动同时预览浅色和深色配色 */
@Composable
internal fun CustomColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit,
) {
    var hex by rememberSaveable { mutableStateOf(initialColor.toHexColor()) }
    // HEX 尚未输入完整时保留上一次有效预览避免颜色跳变
    var previewArgb by rememberSaveable { mutableStateOf(initialColor.toArgb()) }
    val parsed = parseHexColor(hex)
    val preview = Color(previewArgb)

    AlertDialog(
        onDismissRequest = onDismiss,
        // 自定义主题色
        title = { Text(localizedString(Res.string.custom_color_title)) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PalettePreview(preview, dark = false, Modifier.weight(1f))
                    PalettePreview(preview, dark = true, Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { value ->
                        hex = value
                        parseHexColor(value)?.let { previewArgb = it.toArgb() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    // HEX 颜色
                    label = { Text(localizedString(Res.string.hex_color)) },
                    placeholder = { Text("#673AB7") },
                    singleLine = true,
                    isError = parsed == null,
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Ascii,
                        ),
                    supportingText = {
                        Text(
                            if (parsed ==
                                null
                            ) {
                                // 请输入 6 位十六进制颜色如 #673AB7
                                localizedString(Res.string.hex_error)
                            } else {
                                // 支持 RRGGBB 或 #RRGGBB
                                localizedString(Res.string.hex_help)
                            },
                        )
                    },
                )
                listOf(
                    // 红色 R
                    localizedString(Res.string.red_channel) to 16,
                    // 绿色 G
                    localizedString(Res.string.green_channel) to 8,
                    // 蓝色 B
                    localizedString(Res.string.blue_channel) to 0,
                ).forEach { (label, shift) ->
                    val channel = (previewArgb ushr shift) and 0xFF
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(channel.toString(), style = MaterialTheme.typography.labelMedium)
                        }
                        Slider(
                            value = channel.toFloat(),
                            valueRange = 0f..255f,
                            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
                            onValueChange = { value ->
                                previewArgb = (previewArgb and (0xFF shl shift).inv()) or (value.roundToInt() shl shift)
                                hex = Color(previewArgb).toHexColor()
                            },
                        )
                    }
                }
            }
        },
        confirmButton = {
            // 应用颜色
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onApply) }) { Text(localizedString(Res.string.apply_color)) }
        },
        // 取消
        dismissButton = { TextButton(onClick = onDismiss) { Text(localizedString(Res.string.cancel)) } },
    )
}

@Composable
private fun PalettePreview(
    seed: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = rememberDynamicColorScheme(seedColor = seed, isDark = dark)
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = colors.surfaceContainer) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    // 深色预览 / 浅色预览
                    if (dark) localizedString(Res.string.dark_preview) else localizedString(Res.string.light_preview),
                    style = MaterialTheme.typography.labelMedium,
                )
                Surface(shape = RoundedCornerShape(12.dp), color = colors.primary, contentColor = colors.onPrimary) {
                    Text(
                        // 开始对话
                        localizedString(Res.string.start_chat),
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer).forEach { color ->
                        Surface(Modifier.weight(1f).height(8.dp), color = color, shape = RoundedCornerShape(4.dp)) {}
                    }
                }
            }
        }
    }
}
