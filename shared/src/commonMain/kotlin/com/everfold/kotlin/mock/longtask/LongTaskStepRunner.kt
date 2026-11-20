package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_thermal
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val THROTTLE_NUMERATOR = 8L
private const val THROTTLE_DENOMINATOR = 5L

internal class StageLoopCallbacks(
    val readRecord: () -> TaskRunRecord?,
    val writeRecord: (TaskRunRecord) -> Unit,
    val publishSnapshot: (TaskRunRecord, AgentRunState) -> Unit,
)

internal suspend fun runAllScenarioStages(callbacks: StageLoopCallbacks): Boolean {
    var outcome: Boolean? = null
    while (outcome == null) {
        val record = callbacks.readRecord()
        if (record == null) {
            outcome = false
        } else if (record.currentStageIndex >= record.scenario.stages.size) {
            outcome = true
        } else {
            val stage = record.scenario.stages[record.currentStageIndex]
            if (!executeSingleStage(stage, record, callbacks)) {
                outcome = false
            }
        }
    }
    return outcome
}

private suspend fun executeSingleStage(
    stage: StreamStageSpec,
    record: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean =
    when (stage) {
        is StreamStageSpec.ApprovalStage -> {
            if (stage.gate.id in record.approvedGates) {
                callbacks.writeRecord(record.copy(currentStageIndex = record.currentStageIndex + 1, currentChunkOffset = 0))
                true
            } else {
                val waiting = record.copy(activeBlock = AgentStreamBlock.Approval(id = stage.id, gate = stage.gate))
                callbacks.writeRecord(waiting)
                callbacks.publishSnapshot(waiting, AgentRunState.Waiting)
                false
            }
        }

        is StreamStageSpec.ThinkStage -> {
            runTextSlicesStage(stage, stage.thought, stage.durationMillis, record, callbacks)
        }

        is StreamStageSpec.NarrationStage -> {
            runTextSlicesStage(stage, stage.textBuilder(record.toContext()), stage.durationMillis, record, callbacks)
        }

        is StreamStageSpec.ToolStage -> {
            runToolChunksStage(stage, record, callbacks)
        }
    }

private suspend fun runTextSlicesStage(
    stage: StreamStageSpec,
    fullText: String,
    durationMillis: Long,
    initial: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean {
    val totalSlices = stage.totalUnits.coerceAtLeast(1)
    val baseSliceDelay = (durationMillis / totalSlices).coerceAtLeast(1L)
    val started =
        initial.copy(
            activeBlock = buildStreamingTextBlock(stage, fullText, initial.currentChunkOffset, totalSlices, initial.currentStageElapsedMs),
        )
    callbacks.writeRecord(started)
    callbacks.publishSnapshot(started, AgentRunState.Running)

    var outcome: Boolean? = null
    while (outcome == null) {
        val current = callbacks.readRecord()
        if (current == null) {
            outcome = false
        } else if (current.currentChunkOffset >= totalSlices) {
            outcome = commitCompletedTextStage(stage, fullText, current, callbacks)
        } else {
            outcome = advanceTextSlice(stage, fullText, totalSlices, baseSliceDelay, current, callbacks)
        }
    }
    return outcome
}

private suspend fun advanceTextSlice(
    stage: StreamStageSpec,
    fullText: String,
    totalSlices: Int,
    baseSliceDelay: Long,
    current: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean? {
    val delayMs = throttledDelay(baseSliceDelay, current.throttled)
    delay(delayMs)
    val latest = callbacks.readRecord() ?: return false
    val nextSlice = latest.currentChunkOffset + 1
    val nextStageElapsed = latest.currentStageElapsedMs + delayMs
    val updated =
        latest.copy(
            currentChunkOffset = nextSlice,
            currentStageElapsedMs = nextStageElapsed,
            elapsedMillis = latest.elapsedMillis + delayMs,
            activeBlock = buildStreamingTextBlock(stage, fullText, nextSlice, totalSlices, nextStageElapsed),
        )
    callbacks.writeRecord(updated)
    callbacks.publishSnapshot(updated, AgentRunState.Running)
    return null
}

private fun commitCompletedTextStage(
    stage: StreamStageSpec,
    fullText: String,
    current: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean {
    val completedBlock =
        if (stage is StreamStageSpec.ThinkStage) {
            AgentStreamBlock.Thinking(stage.id, fullText, StreamRunStatus.Completed, current.currentStageElapsedMs)
        } else {
            AgentStreamBlock.Narration(stage.id, fullText, isStreaming = false)
        }
    val advanced =
        current.copy(
            committedBlocks = current.committedBlocks + completedBlock,
            activeBlock = null,
            currentStageIndex = current.currentStageIndex + 1,
            currentChunkOffset = 0,
            currentStageElapsedMs = 0L,
        )
    callbacks.writeRecord(advanced)
    if (advanced.currentStageIndex < advanced.scenario.stages.size) {
        callbacks.publishSnapshot(advanced, AgentRunState.Running)
    }
    return true
}

private fun buildStreamingTextBlock(
    stage: StreamStageSpec,
    fullText: String,
    sliceIndex: Int,
    totalSlices: Int,
    stageElapsedMs: Long,
): AgentStreamBlock {
    val visibleSlice = sliceIndex.coerceIn(1, totalSlices)
    val charCount = ((fullText.length * visibleSlice) / totalSlices).coerceAtLeast(1)
    val partial = fullText.take(charCount)
    return if (stage is StreamStageSpec.ThinkStage) {
        AgentStreamBlock.Thinking(id = stage.id, text = partial, status = StreamRunStatus.Running, durationMillis = stageElapsedMs)
    } else {
        AgentStreamBlock.Narration(id = stage.id, text = partial, isStreaming = true)
    }
}

private suspend fun runToolChunksStage(
    stage: StreamStageSpec.ToolStage,
    initial: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean {
    val startFraction = (initial.currentChunkOffset.toFloat() / stage.totalChunks).coerceIn(0f, 1f)
    val startDone = (startFraction * stage.totalItems).roundToInt().coerceAtMost(stage.totalItems)
    val startTool =
        AgentStreamBlock.ToolCall(
            id = stage.id,
            toolName = stage.toolName,
            command = stage.command,
            status = StreamRunStatus.Running,
            summary = stage.runningSummary(startDone, stage.totalItems),
            logLines = initial.activeLogLines,
            progress = startFraction,
            durationMillis = initial.currentStageElapsedMs,
            resumed = initial.currentStageIndex in initial.resumedStages,
        )
    val started = initial.copy(activeBlock = startTool)
    callbacks.writeRecord(started)
    callbacks.publishSnapshot(started, AgentRunState.Running)

    var outcome: Boolean? = null
    while (outcome == null) {
        val current = callbacks.readRecord()
        if (current == null) {
            outcome = false
        } else if (current.currentChunkOffset >= stage.totalChunks) {
            outcome = commitCompletedToolStage(stage, current, callbacks)
        } else if (stage.autoFaultAtChunk != null && !current.autoFaultTriggered && current.currentChunkOffset == stage.autoFaultAtChunk) {
            val failed = current.withIoFault(markAutoFault = true)
            callbacks.writeRecord(failed)
            callbacks.publishSnapshot(failed, AgentRunState.Failed)
            outcome = false
        } else {
            outcome = advanceToolChunk(stage, current, callbacks)
        }
    }
    return outcome
}

private suspend fun advanceToolChunk(
    stage: StreamStageSpec.ToolStage,
    current: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean? {
    val delayMs = throttledDelay(stage.chunkDelayMillis, current.throttled)
    delay(delayMs)
    val latest = callbacks.readRecord() ?: return false
    val nextChunk = latest.currentChunkOffset + 1
    val nextStageElapsed = latest.currentStageElapsedMs + delayMs
    val newLine = stage.logLineAtChunk(nextChunk, stage.totalChunks)
    val nextLogs = if (newLine == null) latest.activeLogLines else latest.activeLogLines + newLine
    val withThermal =
        if (stage.thermalNoteAtChunk == nextChunk) {
            latest.appendNotice(DeviceEnvironmentNote(DeviceNoteSeverity.Info, UiText.Resource(Res.string.longtask_note_thermal)))
        } else {
            latest
        }
    val fraction = (nextChunk.toFloat() / stage.totalChunks).coerceIn(0f, 1f)
    val doneItems = (fraction * stage.totalItems).roundToInt().coerceAtMost(stage.totalItems)
    val activeTool =
        AgentStreamBlock.ToolCall(
            id = stage.id,
            toolName = stage.toolName,
            command = stage.command,
            status = StreamRunStatus.Running,
            summary = stage.runningSummary(doneItems, stage.totalItems),
            logLines = nextLogs,
            progress = fraction,
            durationMillis = nextStageElapsed,
            resumed = withThermal.currentStageIndex in withThermal.resumedStages,
        )
    val progressed =
        withThermal.copy(
            currentChunkOffset = nextChunk,
            currentStageElapsedMs = nextStageElapsed,
            activeLogLines = nextLogs,
            elapsedMillis = withThermal.elapsedMillis + delayMs,
            activeBlock = activeTool,
        )
    callbacks.writeRecord(progressed)
    callbacks.publishSnapshot(progressed, AgentRunState.Running)
    return null
}

private fun commitCompletedToolStage(
    stage: StreamStageSpec.ToolStage,
    current: TaskRunRecord,
    callbacks: StageLoopCallbacks,
): Boolean {
    val completedTool =
        AgentStreamBlock.ToolCall(
            id = stage.id,
            toolName = stage.toolName,
            command = stage.command,
            status = StreamRunStatus.Completed,
            summary = stage.completedSummary,
            logLines = current.activeLogLines,
            progress = 1f,
            durationMillis = current.currentStageElapsedMs,
            resumed = current.currentStageIndex in current.resumedStages,
        )
    val advanced =
        current.copy(
            committedBlocks = current.committedBlocks + completedTool,
            activeBlock = null,
            activeLogLines = emptyList(),
            currentStageIndex = current.currentStageIndex + 1,
            currentChunkOffset = 0,
            currentStageElapsedMs = 0L,
        )
    callbacks.writeRecord(advanced)
    if (advanced.currentStageIndex < advanced.scenario.stages.size) {
        callbacks.publishSnapshot(advanced, AgentRunState.Running)
    }
    return true
}

private fun throttledDelay(
    baseMillis: Long,
    throttled: Boolean,
): Long = if (throttled) (baseMillis * THROTTLE_NUMERATOR) / THROTTLE_DENOMINATOR else baseMillis
