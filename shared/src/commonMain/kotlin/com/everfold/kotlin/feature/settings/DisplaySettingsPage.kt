package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

/** 显示设置调整会话文字大小并实时预览复用语言选择组件 */
@Composable
internal fun DisplaySettingsPage(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    textSize: ConversationTextSize,
    onTextSizeChange: (ConversationTextSize) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // 会话文字大小
        Text(localizedString(Res.string.conversation_text_size), style = MaterialTheme.typography.titleMedium)
        SettingsChoices(
            options = ConversationTextSize.entries,
            selected = textSize,
            onSelect = onTextSizeChange,
            label = {
                localizedString(
                    when (it) {
                        // 跟随系统
                        ConversationTextSize.System -> Res.string.follow_system

                        // 较小
                        ConversationTextSize.Small -> Res.string.text_size_small

                        // 较大
                        ConversationTextSize.Large -> Res.string.text_size_large

                        // 特大
                        ConversationTextSize.ExtraLarge -> Res.string.text_size_extra_large
                    },
                )
            },
        )
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            ConversationTextScale(textSize) {
                Text(
                    // 让每一个想法清晰呈现\n这里预览会话正文和输入框的文字大小
                    localizedString(Res.string.text_size_preview),
                    Modifier.fillMaxWidth().padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        LanguageSettings(language, onLanguageChange)
    }
}
