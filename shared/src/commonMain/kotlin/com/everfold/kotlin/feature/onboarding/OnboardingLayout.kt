package com.everfold.kotlin.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 首次设置的共享滚动容器使用 Miuix 跨平台滚动条 */
@OptIn(ExperimentalScrollBarApi::class)
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
        if (scroll.maxValue > 0) {
            VerticalScrollBar(rememberScrollBarAdapter(scroll), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
        }
    }
}

/** 欢迎介绍与功能卡片随可用宽度切换一列或两列 */
@Composable
internal fun WelcomeFeatures(wide: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        Text(
            text = localizedString(Res.string.onboarding_intro),
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        val features =
            listOf(
                Triple(AppSymbol.Connection, Res.string.feature_workspace, Res.string.feature_workspace_detail),
                Triple(AppSymbol.Chat, Res.string.feature_topology, Res.string.feature_topology_detail),
                Triple(AppSymbol.Spark, Res.string.feature_continuity, Res.string.feature_continuity_detail),
                Triple(AppSymbol.Settings, Res.string.feature_customization, Res.string.feature_customization_detail),
            )
        FlowRow(
            Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (wide) 2 else 1,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            features.forEach { (icon, title, detail) ->
                Card(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 20.dp,
                    insideMargin = PaddingValues(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureIcon(icon)
                        Text(
                            text = localizedString(title),
                            style = MiuixTheme.textStyles.headline1,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = localizedString(detail),
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** 选择本地或集群模式，本地模式跳过后续连接配置 */
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
            ModeOptionCard(
                clusterOption = option,
                selected = cluster == option,
                onSelect = { onChange(option) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeOptionCard(
    clusterOption: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (selected) {
            MiuixTheme.colorScheme.secondaryContainer
        } else {
            MiuixTheme.colorScheme.surfaceContainer
        }
    val borderColor = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.outline
    Surface(
        modifier =
            modifier
                .squircleSurface(containerColor, 24.dp)
                .squircleBorder(width = if (selected) 1.5.dp else 1.dp, color = borderColor, cornerRadius = 24.dp),
        color = Color.Transparent,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .selectable(selected, role = Role.RadioButton, onClick = onSelect)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FeatureIcon(if (clusterOption) AppSymbol.Connection else AppSymbol.Spark)
                RadioButton(selected = selected, onClick = null)
            }
            Text(
                text = localizedString(if (clusterOption) Res.string.mode_cluster else Res.string.mode_offline),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = localizedString(if (clusterOption) Res.string.cluster_hint else Res.string.offline_hint),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

@Composable
private fun FeatureIcon(symbol: AppSymbol) {
    Surface(
        modifier = Modifier.squircleSurface(MiuixTheme.colorScheme.primaryContainer, 14.dp),
        color = Color.Transparent,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            AppIcon(symbol, tint = MiuixTheme.colorScheme.onPrimaryContainer)
        }
    }
}
