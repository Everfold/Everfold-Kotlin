package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_constraint_applied
import everfold.shared.generated.resources.longtask_note_io_fault
import everfold.shared.generated.resources.longtask_note_resumed
import everfold.shared.generated.resources.longtask_note_thermal
import kotlinx.coroutines.delay

private const val THROTTLE_NUMERATOR = 8L
private const val THROTTLE_DENOMINATOR = 5L
private const val REPORT_STREAM_SEGMENTS = 25

internal fun TaskRunRecord.withIoFault(markAutoFault: Boolean = false): TaskRunRecord {
    val pct = progressPercent(AgentRunState.Failed)
    return copy(
        pendingIoInterrupt = false,
        autoFaultTriggered = autoFaultTriggered || markAutoFault,
        deviceNote =
            DeviceEnvironmentNote(
                severity = DeviceNoteSeverity.Warning,
                message = uiResource(Res.string.longtask_note_io_fault, pct),
            ),
    )
}

internal fun TaskRunRecord.prepareForResume(
    newAssistantMessageId: String?,
    selectedOptionId: String?,
    addedConstraint: String?,
): TaskRunRecord {
    val activeStep = scenario.steps.getOrNull(currentStepIndex)
    val gateToApprove = activeStep?.gate?.id
    val updatedGates = if (gateToApprove != null) approvedGates + gateToApprove else approvedGates
    val updatedResumed = if (currentChunkOffset > 0) resumedSteps + currentStepIndex else resumedSteps
    val updatedConstraints = if (!addedConstraint.isNullOrBlank()) userConstraints + addedConstraint.trim() else userConstraints
    val note =
        when {
            !addedConstraint.isNullOrBlank() -> {
                DeviceEnvironmentNote(
                    severity = DeviceNoteSeverity.Info,
                    message = uiResource(Res.string.longtask_note_constraint_applied, addedConstraint.trim()),
                )
            }

            currentStepIndex > 0 || currentChunkOffset > 0 -> {
                DeviceEnvironmentNote(
                    severity = DeviceNoteSeverity.Info,
                    message =
                        uiResource(
                            Res.string.longtask_note_resumed,
                            (currentStepIndex + 1).coerceAtMost(scenario.steps.size),
                            scenario.steps.size,
                            progressPercent(AgentRunState.Running),
                        ),
                )
            }

            else -> {
                deviceNote
            }
        }
    return copy(
        assistantMessageId = newAssistantMessageId ?: assistantMessageId,
        approvedGates = updatedGates,
        selectedGateOptionId = selectedOptionId ?: selectedGateOptionId,
        userConstraints = updatedConstraints,
        resumedSteps = updatedResumed,
        pendingIoInterrupt = false,
        deviceNote = note,
    )
}

internal class StepLoopCallbacks(
    val readRecord: () -> TaskRunRecord?,
    val writeRecord: (TaskRunRecord) -> Unit,
    val publishProgress: (TaskRunRecord, AgentRunState, String?) -> Unit,
)

internal suspend fun runAllScenarioSteps(callbacks: StepLoopCallbacks): Boolean {
    var outcome: Boolean? = null
    while (outcome == null) {
        val record = callbacks.readRecord()
        if (record == null) {
            outcome = false
        } else if (record.currentStepIndex >= record.scenario.steps.size) {
            outcome = true
        } else {
            val step = record.scenario.steps[record.currentStepIndex]
            if (step.gate != null && step.gate.id !in record.approvedGates) {
                callbacks.publishProgress(record, AgentRunState.Waiting, null)
                outcome = false
            } else if (!runSingleStepChunks(step, callbacks)) {
                outcome = false
            } else {
                val afterChunks = callbacks.readRecord()
                if (afterChunks == null) {
                    outcome = false
                } else {
                    val advanced = afterChunks.copy(currentStepIndex = afterChunks.currentStepIndex + 1, currentChunkOffset = 0)
                    callbacks.writeRecord(advanced)
                    callbacks.publishProgress(advanced, AgentRunState.Running, null)
                }
            }
        }
    }
    return outcome
}

private suspend fun runSingleStepChunks(
    step: StepSpec,
    callbacks: StepLoopCallbacks,
): Boolean {
    var outcome: Boolean? = null
    while (outcome == null) {
        val current = callbacks.readRecord()
        if (current == null) {
            outcome = false
        } else if (current.currentChunkOffset >= step.totalChunks) {
            outcome = true
        } else {
            val hitAutoFault =
                step.autoFaultAtChunk != null &&
                    !current.autoFaultTriggered &&
                    current.currentChunkOffset == step.autoFaultAtChunk
            if (current.pendingIoInterrupt || hitAutoFault) {
                val failed = current.withIoFault(markAutoFault = hitAutoFault)
                callbacks.writeRecord(failed)
                callbacks.publishProgress(failed, AgentRunState.Failed, null)
                outcome = false
            } else {
                outcome = advanceOneChunk(step, current, callbacks)
            }
        }
    }
    return outcome
}

private suspend fun advanceOneChunk(
    step: StepSpec,
    current: TaskRunRecord,
    callbacks: StepLoopCallbacks,
): Boolean? {
    val delayMs =
        if (current.throttled) {
            (step.chunkDelayMillis * THROTTLE_NUMERATOR) / THROTTLE_DENOMINATOR
        } else {
            step.chunkDelayMillis
        }
    delay(delayMs)
    val latest = callbacks.readRecord() ?: return false
    val stepIndex = latest.currentStepIndex
    val nextChunk = latest.currentChunkOffset + 1
    val nextNote =
        if (step.thermalNoteAtChunk == nextChunk && latest.deviceNote == null) {
            DeviceEnvironmentNote(DeviceNoteSeverity.Info, UiText.Resource(Res.string.longtask_note_thermal))
        } else {
            latest.deviceNote
        }
    val progressed =
        latest.copy(
            currentChunkOffset = nextChunk,
            stepDurations = latest.stepDurations + (stepIndex to ((latest.stepDurations[stepIndex] ?: 0L) + delayMs)),
            elapsedMillis = latest.elapsedMillis + delayMs,
            deviceNote = nextNote,
        )
    callbacks.writeRecord(progressed)
    callbacks.publishProgress(progressed, AgentRunState.Running, null)
    return null
}

internal suspend fun streamScenarioReport(callbacks: StepLoopCallbacks): TaskRunRecord? {
    val startRecord = callbacks.readRecord() ?: return null
    val fullReport = startRecord.scenario.reportBuilder(startRecord.toContext())
    val totalSegments = REPORT_STREAM_SEGMENTS.coerceAtMost(fullReport.length.coerceAtLeast(1))
    val segmentDelay = (startRecord.scenario.streamDurationMillis / totalSegments).coerceAtLeast(1L)
    val lastStepIdx = startRecord.scenario.steps.lastIndex
    var cancelled = false
    for (segment in 1..totalSegments) {
        if (!cancelled) {
            delay(segmentDelay)
            val current = callbacks.readRecord()
            if (current == null) {
                cancelled = true
            } else {
                val charCount = ((fullReport.length * segment) / totalSegments).coerceAtLeast(1)
                val lastStepDuration = (current.stepDurations[lastStepIdx] ?: 0L) + segmentDelay
                val updated =
                    current.copy(
                        elapsedMillis = current.elapsedMillis + segmentDelay,
                        stepDurations = current.stepDurations + (lastStepIdx to lastStepDuration),
                    )
                callbacks.writeRecord(updated)
                callbacks.publishProgress(updated, AgentRunState.Running, fullReport.take(charCount))
            }
        }
    }
    return if (cancelled) null else callbacks.readRecord()
}
