package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.parseHexColor
import com.everfold.kotlin.ui.theme.toHexColor
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.apply_color
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.custom_color_title
import everfold.shared.generated.resources.dark_preview
import everfold.shared.generated.resources.hex_color
import everfold.shared.generated.resources.hex_error
import everfold.shared.generated.resources.hex_help
import everfold.shared.generated.resources.light_preview
import everfold.shared.generated.resources.start_chat
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 自定义主题色：Miuix ColorPicker 与 HEX 输入联动，并实时预览 Monet 浅色/深色配色 */
@Composable
internal fun CustomColorDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onApply: (Color) -> Unit,
) {
    var hex by rememberSaveable { mutableStateOf(initialColor.toHexColor()) }
    var previewArgb by rememberSaveable { mutableStateOf(initialColor.toArgb()) }
    val parsed = parseHexColor(hex)
    val preview = Color(previewArgb)
    GlassDialog(
        show = true,
        onDismissRequest = onDismiss,
        backdrop = LocalAppBackdrop.current,
        title = localizedString(Res.string.custom_color_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PalettePreview(preview, dark = false, Modifier.weight(1f))
                    PalettePreview(preview, dark = true, Modifier.weight(1f))
                }
                ColorPicker(
                    color = preview,
                    onColorChanged = { color ->
                        previewArgb = color.toArgb()
                        hex = color.toHexColor()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    showPreview = false,
                )
                CustomColorHexField(
                    hex = hex,
                    isError = parsed == null,
                    onValueChange = { value ->
                        hex = value
                        parseHexColor(value)?.let { previewArgb = it.toArgb() }
                    },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassDialogButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(text = localizedString(Res.string.cancel), style = MiuixTheme.textStyles.button)
                }
                GlassDialogButton(
                    onClick = { parsed?.let(onApply) },
                    modifier = Modifier.weight(1f),
                    enabled = parsed != null,
                    role = GlassDialogButtonRole.Primary,
                ) {
                    Text(text = localizedString(Res.string.apply_color), style = MiuixTheme.textStyles.button)
                }
            }
        }
    }
}

@Composable
private fun CustomColorHexField(
    hex: String,
    isError: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextField(
            value = hex,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = localizedString(Res.string.hex_color),
            useLabelAsPlaceholder = false,
            singleLine = true,
            colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    borderColor = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                ),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            keyboardOptions =
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                ),
        )
        Text(
            text = localizedString(if (isError) Res.string.hex_error else Res.string.hex_help),
            style = MiuixTheme.textStyles.footnote1,
            color = if (isError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@Composable
private fun PalettePreview(
    seed: Color,
    dark: Boolean,
    modifier: Modifier = Modifier,
) {
    val controller =
        remember(seed, dark) {
            ThemeController(
                colorSchemeMode = if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
                keyColor = seed,
            )
        }
    MiuixTheme(controller = controller) {
        val colors = MiuixTheme.colorScheme
        Card(
            modifier = modifier,
            insideMargin = PaddingValues(12.dp),
            colors = CardDefaults.defaultColors(color = colors.surfaceContainer),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = localizedString(if (dark) Res.string.dark_preview else Res.string.light_preview),
                    style = MiuixTheme.textStyles.footnote1,
                    color = colors.onSurfaceContainer,
                )
                Surface(
                    modifier = Modifier.squircleSurface(colors.primary, 10.dp),
                    color = Color.Transparent,
                    contentColor = colors.onPrimary,
                ) {
                    Text(
                        text = localizedString(Res.string.start_chat),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MiuixTheme.textStyles.footnote1,
                        fontWeight = FontWeight.Medium,
                        color = colors.onPrimary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(colors.primaryContainer, colors.secondaryContainer, colors.secondaryVariant).forEach { tone ->
                        Surface(
                            modifier = Modifier.weight(1f).height(8.dp).squircleSurface(tone, 4.dp),
                            color = Color.Transparent,
                        ) {}
                    }
                }
            }
        }
    }
}
