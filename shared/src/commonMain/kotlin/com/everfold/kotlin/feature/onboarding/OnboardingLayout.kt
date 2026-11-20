package com.everfold.kotlin.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.AppearanceSettings
import com.everfold.kotlin.feature.settings.LanguageSettings
import com.everfold.kotlin.feature.settings.ThemeColorSettings
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.AppearancePreferences
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cluster_hint
import everfold.shared.generated.resources.feature_continuity
import everfold.shared.generated.resources.feature_continuity_detail
import everfold.shared.generated.resources.feature_customization
import everfold.shared.generated.resources.feature_customization_detail
import everfold.shared.generated.resources.feature_topology
import everfold.shared.generated.resources.feature_topology_detail
import everfold.shared.generated.resources.feature_workspace
import everfold.shared.generated.resources.feature_workspace_detail
import everfold.shared.generated.resources.mode_cluster
import everfold.shared.generated.resources.mode_offline
import everfold.shared.generated.resources.offline_hint
import everfold.shared.generated.resources.onboarding_intro

/** 首次设置的共享滚动容器滚动条由各平台实现 */
@Composable
internal fun OnboardingScrollArea(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    Box(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(scroll).padding(top = 24.dp, bottom = 24.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            content = content,
        )
        OnboardingScrollbar(scroll, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
internal expect fun OnboardingScrollbar(
    scroll: ScrollState,
    modifier: Modifier = Modifier,
)

/** 欢迎介绍与功能卡片随可用宽度切换一列或两列 */
@Composable
internal fun WelcomeFeatures(wide: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Text(
            // 欢迎使用 Everfold一款具有无限可能的智能体
            localizedString(Res.string.onboarding_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val features =
            listOf(
                // 随你成长的工作空间 / 连接你的设备工具与计算资源让更多力量参与工作
                Triple(AppSymbol.Connection, Res.string.feature_workspace, Res.string.feature_workspace_detail),
                // 由你组织的 AI 团队 / 自由安排 Agent 的分工协作与运行位置让 AI 按你的方式配合
                Triple(AppSymbol.Chat, Res.string.feature_topology, Res.string.feature_topology_detail),
                // 让每一次运行接续下去 / 核查执行过程接续可恢复的任务带着已有成果探索新的方向
                Triple(AppSymbol.Spark, Res.string.feature_continuity, Res.string.feature_continuity_detail),
                // 构建适合你的 AI / 从模型工具到运行环境自由选择与组合打造自己的工作方式
                Triple(AppSymbol.Settings, Res.string.feature_customization, Res.string.feature_customization_detail),
            )
        FlowRow(
            Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (wide) 2 else 1,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            features.forEach { (icon, title, detail) ->
                Surface(Modifier.weight(1f), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FeatureIcon(icon)
                        Text(localizedString(title), style = MaterialTheme.typography.titleMedium)
                        Text(
                            localizedString(detail),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** 选择本地或集群模式本地模式跳过后续连接配置 */
@Composable
internal fun ModeSelection(
    cluster: Boolean,
    onChange: (Boolean) -> Unit,
    wide: Boolean,
) {
    FlowRow(
        Modifier.fillMaxWidth().selectableGroup(),
        maxItemsInEachRow = if (wide) 2 else 1,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        listOf(false, true).forEach { option ->
            val selected = cluster == option
            Surface(
                Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected, role = Role.RadioButton, onClick = { onChange(option) })
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FeatureIcon(if (option) AppSymbol.Connection else AppSymbol.Spark)
                        RadioButton(selected, onClick = null)
                    }
                    Text(
                        // 集群 / 本地
                        localizedString(if (option) Res.string.mode_cluster else Res.string.mode_offline),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        // 连接局域网中心节点或云服务商 / 仅在本机上使用无需连接集群
                        localizedString(if (option) Res.string.cluster_hint else Res.string.offline_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureIcon(symbol: AppSymbol) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { AppIcon(symbol) }
    }
}

internal const val PREFERENCE_MEDIUM_COLUMNS = 2

@Composable
internal fun OnboardingPreferences(
    appearance: AppearancePreferences,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    columns: Int,
    appearanceHorizontal: Boolean,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        maxItemsInEachRow = columns,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            AppearanceSettings(appearance.themeMode, appearance::setTheme, appearanceHorizontal)
            if (columns == PREFERENCE_MEDIUM_COLUMNS) OnboardingThemeColors(appearance)
        }
        if (columns != PREFERENCE_MEDIUM_COLUMNS) {
            Column(Modifier.weight(1f)) { OnboardingThemeColors(appearance) }
        }
        Column(Modifier.weight(1f)) { LanguageSettings(language, onLanguageChange) }
    }
}

@Composable
private fun OnboardingThemeColors(appearance: AppearancePreferences) {
    ThemeColorSettings(
        appearance.themeColor,
        appearance::useSystemThemeColor,
        appearance::selectThemePreset,
        appearance::applyCustomThemeColor,
    )
}
