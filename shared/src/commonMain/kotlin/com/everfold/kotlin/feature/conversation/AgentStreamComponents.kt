package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.ConversationTextScale
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.agent_continue
import everfold.shared.generated.resources.agent_retry
import everfold.shared.generated.resources.stream_approval_confirmed
import everfold.shared.generated.resources.stream_approval_custom_hint
import everfold.shared.generated.resources.stream_footer_completed
import everfold.shared.generated.resources.stream_footer_failed
import everfold.shared.generated.resources.stream_footer_stopped
import everfold.shared.generated.resources.stream_seconds_short
import everfold.shared.generated.resources.stream_thinking_active
import everfold.shared.generated.resources.stream_thinking_done
import everfold.shared.generated.resources.stream_thinking_paused
import everfold.shared.generated.resources.stream_toggle_thinking

private const val MILLIS_PER_SEC = 1000L
private const val CHEVRON_OPEN_DEG = 180f

@Composable
internal fun AgentStreamFlow(
    blocks: List<AgentStreamBlock>,
    textSize: ConversationTextSize,
    canInteract: Boolean,
    onSelectGateOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        blocks.forEach { block ->
            androidx.compose.runtime.key(block.id) {
                when (block) {
                    is AgentStreamBlock.Thinking -> {
                        StreamThinkingBlock(block)
                    }

                    is AgentStreamBlock.Narration -> {
                        if (block.text.isNotEmpty()) {
                            ConversationTextScale(textSize) {
                                SelectionContainer { MessageMarkdown(block.text) }
                            }
                        }
                    }

                    is AgentStreamBlock.ToolCall -> {
                        StreamToolCallBlock(block)
                    }

                    is AgentStreamBlock.Approval -> {
                        StreamApprovalBlock(block, enabled = canInteract, onSelectOption = onSelectGateOption)
                    }

                    is AgentStreamBlock.SystemNotice -> {
                        StreamNoticeLine(block.note)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamThinkingBlock(block: AgentStreamBlock.Thinking) {
    var manualExpanded by rememberSaveable(block.id) { mutableStateOf<Boolean?>(null) }
    val expanded = manualExpanded ?: (block.status == StreamRunStatus.Running)
    val toggleDesc = localizedString(Res.string.stream_toggle_thinking)
    val sec = (block.durationMillis / MILLIS_PER_SEC).coerceAtLeast(1L)
    val secText = localizedString(Res.string.stream_seconds_short, sec)
    val label =
        when (block.status) {
            StreamRunStatus.Running -> localizedString(Res.string.stream_thinking_active)
            StreamRunStatus.Completed -> localizedString(Res.string.stream_thinking_done, secText)
            StreamRunStatus.Paused, StreamRunStatus.Failed -> localizedString(Res.string.stream_thinking_paused, secText)
        }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button) { manualExpanded = !expanded }
                    .semantics { contentDescription = toggleDesc }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (block.status == StreamRunStatus.Running) {
                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
            } else {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                    AppIcon(AppSymbol.Spark, Modifier.size(14.dp))
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppIcon(AppSymbol.ChevronDown, Modifier.size(14.dp).rotate(if (expanded) CHEVRON_OPEN_DEG else 0f))
        }
        if (expanded && block.text.isNotEmpty()) {
            val barColor = MaterialTheme.colorScheme.outlineVariant
            Text(
                block.text,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .leftQuoteLine(barColor)
                        .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.leftQuoteLine(color: Color): Modifier =
    drawBehind {
        val strokePx = 2.dp.toPx()
        drawLine(
            color = color,
            start = Offset(strokePx / 2f, 0f),
            end = Offset(strokePx / 2f, size.height),
            strokeWidth = strokePx,
        )
    }

@Composable
private fun StreamApprovalBlock(
    block: AgentStreamBlock.Approval,
    enabled: Boolean,
    onSelectOption: (String) -> Unit,
) {
    val resolved = block.resolvedChoice
    if (resolved != null) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                AppIcon(AppSymbol.Check, Modifier.size(14.dp))
            }
            Text(
                localizedString(Res.string.stream_approval_confirmed, resolved.resolve()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        val barColor = MaterialTheme.colorScheme.primary
        Column(
            Modifier
                .fillMaxWidth()
                .leftQuoteLine(barColor)
                .padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(block.gate.prompt.resolve(), style = MaterialTheme.typography.bodySmall)
            block.gate.riskHint?.let { hint ->
                Text(
                    hint.resolve(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                block.gate.options.forEachIndexed { index, option ->
                    if (index == 0) {
                        FilledTonalButton(onClick = { onSelectOption(option.id) }, enabled = enabled) {
                            Text(option.label.resolve())
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectOption(option.id) }, enabled = enabled) {
                            Text(option.label.resolve())
                        }
                    }
                }
            }
            Text(
                localizedString(Res.string.stream_approval_custom_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StreamNoticeLine(note: DeviceEnvironmentNote) {
    val warning = note.severity == DeviceNoteSeverity.Warning
    val tone = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Text(
        note.message.resolve(),
        modifier =
            Modifier
                .fillMaxWidth()
                .leftQuoteLine(tone)
                .padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = if (warning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun StreamTurnFooter(
    runState: AgentRunState,
    elapsedMillis: Long?,
    canRetry: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (runState == AgentRunState.Idle || runState == AgentRunState.Running) return
    val sec = ((elapsedMillis ?: 0L) / MILLIS_PER_SEC).coerceAtLeast(1L)
    val secText = localizedString(Res.string.stream_seconds_short, sec)
    val statusText =
        when (runState) {
            AgentRunState.Completed -> localizedString(Res.string.stream_footer_completed, secText)
            AgentRunState.Stopped -> localizedString(Res.string.stream_footer_stopped, secText)
            AgentRunState.Failed -> localizedString(Res.string.stream_footer_failed)
            else -> null
        }
    val showAction = runState == AgentRunState.Waiting || runState == AgentRunState.Stopped || runState == AgentRunState.Failed
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (statusText != null) {
            Text(
                statusText,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (runState ==
                        AgentRunState.Failed
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        if (showAction) {
            TextButton(onClick = onRetry, enabled = canRetry) {
                Text(localizedString(if (runState == AgentRunState.Failed) Res.string.agent_retry else Res.string.agent_continue))
            }
        }
    }
}
