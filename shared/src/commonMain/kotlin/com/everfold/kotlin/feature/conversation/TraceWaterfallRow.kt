package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.trace_loop_heading
import everfold.shared.generated.resources.trace_span_duration
import everfold.shared.generated.resources.trace_span_issued_at
import everfold.shared.generated.resources.trace_span_returned_at
import everfold.shared.generated.resources.trace_span_running
import everfold.shared.generated.resources.trace_span_ttft

private const val MILLIS_PER_SECOND = 1_000L
private const val TENTH_ROUNDING_MS = 50L
private const val TENTH_DIVISOR_MS = 100L
private const val TENTHS_PER_SECOND = 10L
private const val SECONDS_PER_MINUTE = 60L
private const val HALF_DIVISOR = 2L
private const val TTFT_LEADING_ALPHA = 0.42f
private const val STATUS_PILL_BG_ALPHA = 0.65f
private const val BAR_CORNER_DP = 4
private const val MIN_SPAN_WIDTH_DP = 4
private const val EVENT_MARKER_RADIUS_DP = 3.5f

internal fun formatTraceDuration(ms: Long): String {
    val safeMs = ms.coerceAtLeast(0L)
    if (safeMs < MILLIS_PER_SECOND) return "${safeMs}ms"
    val tenths = (safeMs + TENTH_ROUNDING_MS) / TENTH_DIVISOR_MS
    val wholeSec = tenths / TENTHS_PER_SECOND
    return if (wholeSec < SECONDS_PER_MINUTE) {
        "$wholeSec.${tenths % TENTHS_PER_SECOND}s"
    } else {
        "${wholeSec / SECONDS_PER_MINUTE}m${wholeSec % SECONDS_PER_MINUTE}s"
    }
}

@Composable
internal fun spanKindColor(
    kind: TraceSpanKind,
    status: StreamRunStatus,
): Color =
    when {
        status == StreamRunStatus.Failed -> MaterialTheme.colorScheme.error
        kind == TraceSpanKind.LlmRequest -> MaterialTheme.colorScheme.primary
        kind == TraceSpanKind.ToolCall -> MaterialTheme.colorScheme.tertiary
        kind == TraceSpanKind.UserGate -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.outline
    }

@Composable
internal fun TraceLoopSection(
    loop: AgentLoopTrace,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TraceLoopHeader(loop)
            if (loop.promptPreview.isNotBlank()) {
                Text(
                    loop.promptPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TraceAxisRow(loop.totalDurationMs)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                loop.spans.forEach { span ->
                    TraceSpanRow(span = span, loopDurationMs = loop.totalDurationMs)
                }
            }
        }
    }
}

