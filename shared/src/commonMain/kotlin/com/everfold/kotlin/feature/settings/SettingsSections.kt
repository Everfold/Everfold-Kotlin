package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.about
import everfold.shared.generated.resources.appearance
import everfold.shared.generated.resources.collapse_long_prompts
import everfold.shared.generated.resources.conversation_settings
import everfold.shared.generated.resources.conversation_side_margins
import everfold.shared.generated.resources.conversation_side_margins_hint
import everfold.shared.generated.resources.conversation_text_size
import everfold.shared.generated.resources.display_settings
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.github_repository
import everfold.shared.generated.resources.kernel_github_repository
import everfold.shared.generated.resources.language
import everfold.shared.generated.resources.language_chinese
import everfold.shared.generated.resources.language_english
import everfold.shared.generated.resources.logo
import everfold.shared.generated.resources.official_website
import everfold.shared.generated.resources.open_in_browser
import everfold.shared.generated.resources.open_source_license
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.send_shortcut_setting
import everfold.shared.generated.resources.text_size_extra_large
import everfold.shared.generated.resources.text_size_large
import everfold.shared.generated.resources.text_size_preview
import everfold.shared.generated.resources.text_size_small
import everfold.shared.generated.resources.theme_settings
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Immutable
internal data class SettingsUiState(
    val language: AppLanguage,
    val themeMode: ThemeMode,
    val themeColor: ThemeColorSelection,
    val textSize: ConversationTextSize,
    val shortcut: SendShortcut,
    val collapsePrompts: Boolean,
    val sideMargins: Boolean,
)

@Immutable
internal class SettingsActions(
    val onLanguageChange: (AppLanguage) -> Unit,
    val onThemeChange: (ThemeMode) -> Unit,
    val onSystemColorChange: () -> Unit,
    val onPresetChange: (ThemePreset) -> Unit,
    val onCustomColorChange: (Color) -> Unit,
    val onTextSizeChange: (ConversationTextSize) -> Unit,
    val onShortcutChange: (SendShortcut) -> Unit,
    val onCollapsePromptsChange: (Boolean) -> Unit,
    val onSideMarginsChange: (Boolean) -> Unit,
)

internal val AppLanguage.labelResource: StringResource
    get() =
        when (this) {
            AppLanguage.System -> Res.string.follow_system
            AppLanguage.Chinese -> Res.string.language_chinese
            AppLanguage.English -> Res.string.language_english
        }

private val ConversationTextSize.labelResource: StringResource
    get() =
        when (this) {
            ConversationTextSize.System -> Res.string.follow_system
            ConversationTextSize.Small -> Res.string.text_size_small
            ConversationTextSize.Large -> Res.string.text_size_large
            ConversationTextSize.ExtraLarge -> Res.string.text_size_extra_large
        }

private val SendShortcut.labelResource: StringResource
    get() =
        when (this) {
            SendShortcut.ModifiedEnter -> Res.string.send_shortcut
            SendShortcut.Enter -> Res.string.send_shortcut_enter
        }

private data class AboutLinkItem(
    val title: StringResource,
    val summary: String,
    val url: String,
)

private val ABOUT_LINKS =
    listOf(
        AboutLinkItem(Res.string.official_website, "everfold.org", "https://everfold.org/"),
        AboutLinkItem(Res.string.github_repository, "github.com/Everfold/Everfold-Kotlin", "https://github.com/Everfold/Everfold-Kotlin"),
        AboutLinkItem(Res.string.kernel_github_repository, "github.com/Everfold/Beamlet", "https://github.com/Everfold/Beamlet"),
        AboutLinkItem(Res.string.open_source_license, "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    )

/** HyperOS 单页分组偏好列表：外观、主题色、显示预览、会话偏好与关于。 */
@Composable
internal fun SettingsListContent(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "appearance") {
            SettingsWidthBox {
                AppearanceSection(state, actions)
            }
        }
        item(key = "theme_color") {
            SettingsWidthBox {
                ThemeColorSettings(
                    selection = state.themeColor,
                    onSystemColorChange = actions.onSystemColorChange,
                    onPresetChange = actions.onPresetChange,
                    onCustomColorChange = actions.onCustomColorChange,
                )
            }
        }
        item(key = "display") {
            SettingsWidthBox {
                DisplaySection(state.textSize, actions.onTextSizeChange)
            }
        }
        item(key = "conversation") {
            SettingsWidthBox {
                ConversationSection(state, actions)
            }
        }
        item(key = "about") {
            SettingsWidthBox {
                AboutSection()
            }
        }
    }
}

