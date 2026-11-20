package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.ExecutionStepSnapshot
import com.everfold.kotlin.feature.conversation.StepStatus
import com.everfold.kotlin.feature.conversation.TaskExecutionTrace
import com.everfold.kotlin.feature.conversation.WaitingGate
import com.everfold.kotlin.ui.localization.UiText
import kotlin.math.roundToInt

internal const val PERCENT_HUNDRED = 100
private const val COMPLETE_PROGRESS = 1f

internal enum class ScenarioKind {
    StorageArchive,
    Diagnostics,
    DocumentIndex,
    GenericWorkspace,
}

internal data class StepSpec(
    val id: String,
    val title: UiText,
    val toolName: String,
    val commandPreview: UiText,
    val totalChunks: Int,
    val chunkDelayMillis: Long,
    val totalItems: Int = totalChunks,
    val observationBuilder: ((Int, Int) -> UiText)? = null,
    val gate: WaitingGate? = null,
    val autoFaultAtChunk: Int? = null,
    val thermalNoteAtChunk: Int? = null,
)

internal data class TaskRunContext(
    val prompt: String,
    val workspacePath: String,
    val attachmentNames: List<String>,
    val selectedGateOptionId: String?,
    val userConstraints: List<String>,
    val elapsedMillis: Long,
)

internal data class ScenarioSpec(
    val kind: ScenarioKind,
    val headline: UiText,
    val steps: List<StepSpec>,
    val streamDurationMillis: Long,
    val reportBuilder: (TaskRunContext) -> String,
)

internal data class TaskRunRecord(
    val taskId: String,
    val conversationId: String,
    val connectionId: String,
    val assistantMessageId: String,
    val prompt: String,
    val workspacePath: String,
    val attachmentNames: List<String>,
    val scenario: ScenarioSpec,
    val currentStepIndex: Int = 0,
    val currentChunkOffset: Int = 0,
    val stepDurations: Map<Int, Long> = emptyMap(),
    val resumedSteps: Set<Int> = emptySet(),
    val approvedGates: Set<String> = emptySet(),
    val selectedGateOptionId: String? = null,
    val userConstraints: List<String> = emptyList(),
    val elapsedMillis: Long = 0L,
    val throttled: Boolean = false,
    val pendingIoInterrupt: Boolean = false,
    val autoFaultTriggered: Boolean = false,
    val deviceNote: DeviceEnvironmentNote? = null,
) {
    fun toContext(): TaskRunContext =
        TaskRunContext(
            prompt = prompt,
            workspacePath = workspacePath,
            attachmentNames = attachmentNames,
            selectedGateOptionId = selectedGateOptionId,
            userConstraints = userConstraints,
            elapsedMillis = elapsedMillis,
        )

    fun overallProgress(runState: AgentRunState): Float {
        if (runState == AgentRunState.Completed) return COMPLETE_PROGRESS
        val total = scenario.steps.size.coerceAtLeast(1)
        val completedSteps = currentStepIndex.coerceIn(0, total)
        val step = scenario.steps.getOrNull(currentStepIndex)
        val stepFraction =
            if (step == null || step.totalChunks <= 0) {
                0f
            } else {
                (currentChunkOffset.toFloat() / step.totalChunks).coerceIn(0f, COMPLETE_PROGRESS)
            }
        return ((completedSteps + stepFraction) / total).coerceIn(0f, COMPLETE_PROGRESS)
    }

    fun progressPercent(runState: AgentRunState): Int = (overallProgress(runState) * PERCENT_HUNDRED).roundToInt()

    fun toTrace(runState: AgentRunState): TaskExecutionTrace {
        val activeStepIdx = currentStepIndex.coerceIn(0, scenario.steps.lastIndex)
        val snapshots =
            scenario.steps.mapIndexed { index, spec ->
                buildStepSnapshot(index, activeStepIdx, spec, runState)
            }
        val activeGate =
            scenario.steps
                .getOrNull(activeStepIdx)
                ?.gate
                ?.takeIf { runState == AgentRunState.Waiting }
        return TaskExecutionTrace(
            taskId = taskId,
            headline = scenario.headline,
            currentStepIndex = activeStepIdx + 1,
            totalSteps = scenario.steps.size,
            overallProgress = overallProgress(runState),
            elapsedMillis = elapsedMillis,
            steps = snapshots,
            deviceNote = deviceNote,
            activeGate = activeGate,
        )
    }

    private fun buildStepSnapshot(
        index: Int,
        activeIndex: Int,
        spec: StepSpec,
        runState: AgentRunState,
    ): ExecutionStepSnapshot {
        val status = resolveStepStatus(index, activeIndex, runState)
        val chunkProgress =
            when {
                index < activeIndex || runState == AgentRunState.Completed -> COMPLETE_PROGRESS
                index > activeIndex -> null
                spec.totalChunks <= 0 -> null
                else -> (currentChunkOffset.toFloat() / spec.totalChunks).coerceIn(0f, COMPLETE_PROGRESS)
            }
        val observation =
            when {
                index > activeIndex && runState != AgentRunState.Completed -> {
                    null
                }

                spec.observationBuilder == null -> {
                    null
                }

                index < activeIndex || runState == AgentRunState.Completed -> {
                    spec.observationBuilder.invoke(spec.totalItems, spec.totalItems)
                }

                else -> {
                    val doneItems = ((chunkProgress ?: 0f) * spec.totalItems).roundToInt().coerceAtMost(spec.totalItems)
                    spec.observationBuilder.invoke(doneItems, spec.totalItems)
                }
            }
        return ExecutionStepSnapshot(
            id = spec.id,
            title = spec.title,
            status = status,
            toolName = spec.toolName,
            commandPreview = spec.commandPreview,
            observation = observation,
            stepProgress = chunkProgress,
            durationMillis = stepDurations[index],
            resumedFromCheckpoint = index in resumedSteps,
        )
    }

    private fun resolveStepStatus(
        index: Int,
        activeIndex: Int,
        runState: AgentRunState,
    ): StepStatus =
        when {
            runState == AgentRunState.Completed || index < activeIndex -> StepStatus.Completed
            index > activeIndex -> StepStatus.Pending
            runState == AgentRunState.Waiting -> StepStatus.WaitingApproval
            runState == AgentRunState.Failed -> StepStatus.Failed
            runState == AgentRunState.Running -> StepStatus.Running
            else -> StepStatus.Pending
        }
}
