package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.collapse_long_prompts
import everfold.shared.generated.resources.conversation_side_margins
import everfold.shared.generated.resources.conversation_side_margins_hint
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.send_shortcut_setting
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

/** 会话偏好页：发送快捷键、长消息折叠与正文宽度。 */
@Composable
internal fun ConversationSettingsPage(
    shortcut: SendShortcut,
    onShortcutChange: (SendShortcut) -> Unit,
    collapsePrompts: Boolean,
    onCollapsePromptsChange: (Boolean) -> Unit,
    sideMargins: Boolean,
    onSideMarginsChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (desktopUi) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallTitle(
                    text = localizedString(Res.string.send_shortcut_setting),
                    insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                )
                SettingsChoices(
                    options = SendShortcut.entries,
                    selected = shortcut,
                    onSelect = onShortcutChange,
                    label = {
                        localizedString(if (it == SendShortcut.Enter) Res.string.send_shortcut_enter else Res.string.send_shortcut)
                    },
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = localizedString(Res.string.conversation_side_margins),
                summary = localizedString(Res.string.conversation_side_margins_hint),
                checked = sideMargins,
                onCheckedChange = onSideMarginsChange,
                modifier = Modifier.semantics { toggleableState = ToggleableState(sideMargins) },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SwitchPreference(
                title = localizedString(Res.string.collapse_long_prompts),
                checked = collapsePrompts,
                onCheckedChange = onCollapsePromptsChange,
                modifier = Modifier.semantics { toggleableState = ToggleableState(collapsePrompts) },
            )
        }
    }
}

/** 通用单选列表：HyperOS 分组卡片 + Miuix RadioButtonPreference */
@Composable
internal fun <T> SettingsChoices(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            RadioButtonPreference(
                title = label(option),
                selected = selected == option,
                onClick = { onSelect(option) },
            )
        }
    }
}
