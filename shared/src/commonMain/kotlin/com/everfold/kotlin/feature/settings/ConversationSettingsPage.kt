package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.collapse_long_prompts
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.send_shortcut_setting

/** 会话偏好页桌面发送快捷键与长用户消息自动折叠开关 */
@Composable
internal fun ConversationSettingsPage(
    shortcut: SendShortcut,
    onShortcutChange: (SendShortcut) -> Unit,
    collapsePrompts: Boolean,
    onCollapsePromptsChange: (Boolean) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (desktopUi) {
            // 发送快捷键
            Text(localizedString(Res.string.send_shortcut_setting), style = MaterialTheme.typography.titleMedium)
            SettingsChoices(
                options = SendShortcut.entries,
                selected = shortcut,
                onSelect = onShortcutChange,
                // Enter 发送 · Shift + Enter 换行 / Ctrl/Cmd + Enter 发送 · Enter 换行
                label = { localizedString(if (it == SendShortcut.Enter) Res.string.send_shortcut_enter else Res.string.send_shortcut) },
            )
        }
        Row(
            Modifier.fillMaxWidth().toggleable(collapsePrompts, role = Role.Switch, onValueChange = onCollapsePromptsChange).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 自动折叠较长的用户消息
            Text(localizedString(Res.string.collapse_long_prompts), Modifier.weight(1f))
            Switch(checked = collapsePrompts, onCheckedChange = null)
        }
    }
}

/** 通用单选列表整行负责选择避免单选按钮产生重复点击事件 */
@Composable
internal fun <T> SettingsChoices(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    Column(Modifier.fillMaxWidth().selectableGroup()) {
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected == option,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RadioButton(selected == option, onClick = null)
                Text(label(option))
            }
        }
    }
}
