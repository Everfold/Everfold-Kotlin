package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.follow_system
import everfold.shared.generated.resources.language
import everfold.shared.generated.resources.language_chinese
import everfold.shared.generated.resources.language_english

/** 语言选择跟随系统简体中文或 English实际切换由上层回调处理 */
@Composable
internal fun LanguageSettings(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 语言
        Text(localizedString(Res.string.language), style = MaterialTheme.typography.titleMedium)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().selectableGroup().padding(vertical = 8.dp)) {
                AppLanguage.entries.forEach { option ->
                    val label =
                        when (option) {
                            // 跟随系统
                            AppLanguage.System -> Res.string.follow_system

                            // 简体中文
                            AppLanguage.Chinese -> Res.string.language_chinese

                            // English
                            AppLanguage.English -> Res.string.language_english
                        }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(language == option, role = Role.RadioButton, onClick = { onLanguageChange(option) })
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(language == option, onClick = null)
                        Text(localizedString(label))
                    }
                }
            }
        }
    }
}
