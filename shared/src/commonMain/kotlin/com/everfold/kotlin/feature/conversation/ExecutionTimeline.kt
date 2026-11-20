package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_gate_custom_hint
import everfold.shared.generated.resources.longtask_resumed_badge
import everfold.shared.generated.resources.longtask_status_completed
import everfold.shared.generated.resources.longtask_status_failed
import everfold.shared.generated.resources.longtask_status_running
import everfold.shared.generated.resources.longtask_status_stopped
import everfold.shared.generated.resources.longtask_status_waiting
import everfold.shared.generated.resources.longtask_step_duration
import kotlin.math.roundToInt

private const val PERCENT_SCALE = 100
private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
private const val TWO_DIGIT_THRESHOLD = 10L
private const val PENDING_STEP_ALPHA = 0.55f

@Composable
internal fun formatExecutionSummary(
    state: AgentRunState,
    trace: TaskExecutionTrace,
): String {
    val stateLabel = localizedString(state.label)
    val stepIndex = trace.currentStepIndex.coerceIn(1, trace.totalSteps.coerceAtLeast(1))
    val stepTitle =
        trace.steps
            .getOrNull(stepIndex - 1)
            ?.title
            ?.resolve()
            .orEmpty()
    val percent = (trace.overallProgress * PERCENT_SCALE).roundToInt().coerceIn(0, PERCENT_SCALE)
    return when (state) {
        AgentRunState.Running -> {
            localizedString(
                Res.string.longtask_status_running,
                stateLabel,
                stepIndex,
                trace.totalSteps,
                stepTitle,
                percent,
                formatElapsedClock(trace.elapsedMillis),
            )
        }

        AgentRunState.Waiting -> {
            localizedString(
                Res.string.longtask_status_waiting,
                stateLabel,
                stepIndex,
                trace.totalSteps,
                stepTitle,
                formatElapsedClock(trace.elapsedMillis),
            )
        }

        AgentRunState.Stopped -> {
            localizedString(Res.string.longtask_status_stopped, stateLabel, stepIndex, trace.totalSteps, percent)
        }

        AgentRunState.Failed -> {
            localizedString(Res.string.longtask_status_failed, stateLabel, stepIndex, trace.totalSteps, percent)
        }

        AgentRunState.Completed, AgentRunState.Idle -> {
            val totalSeconds = (trace.elapsedMillis / MILLIS_PER_SECOND).coerceAtLeast(1L)
            localizedString(
                Res.string.longtask_status_completed,
                stateLabel,
                trace.totalSteps,
                localizedString(Res.string.longtask_step_duration, totalSeconds),
            )
        }
    }
}

internal fun formatElapsedClock(millis: Long): String {
    val totalSeconds = (millis / MILLIS_PER_SECOND).coerceAtLeast(0L)
    val minutes = totalSeconds / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    val minStr = if (minutes < TWO_DIGIT_THRESHOLD) "0$minutes" else "$minutes"
    val secStr = if (seconds < TWO_DIGIT_THRESHOLD) "0$seconds" else "$seconds"
    return "$minStr:$secStr"
}

@Composable
internal fun ExecutionTimelineCard(
    trace: TaskExecutionTrace,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                trace.headline.resolve(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            LinearProgressIndicator(
                progress = { trace.overallProgress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            trace.deviceNote?.let { note ->
                DeviceNoteBanner(note)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                trace.steps.forEachIndexed { index, step ->
                    ExecutionStepRow(stepNumber = index + 1, step = step)
                }
            }
        }
    }
}

@Composable
private fun DeviceNoteBanner(note: DeviceEnvironmentNote) {
    val warning = note.severity == DeviceNoteSeverity.Warning
    val container = if (warning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
    val content = if (warning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
    Surface(shape = RoundedCornerShape(10.dp), color = container) {
        Text(
            note.message.resolve(),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = content,
        )
    }
}

@Composable
private fun ExecutionStepRow(
    stepNumber: Int,
    step: ExecutionStepSnapshot,
) {
    val rowAlpha = if (step.status == StepStatus.Pending) PENDING_STEP_ALPHA else 1f
    Column(
        Modifier.fillMaxWidth().alpha(rowAlpha),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StepStatusGlyph(stepNumber, step.status)
            Text(
                "$stepNumber. ${step.title.resolve()}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (step.resumedFromCheckpoint) {
                Text(
                    localizedString(Res.string.longtask_resumed_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            step.durationMillis?.takeIf { step.status == StepStatus.Completed }?.let { durationMs ->
                val sec = (durationMs / MILLIS_PER_SECOND).coerceAtLeast(1L)
                Text(
                    localizedString(Res.string.longtask_step_duration, sec),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        StepToolAndObservation(step)
    }
}

@Composable
private fun StepStatusGlyph(
    stepNumber: Int,
    status: StepStatus,
) {
    when (status) {
        StepStatus.Completed -> AppIcon(AppSymbol.Check, Modifier.size(14.dp))
        StepStatus.Running -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp)
        StepStatus.Paused -> AppIcon(AppSymbol.Stop, Modifier.size(12.dp))
        StepStatus.Failed -> AppIcon(AppSymbol.Close, Modifier.size(14.dp))
        StepStatus.WaitingApproval -> AppIcon(AppSymbol.Spark, Modifier.size(14.dp))
        StepStatus.Pending -> Text("$stepNumber", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StepToolAndObservation(step: ExecutionStepSnapshot) {
    if (step.status == StepStatus.Pending) return
    val showCommand = step.status != StepStatus.Completed
    Column(Modifier.padding(start = 22.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showCommand && step.toolName != null) {
            val preview = step.commandPreview?.resolve()
            val commandLine = if (preview.isNullOrBlank()) step.toolName else "${step.toolName} · $preview"
            Text(
                commandLine,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        step.observation?.let { obs ->
            Text(
                obs.resolve(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun WaitingGateCard(
    gate: WaitingGate,
    enabled: Boolean,
    onSelectOption: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                gate.prompt.resolve(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            gate.riskHint?.let { hint ->
                Text(
                    hint.resolve(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (gate.options.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    gate.options.forEachIndexed { index, option ->
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
            }
            Text(
                localizedString(Res.string.longtask_gate_custom_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