@Composable
private fun TraceLoopHeader(loop: AgentLoopTrace) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                localizedString(Res.string.trace_loop_heading, loop.loopIndex),
                style = MaterialTheme.typography.titleSmall,
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = STATUS_PILL_BG_ALPHA),
            ) {
                Text(
                    localizedString(loop.runState.label),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (loop.runState == AgentRunState.Failed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
        }
        Text(
            formatTraceDuration(loop.totalDurationMs),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TraceAxisRow(totalDurationMs: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val labelStyle = MaterialTheme.typography.labelSmall
        val labelColor = MaterialTheme.colorScheme.outline
        Text("0ms", style = labelStyle, fontFamily = FontFamily.Monospace, color = labelColor)
        Text(
            formatTraceDuration(totalDurationMs / HALF_DIVISOR),
            style = labelStyle,
            fontFamily = FontFamily.Monospace,
            color = labelColor,
        )
        Text(
            formatTraceDuration(totalDurationMs),
            style = labelStyle,
            fontFamily = FontFamily.Monospace,
            color = labelColor,
        )
    }
}

@Composable
private fun TraceSpanRow(
    span: TraceSpan,
    loopDurationMs: Long,
) {
    var expanded by rememberSaveable(span.id) { mutableStateOf(false) }
    val barColor = spanKindColor(span.kind, span.status)
    val durationMs = span.durationMs(loopDurationMs)
    val timingText =
        if (span.kind == TraceSpanKind.SystemEvent) {
            "+${formatTraceDuration(span.startOffsetMs)}"
        } else {
            formatTraceDuration(durationMs)
        }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TraceSpanLabelRow(span, barColor, timingText)
        TraceWaterfallBar(span, loopDurationMs, barColor)
        if (expanded) {
            TraceSpanDetailLine(span, durationMs)
        }
    }
}

@Composable
private fun TraceSpanLabelRow(
    span: TraceSpan,
    tone: Color,
    timingText: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(7.dp).background(tone, CircleShape))
        Text(
            span.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = if (span.kind == TraceSpanKind.ToolCall) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (span.endOffsetMs == null && span.status == StreamRunStatus.Running) {
            CircularProgressIndicator(Modifier.size(11.dp), strokeWidth = 1.5.dp)
        } else if (span.status == StreamRunStatus.Failed) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.error) {
                AppIcon(AppSymbol.Close, Modifier.size(12.dp))
            }
        }
        Text(
            timingText,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TraceWaterfallBar(
    span: TraceSpan,
    loopDurationMs: Long,
    barColor: Color,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val safeTotal = loopDurationMs.coerceAtLeast(1L).toFloat()
    val startRatio = (span.startOffsetMs / safeTotal).coerceIn(0f, 1f)
    val endRatio = (span.effectiveEndMs(loopDurationMs) / safeTotal).coerceIn(startRatio, 1f)
    val ttftRatio = span.ttftMillis?.let { ((span.startOffsetMs + it) / safeTotal).coerceIn(startRatio, endRatio) }
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val radius = CornerRadius(BAR_CORNER_DP.dp.toPx(), BAR_CORNER_DP.dp.toPx())
        drawRoundRect(color = trackColor, size = size, cornerRadius = radius)
        if (span.kind == TraceSpanKind.SystemEvent) {
            val centerX =
                (size.width * startRatio).coerceIn(
                    EVENT_MARKER_RADIUS_DP.dp.toPx(),
                    size.width - EVENT_MARKER_RADIUS_DP.dp.toPx(),
                )
            drawCircle(color = barColor, radius = EVENT_MARKER_RADIUS_DP.dp.toPx(), center = Offset(centerX, size.height / 2f))
        } else {
            val minWidthPx = MIN_SPAN_WIDTH_DP.dp.toPx()
            val leftPx = (size.width * startRatio).coerceAtMost((size.width - minWidthPx).coerceAtLeast(0f))
            val rightPx = (size.width * endRatio).coerceAtLeast(leftPx + minWidthPx).coerceAtMost(size.width)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(leftPx, 0f),
                size = Size((rightPx - leftPx).coerceAtLeast(minWidthPx), size.height),
                cornerRadius = radius,
            )
            if (ttftRatio != null && span.kind == TraceSpanKind.LlmRequest) {
                val ttftRightPx = (size.width * ttftRatio).coerceIn(leftPx, rightPx)
                if (ttftRightPx > leftPx) {
                    drawRoundRect(
                        color = trackColor.copy(alpha = TTFT_LEADING_ALPHA),
                        topLeft = Offset(leftPx, 0f),
                        size = Size(ttftRightPx - leftPx, size.height),
                        cornerRadius = radius,
                    )
                }
            }
        }
    }
}

@Composable
private fun TraceSpanDetailLine(
    span: TraceSpan,
    durationMs: Long,
) {
    val issuedLabel = localizedString(Res.string.trace_span_issued_at, "+${formatTraceDuration(span.startOffsetMs)}")
    val returnLabel =
        span.endOffsetMs?.let { localizedString(Res.string.trace_span_returned_at, "+${formatTraceDuration(it)}") }
            ?: localizedString(Res.string.trace_span_running)
    val durLabel = localizedString(Res.string.trace_span_duration, formatTraceDuration(durationMs))
    val ttftPart = span.ttftMillis?.let { " · ${localizedString(Res.string.trace_span_ttft, formatTraceDuration(it))}" }.orEmpty()
    val meta =
        if (span.kind == TraceSpanKind.SystemEvent) {
            issuedLabel
        } else {
            "$issuedLabel · $returnLabel · $durLabel$ttftPart"
        }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            meta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (span.detail.isNotBlank()) {
            Text(
                span.detail,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
