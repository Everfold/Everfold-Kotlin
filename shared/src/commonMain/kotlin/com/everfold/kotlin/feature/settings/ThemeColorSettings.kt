package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemePreset
import com.everfold.kotlin.ui.theme.toHexColor
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.adjust
import everfold.shared.generated.resources.custom_color
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.theme_color
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 主题色选择系统配色预设种子色与自定义颜色共用选择状态 */
@Composable
internal fun ThemeColorSettings(
    selection: ThemeColorSelection,
    onSystemColorChange: () -> Unit,
    onPresetChange: (ThemePreset) -> Unit,
    onCustomColorChange: (Color) -> Unit,
) {
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.theme_color), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().selectableGroup()) {
                RadioButtonPreference(
                    title = localizedString(Res.string.follow_system),
                    selected = selection.mode == ThemeColorMode.System,
                    onClick = onSystemColorChange,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ThemePresetGrid(selection, onPresetChange)
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ArrowPreference(
                    title = localizedString(Res.string.custom_color),
                    summary = selection.customSeed.toHexColor(),
                    startAction = { ColorSwatch(selection.customSeed, selection.mode == ThemeColorMode.Custom) },
                    endActions = {
                        Text(
                            text = localizedString(Res.string.adjust),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    onClick = { showCustomDialog = true },
                )
            }
        }
    }
    if (showCustomDialog) {
        CustomColorDialog(
            initialColor = selection.customSeed,
            onDismiss = { showCustomDialog = false },
            onApply = {
                onCustomColorChange(it)
                showCustomDialog = false
            },
        )
    }
}

@Composable
private fun ThemePresetGrid(
    selection: ThemeColorSelection,
    onPresetChange: (ThemePreset) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemePreset.entries.chunked(3).forEach { presets ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val selected = selection.mode == ThemeColorMode.Preset && selection.preset == preset
                    val container = if (selected) MiuixTheme.colorScheme.secondaryContainer else Color.Transparent
                    Column(
                        Modifier
                            .weight(1f)
                            .squircleSurface(color = container, cornerRadius = 16.dp)
                            .selectable(selected, role = Role.RadioButton, onClick = { onPresetChange(preset) })
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ColorSwatch(preset.seed, selected)
                        Text(
                            text = localizedString(preset.label),
                            style = MiuixTheme.textStyles.footnote1,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
) {
    val ring = if (selected) MiuixTheme.colorScheme.primary else Color.Transparent
    Box(
        Modifier
            .size(44.dp)
            .squircleBorder(width = 2.dp, color = ring, cornerRadius = 14.dp)
            .padding(4.dp)
            .squircleClip(10.dp)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            AppIcon(
                AppSymbol.Check,
                modifier = Modifier.size(16.dp),
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}
