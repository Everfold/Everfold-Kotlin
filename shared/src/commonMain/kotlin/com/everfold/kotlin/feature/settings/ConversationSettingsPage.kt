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
import com.everfold.kotlin.ui.components.primaryShortcutModifier
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.collapse_long_prompts
import everfold.shared.generated.resources.conversation_side_margins
import everfold.shared.generated.resources.conversation_side_margins_hint
import everfold.shared.generated.resources.expand_process_completed
import everfold.shared.generated.resources.expand_process_completed_hint
import everfold.shared.generated.resources.expand_process_running
import everfold.shared.generated.resources.expand_process_running_hint
import everfold.shared.generated.resources.process_visibility_section
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.send_shortcut_setting

/** 会话偏好页：发送快捷键、执行过程默认展开策略、长消息折叠与正文宽度。 */
@Composable
internal fun ConversationSettingsPage(
    shortcut: SendShortcut,
    onShortcutChange: (SendShortcut) -> Unit,
    collapsePrompts: Boolean,
    onCollapsePromptsChange: (Boolean) -> Unit,
    sideMargins: Boolean,
    onSideMarginsChange: (Boolean) -> Unit,
    processVisibility: ProcessVisibility = ProcessVisibility(),
    onProcessVisibilityChange: (ProcessVisibility) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (desktopUi) {
            Text(localizedString(Res.string.send_shortcut_setting), style = MaterialTheme.typography.titleMedium)
            SettingsChoices(
                options = SendShortcut.entries,
                selected = shortcut,
                onSelect = onShortcutChange,
                label = {
                    if (it == SendShortcut.Enter) {
                        localizedString(Res.string.send_shortcut_enter)
                    } else {
                        localizedString(Res.string.send_shortcut, primaryShortcutModifier)
                    }
                },
            )
        }
        Text(localizedString(Res.string.process_visibility_section), style = MaterialTheme.typography.titleMedium)
        SettingsToggleRow(
            title = localizedString(Res.string.expand_process_running),
            checked = processVisibility.expandWhileRunning,
            onCheckedChange = { onProcessVisibilityChange(processVisibility.copy(expandWhileRunning = it)) },
            hint = localizedString(Res.string.expand_process_running_hint),
        )
        SettingsToggleRow(
            title = localizedString(Res.string.expand_process_completed),
            checked = processVisibility.expandAfterCompletion,
            onCheckedChange = { onProcessVisibilityChange(processVisibility.copy(expandAfterCompletion = it)) },
            hint = localizedString(Res.string.expand_process_completed_hint),
        )
        SettingsToggleRow(
            title = localizedString(Res.string.conversation_side_margins),
            checked = sideMargins,
            onCheckedChange = onSideMarginsChange,
            hint = localizedString(Res.string.conversation_side_margins_hint),
        )
        SettingsToggleRow(
            title = localizedString(Res.string.collapse_long_prompts),
            checked = collapsePrompts,
            onCheckedChange = onCollapsePromptsChange,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    hint: String? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
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
