package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
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
import top.yukonga.miuix.kmp.basic.TabRowWithContour

/** 设置页签主题显示会话与关于 */
private enum class SettingsPage { Theme, Display, Conversation, About }

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
    onBack: (() -> Unit)? = null,
    bottomPadding: Dp = 0.dp,
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Theme) }
    val scrollStates = SettingsPage.entries.map { rememberScrollState() }
    SettingsLayout(
        title = localizedString(Res.string.nav_settings),
        modifier = modifier,
        onBack = onBack,
        bottomPadding = bottomPadding,
        scrollState = scrollStates[page.ordinal],
        navigation = {
            SettingsTabs(
                selected = page,
                onSelect = { page = it },
            )
        },
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
                    shortcut,
                    onShortcutChange,
                    collapsePrompts,
                    onCollapsePromptsChange,
                    sideMargins,
                    onSideMarginsChange,
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
    selected: SettingsPage,
    onSelect: (SettingsPage) -> Unit,
) {
    val tabs =
        listOf(
            localizedString(Res.string.theme_settings),
            localizedString(Res.string.display_settings),
            localizedString(Res.string.conversation_settings),
            localizedString(Res.string.about),
        )
    TabRowWithContour(
        tabs = tabs,
        selectedTabIndex = selected.ordinal,
        onTabSelected = { onSelect(SettingsPage.entries[it]) },
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}
