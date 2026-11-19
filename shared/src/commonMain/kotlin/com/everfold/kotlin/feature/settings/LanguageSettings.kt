package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.language
import everfold.shared.generated.resources.language_chinese
import everfold.shared.generated.resources.language_english
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.RadioButtonPreference

/** 语言选择跟随系统简体中文或 English 实际切换由上层回调处理 */
@Composable
internal fun LanguageSettings(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.language), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            AppLanguage.entries.forEachIndexed { index, option ->
                if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                val label =
                    when (option) {
                        AppLanguage.System -> Res.string.follow_system
                        AppLanguage.Chinese -> Res.string.language_chinese
                        AppLanguage.English -> Res.string.language_english
                    }
                RadioButtonPreference(
                    title = localizedString(label),
                    selected = language == option,
                    onClick = { onLanguageChange(option) },
                )
            }
        }
    }
}
