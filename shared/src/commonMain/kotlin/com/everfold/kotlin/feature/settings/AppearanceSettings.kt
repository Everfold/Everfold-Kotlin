package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.ThemeMode
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.appearance
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.preference.RadioButtonPreference

/** 外观模式选择按空间使用分段标签或 HyperOS 分组卡片 */
@Composable
internal fun AppearanceSettings(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    horizontal: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SmallTitle(text = localizedString(Res.string.appearance), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
        if (horizontal) {
            TabRowWithContour(
                tabs = ThemeMode.entries.map { localizedString(it.label) },
                selectedTabIndex = selected.ordinal,
                onTabSelected = { onSelect(ThemeMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    RadioButtonPreference(
                        title = localizedString(mode.label),
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        }
    }
}