@Composable
private fun SettingsWidthBox(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.widthIn(max = 760.dp).fillMaxWidth(), content = content)
}

@Composable
private fun AppearanceSection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    val backdrop = LocalAppBackdrop.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.theme_settings), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            GlassDropdownPreference(
                title = localizedString(Res.string.appearance),
                items = ThemeMode.entries.map { localizedString(it.label) },
                selectedIndex = ThemeMode.entries.indexOf(state.themeMode),
                onSelectedIndexChange = { actions.onThemeChange(ThemeMode.entries[it]) },
                backdrop = backdrop,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            GlassDropdownPreference(
                title = localizedString(Res.string.language),
                items = AppLanguage.entries.map { localizedString(it.labelResource) },
                selectedIndex = AppLanguage.entries.indexOf(state.language),
                onSelectedIndexChange = { actions.onLanguageChange(AppLanguage.entries[it]) },
                backdrop = backdrop,
            )
        }
    }
}

@Composable
private fun DisplaySection(
    textSize: ConversationTextSize,
    onTextSizeChange: (ConversationTextSize) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.display_settings), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            GlassDropdownPreference(
                title = localizedString(Res.string.conversation_text_size),
                items = ConversationTextSize.entries.map { localizedString(it.labelResource) },
                selectedIndex = ConversationTextSize.entries.indexOf(textSize),
                onSelectedIndexChange = { onTextSizeChange(ConversationTextSize.entries[it]) },
                backdrop = LocalAppBackdrop.current,
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            ConversationTextScale(textSize) {
                Text(
                    text = localizedString(Res.string.text_size_preview),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }
    }
}

@Composable
private fun ConversationSection(
    state: SettingsUiState,
    actions: SettingsActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(
            text = localizedString(Res.string.conversation_settings),
            insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            if (desktopUi) {
                GlassDropdownPreference(
                    title = localizedString(Res.string.send_shortcut_setting),
                    items = SendShortcut.entries.map { localizedString(it.labelResource) },
                    selectedIndex = SendShortcut.entries.indexOf(state.shortcut),
                    onSelectedIndexChange = { actions.onShortcutChange(SendShortcut.entries[it]) },
                    backdrop = LocalAppBackdrop.current,
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
            SwitchPreference(
                title = localizedString(Res.string.conversation_side_margins),
                summary = localizedString(Res.string.conversation_side_margins_hint),
                checked = state.sideMargins,
                onCheckedChange = actions.onSideMarginsChange,
                modifier = Modifier.semantics { toggleableState = ToggleableState(state.sideMargins) },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            SwitchPreference(
                title = localizedString(Res.string.collapse_long_prompts),
                checked = state.collapsePrompts,
                onCheckedChange = actions.onCollapsePromptsChange,
                modifier = Modifier.semantics { toggleableState = ToggleableState(state.collapsePrompts) },
            )
        }
    }
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    val openLabel = localizedString(Res.string.open_in_browser)
    val version = appVersion()?.let { "v$it" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.about), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            BasicComponent(
                title = "Everfold",
                summary = version,
                startAction = {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 12.dp).size(40.dp),
                    )
                },
            )
            ABOUT_LINKS.forEach { item ->
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                ArrowPreference(
                    title = localizedString(item.title),
                    summary = item.summary,
                    modifier = Modifier.semantics { stateDescription = openLabel },
                    endActions = {
                        AppIcon(
                            AppSymbol.OpenInNew,
                            modifier = Modifier.size(18.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        )
                    },
                    onClick = { uriHandler.openUri(item.url) },
                )
            }
        }
    }
}
