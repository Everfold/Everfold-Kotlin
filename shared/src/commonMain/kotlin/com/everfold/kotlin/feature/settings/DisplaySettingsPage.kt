package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.conversation_text_size
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.text_size_extra_large
import everfold.shared.generated.resources.text_size_large
import everfold.shared.generated.resources.text_size_preview
import everfold.shared.generated.resources.text_size_small
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 显示设置调整会话文字大小并实时预览复用语言选择组件 */
@Composable
internal fun DisplaySettingsPage(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    textSize: ConversationTextSize,
    onTextSizeChange: (ConversationTextSize) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallTitle(
                text = localizedString(Res.string.conversation_text_size),
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            )
            TextSizeChoices(textSize, onTextSizeChange)
        }
        Card(modifier = Modifier.fillMaxWidth(), insideMargin = PaddingValues(20.dp)) {
            ConversationTextScale(textSize) {
                Text(
                    text = localizedString(Res.string.text_size_preview),
                    modifier = Modifier.fillMaxWidth(),
                    style = MiuixTheme.textStyles.body1,
                )
            }
        }
        LanguageSettings(language, onLanguageChange)
    }
}

@Composable
private fun TextSizeChoices(
    selected: ConversationTextSize,
    onSelect: (ConversationTextSize) -> Unit,
) {
    val label: @Composable (ConversationTextSize) -> String = { size ->
        localizedString(
            when (size) {
                ConversationTextSize.System -> Res.string.follow_system
                ConversationTextSize.Small -> Res.string.text_size_small
                ConversationTextSize.Large -> Res.string.text_size_large
                ConversationTextSize.ExtraLarge -> Res.string.text_size_extra_large
            },
        )
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 480.dp * LocalDensity.current.fontScale) {
            TabRowWithContour(
                tabs = ConversationTextSize.entries.map { label(it) },
                selectedTabIndex = selected.ordinal,
                onTabSelected = { onSelect(ConversationTextSize.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            SettingsChoices(ConversationTextSize.entries, selected, onSelect, label)
        }
    }
}
