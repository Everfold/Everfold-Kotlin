package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.TraceSpan
import com.everfold.kotlin.feature.conversation.TraceSpanKind
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.uiResource
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_constraint_applied
import everfold.shared.generated.resources.longtask_note_io_fault
import everfold.shared.generated.resources.longtask_note_resumed

internal fun TaskRunRecord.withIoFault(markAutoFault: Boolean = false): TaskRunRecord {
    val pct = progressPercent(AgentRunState.Failed)
    val faultLine = "[io-error] chunk lock timed out at $pct% — checkpoint saved"
    val updatedLogs = if (activeBlock is AgentStreamBlock.ToolCall) activeLogLines + faultLine else activeLogLines
    val failedActive =
        when (val block = activeBlock) {
            is AgentStreamBlock.ToolCall -> block.copy(status = StreamRunStatus.Failed, logLines = updatedLogs)
            is AgentStreamBlock.Thinking -> block.copy(status = StreamRunStatus.Failed)
            is AgentStreamBlock.Narration -> block.copy(isStreaming = false)
            else -> block
        }
    val failedSpan = activeSpan?.copy(status = StreamRunStatus.Failed, endOffsetMs = elapsedMillis)
    val note =
        DeviceEnvironmentNote(
            severity = DeviceNoteSeverity.Warning,
            message = uiResource(Res.string.longtask_note_io_fault, pct),
        )
    return copy(
        activeBlock = failedActive,
        activeSpan = failedSpan,
        activeLogLines = updatedLogs,
        autoFaultTriggered = autoFaultTriggered || markAutoFault,
    ).appendNotice(note).appendEventSpan(title = "I/O 中断", detail = "$pct%", status = StreamRunStatus.Failed)
}

internal fun TaskRunRecord.resolveActiveApprovalBlock(addedConstraint: String?): AgentStreamBlock.Approval? {
    val stage = scenario.stages.getOrNull(currentStageIndex) as? StreamStageSpec.ApprovalStage ?: return null
    val trimmed = addedConstraint?.trim().orEmpty()
    val choice =
        if (trimmed.isNotEmpty()) {
            UiText.Literal(trimmed)
        } else {
            stage.gate.options
                .firstOrNull()
                ?.label
        }
    return AgentStreamBlock.Approval(id = stage.id, gate = stage.gate, resolvedChoice = choice)
}

internal fun TaskRunRecord.resolveActiveGateSpan(addedConstraint: String?): TraceSpan? {
    val stage = scenario.stages.getOrNull(currentStageIndex) as? StreamStageSpec.ApprovalStage ?: return null
    val base =
        activeSpan
            ?: TraceSpan(
                id = stage.id,
                kind = TraceSpanKind.UserGate,
                title = "策略确认",
                detail = stage.gate.id,
                startOffsetMs = elapsedMillis,
            )
    val note = addedConstraint?.trim().orEmpty()
    return base.copy(
        detail = if (note.isNotEmpty()) note else base.detail,
        endOffsetMs = elapsedMillis,
        status = StreamRunStatus.Completed,
    )
}

internal fun textStageSpanTitle(stage: StreamStageSpec): String =
    when (stage) {
        is StreamStageSpec.ThinkStage -> "LLM 推理"
        is StreamStageSpec.NarrationStage -> if (stage.isFinalAnswer) "LLM 最终回答" else "LLM 响应"
        else -> "LLM 响应"
    }

internal fun TaskRunRecord.prepareForResume(
    newAssistantMessageId: String?,
    selectedOptionId: String?,
    addedConstraint: String?,
): TaskRunRecord {
    val activeStage = scenario.stages.getOrNull(currentStageIndex)
    return if (activeStage is StreamStageSpec.ApprovalStage) {
        prepareApprovalResume(activeStage, newAssistantMessageId, selectedOptionId, addedConstraint)
    } else {
        val updatedResumed = if (currentChunkOffset > 0) resumedStages + currentStageIndex else resumedStages
        val updatedConstraints = if (!addedConstraint.isNullOrBlank()) userConstraints + addedConstraint.trim() else userConstraints
        val base =
            copy(
                assistantMessageId = newAssistantMessageId ?: assistantMessageId,
                resumedStages = updatedResumed,
                userConstraints = updatedConstraints,
            )
        if (currentStageIndex > 0 || currentChunkOffset > 0) {
            base
                .appendNotice(DeviceEnvironmentNote(DeviceNoteSeverity.Info, UiText.Resource(Res.string.longtask_note_resumed)))
                .appendEventSpan(title = "断点恢复")
        } else {
            base
        }
    }
}

private fun TaskRunRecord.prepareApprovalResume(
    stage: StreamStageSpec.ApprovalStage,
    newAssistantMessageId: String?,
    selectedOptionId: String?,
    addedConstraint: String?,
): TaskRunRecord {
    val chosenOption = stage.gate.options.firstOrNull { it.id == selectedOptionId } ?: stage.gate.options.firstOrNull()
    val chosenId = selectedOptionId ?: chosenOption?.id
    val trimmedConstraint = addedConstraint?.trim().orEmpty()
    val updatedConstraints = if (trimmedConstraint.isNotEmpty()) userConstraints + trimmedConstraint else userConstraints
    val resolvedChoice = if (trimmedConstraint.isNotEmpty()) UiText.Literal(trimmedConstraint) else chosenOption?.label
    val resolvedBlock = AgentStreamBlock.Approval(id = stage.id, gate = stage.gate, resolvedChoice = resolvedChoice)
    val resolvedGateSpan = resolveActiveGateSpan(trimmedConstraint.ifEmpty { chosenId })
    return if (newAssistantMessageId != null) {
        val constraintNote =
            DeviceEnvironmentNote(
                severity = DeviceNoteSeverity.Info,
                message = uiResource(Res.string.longtask_note_constraint_applied, trimmedConstraint),
            )
        copy(
            assistantMessageId = newAssistantMessageId,
            committedBlocks = emptyList(),
            activeBlock = null,
            committedSpans = emptyList(),
            activeSpan = null,
            currentStageIndex = currentStageIndex + 1,
            currentChunkOffset = 0,
            currentStageElapsedMs = 0L,
            approvedGates = approvedGates + stage.gate.id,
            selectedGateOptionId = chosenId ?: selectedGateOptionId,
            userConstraints = updatedConstraints,
        ).appendNotice(constraintNote).appendEventSpan(title = "注入补充约束", detail = trimmedConstraint)
    } else {
        copy(
            committedBlocks = committedBlocks + resolvedBlock,
            activeBlock = null,
            committedSpans = if (resolvedGateSpan != null) committedSpans + resolvedGateSpan else committedSpans,
            activeSpan = null,
            currentStageIndex = currentStageIndex + 1,
            currentChunkOffset = 0,
            currentStageElapsedMs = 0L,
            approvedGates = approvedGates + stage.gate.id,
            selectedGateOptionId = chosenId ?: selectedGateOptionId,
            userConstraints = updatedConstraints,
        )
    }
}
