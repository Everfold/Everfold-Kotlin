package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.SettingsLayout
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.about
import everfold.shared.generated.resources.conversation_settings
import everfold.shared.generated.resources.display_settings
import everfold.shared.generated.resources.nav_settings
import everfold.shared.generated.resources.theme_settings
import org.jetbrains.compose.resources.StringResource

/** 设置页签主题显示会话与关于 */
private enum class SettingsPage(
    val label: StringResource,
) {
    Theme(Res.string.theme_settings),
    Display(Res.string.display_settings),
    Conversation(Res.string.conversation_settings),
    About(Res.string.about),
}

/** 设置页入口分发偏好回调每个页签独立保存滚动位置 */
@Composable
fun SettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    themeMode: ThemeMode,
    themeColor: ThemeColorSelection,
    onThemeChange: (ThemeMode) -> Unit,
    onSystemColorChange: () -> Unit,
    onPresetChange: (ThemePreset) -> Unit,
    onCustomColorChange: (Color) -> Unit,
    textSize: ConversationTextSize,
    onTextSizeChange: (ConversationTextSize) -> Unit,
    shortcut: SendShortcut,
    onShortcutChange: (SendShortcut) -> Unit,
    collapsePrompts: Boolean,
    onCollapsePromptsChange: (Boolean) -> Unit,
    sideMargins: Boolean,
    onSideMarginsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    processVisibility: ProcessVisibility = ProcessVisibility(),
    onProcessVisibilityChange: (ProcessVisibility) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Theme) }
    val scrollStates = SettingsPage.entries.associateWith { rememberScrollState() }
    SettingsLayout(
        title = localizedString(Res.string.nav_settings),
        modifier = modifier,
        onBack = onBack,
        scrollState = scrollStates.getValue(page),
        navigation = { SettingsTabs(page) { page = it } },
    ) {
        when (page) {
            SettingsPage.Theme -> {
                ThemeSettingsPage(
                    themeMode = themeMode,
                    themeColor = themeColor,
                    onThemeChange = onThemeChange,
                    onSystemColorChange = onSystemColorChange,
                    onPresetChange = onPresetChange,
                    onCustomColorChange = onCustomColorChange,
                )
            }

            SettingsPage.Display -> {
                DisplaySettingsPage(language, onLanguageChange, textSize, onTextSizeChange)
            }

            SettingsPage.Conversation -> {
                ConversationSettingsPage(
                    shortcut = shortcut,
                    onShortcutChange = onShortcutChange,
                    collapsePrompts = collapsePrompts,
                    onCollapsePromptsChange = onCollapsePromptsChange,
                    sideMargins = sideMargins,
                    onSideMarginsChange = onSideMarginsChange,
                    processVisibility = processVisibility,
                    onProcessVisibilityChange = onProcessVisibilityChange,
                )
            }

            SettingsPage.About -> {
                AboutSettingsPage()
            }
        }
    }
}

@Composable
private fun SettingsTabs(
    selectedPage: SettingsPage,
    onSelect: (SettingsPage) -> Unit,
) {
    SecondaryScrollableTabRow(selectedTabIndex = selectedPage.ordinal, edgePadding = 0.dp) {
        SettingsPage.entries.forEach { item ->
            Tab(
                selected = selectedPage == item,
                onClick = { onSelect(item) },
                text = { Text(localizedString(item.label)) },
            )
        }
    }
}
