package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.trace_back_to_chat
import everfold.shared.generated.resources.trace_count_suffix
import everfold.shared.generated.resources.trace_empty_hint
import everfold.shared.generated.resources.trace_empty_title
import everfold.shared.generated.resources.trace_legend_llm
import everfold.shared.generated.resources.trace_legend_other
import everfold.shared.generated.resources.trace_legend_tool
import everfold.shared.generated.resources.trace_loops_value
import everfold.shared.generated.resources.trace_metric_llm
import everfold.shared.generated.resources.trace_metric_loops
import everfold.shared.generated.resources.trace_metric_tools
import everfold.shared.generated.resources.trace_metric_total
import everfold.shared.generated.resources.trace_title

private const val DIVIDER_ALPHA = 0.5f

/** 单会话 Agent Loop 执行轨迹页：汇总 LLM/Tool 耗时比例，并以瀑布流展示每次请求与工具的发出/返回时间轴。 */
@Composable
internal fun SessionTraceScreen(
    state: ConversationUiState,
    sideMargins: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = remember(state.messages, state.runState) { buildSessionTraceSummary(state) }
    Column(modifier.fillMaxSize()) {
        TraceTopBar(
            sessionTitle = state.title.resolve(),
            runState = state.runState,
            onBack = onBack,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
        if (summary.loops.isEmpty()) {
            TraceEmptyState(Modifier.weight(1f).fillMaxWidth())
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item(key = "trace-summary") {
                    Box(Modifier.conversationContentWidth(sideMargins).padding(horizontal = 24.dp)) {
                        SessionSummaryCard(summary)
                    }
                }
                items(summary.loops, key = { it.messageId }) { loop ->
                    Box(Modifier.conversationContentWidth(sideMargins).padding(horizontal = 24.dp)) {
                        TraceLoopSection(loop)
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceTopBar(
    sessionTitle: String,
    runState: AgentRunState,
    onBack: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val backLabel = localizedString(Res.string.trace_back_to_chat)
        AppTooltip(backLabel) {
            IconButton(onClick = onBack) { AppIcon(AppSymbol.Back, description = backLabel) }
        }
        Column(Modifier.weight(1f)) {
            Text(
                localizedString(Res.string.trace_title),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                sessionTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (runState != AgentRunState.Idle) {
            Text(
                localizedString(runState.label),
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (runState == AgentRunState.Failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TraceEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            AppIcon(AppSymbol.Trace, Modifier.size(32.dp))
        }
        Text(
            localizedString(Res.string.trace_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            localizedString(Res.string.trace_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SessionSummaryCard(summary: SessionTraceSummary) {
    val llmColor = MaterialTheme.colorScheme.primary
    val toolColor = MaterialTheme.colorScheme.tertiary
    val otherColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SummaryMetric(
                    localizedString(Res.string.trace_metric_loops),
                    localizedString(Res.string.trace_loops_value, summary.loopCount),
                )
                SummaryMetric(localizedString(Res.string.trace_metric_total), formatTraceDuration(summary.totalDurationMs))
                SummaryMetric(localizedString(Res.string.trace_metric_llm), formatTraceDuration(summary.llmDurationMs))
                SummaryMetric(
                    localizedString(Res.string.trace_metric_tools),
                    "${formatTraceDuration(summary.toolDurationMs)} · ${localizedString(Res.string.trace_count_suffix, summary.toolCount)}",
                )
            }
            BreakdownBar(summary, llmColor, toolColor, otherColor)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendChip(llmColor, "${localizedString(Res.string.trace_legend_llm)} (${summary.llmCount})")
                LegendChip(toolColor, "${localizedString(Res.string.trace_legend_tool)} (${summary.toolCount})")
                if (summary.otherDurationMs > 0L) {
                    LegendChip(otherColor, localizedString(Res.string.trace_legend_other))
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun BreakdownBar(
    summary: SessionTraceSummary,
    llmColor: Color,
    toolColor: Color,
    otherColor: Color,
) {
    val safeTotal = summary.totalDurationMs.coerceAtLeast(1L).toFloat()
    val llmFraction = (summary.llmDurationMs / safeTotal).coerceIn(0f, 1f)
    val toolFraction = (summary.toolDurationMs / safeTotal).coerceIn(0f, 1f - llmFraction)
    Canvas(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
        val llmW = size.width * llmFraction
        val toolW = size.width * toolFraction
        drawRect(color = otherColor, size = size)
        if (llmW > 0f) drawRect(color = llmColor, topLeft = Offset.Zero, size = Size(llmW, size.height))
        if (toolW > 0f) drawRect(color = toolColor, topLeft = Offset(llmW, 0f), size = Size(toolW, size.height))
    }
}

@Composable
private fun LegendChip(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
