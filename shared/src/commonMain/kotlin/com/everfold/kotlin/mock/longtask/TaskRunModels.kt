package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.WaitingGate
import com.everfold.kotlin.ui.localization.UiText
import kotlin.math.roundToInt

internal const val PERCENT_HUNDRED = 100
internal const val DEFAULT_TEXT_SLICES = 10
internal const val SUMMARY_TEXT_SLICES = 24
private const val COMPLETE_PROGRESS = 1f

internal enum class ScenarioKind {
    StorageArchive,
    Diagnostics,
    DocumentIndex,
    GenericWorkspace,
}

internal sealed interface StreamStageSpec {
    val id: String
    val totalUnits: Int

    data class ThinkStage(
        override val id: String,
        val thought: String,
        val durationMillis: Long,
        override val totalUnits: Int = DEFAULT_TEXT_SLICES,
    ) : StreamStageSpec

    data class NarrationStage(
        override val id: String,
        val durationMillis: Long,
        override val totalUnits: Int = DEFAULT_TEXT_SLICES,
        val textBuilder: (TaskRunContext) -> String,
    ) : StreamStageSpec

    data class ToolStage(
        override val id: String,
        val toolName: String,
        val command: String,
        val totalChunks: Int,
        val chunkDelayMillis: Long,
        val totalItems: Int = totalChunks,
        val runningSummary: (Int, Int) -> UiText,
        val completedSummary: UiText,
        val logLineAtChunk: (Int, Int) -> String? = { _, _ -> null },
        val autoFaultAtChunk: Int? = null,
        val thermalNoteAtChunk: Int? = null,
    ) : StreamStageSpec {
        override val totalUnits: Int get() = totalChunks
    }

    data class ApprovalStage(
        override val id: String,
        val gate: WaitingGate,
    ) : StreamStageSpec {
        override val totalUnits: Int get() = 1
    }
}

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
    val stages: List<StreamStageSpec>,
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
    val committedBlocks: List<AgentStreamBlock> = emptyList(),
    val activeBlock: AgentStreamBlock? = null,
    val currentStageIndex: Int = 0,
    val currentChunkOffset: Int = 0,
    val currentStageElapsedMs: Long = 0L,
    val activeLogLines: List<String> = emptyList(),
    val resumedStages: Set<Int> = emptySet(),
    val approvedGates: Set<String> = emptySet(),
    val selectedGateOptionId: String? = null,
    val userConstraints: List<String> = emptyList(),
    val elapsedMillis: Long = 0L,
    val throttled: Boolean = false,
    val autoFaultTriggered: Boolean = false,
    val noticeCounter: Int = 0,
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

    fun visibleBlocks(): List<AgentStreamBlock> = if (activeBlock == null) committedBlocks else committedBlocks + activeBlock

    fun overallProgress(runState: AgentRunState): Float {
        if (runState == AgentRunState.Completed) return COMPLETE_PROGRESS
        val total = scenario.stages.size.coerceAtLeast(1)
        val completed = currentStageIndex.coerceIn(0, total)
        val stage = scenario.stages.getOrNull(currentStageIndex)
        val stageFraction =
            if (stage == null || stage.totalUnits <= 0) {
                0f
            } else {
                (currentChunkOffset.toFloat() / stage.totalUnits).coerceIn(0f, COMPLETE_PROGRESS)
            }
        return ((completed + stageFraction) / total).coerceIn(0f, COMPLETE_PROGRESS)
    }

    fun progressPercent(runState: AgentRunState): Int = (overallProgress(runState) * PERCENT_HUNDRED).roundToInt()

    fun activeToolName(): String =
        (scenario.stages.getOrNull(currentStageIndex.coerceIn(0, scenario.stages.lastIndex)) as? StreamStageSpec.ToolStage)
            ?.toolName
            .orEmpty()

    fun appendNotice(note: DeviceEnvironmentNote): TaskRunRecord {
        val nextNoticeId = "$taskId-notice-${noticeCounter + 1}"
        return copy(
            committedBlocks = committedBlocks + AgentStreamBlock.SystemNotice(id = nextNoticeId, note = note),
            noticeCounter = noticeCounter + 1,
        )
    }

    fun withPausedActiveBlock(): TaskRunRecord {
        val paused =
            when (val block = activeBlock) {
                is AgentStreamBlock.Thinking -> block.copy(status = StreamRunStatus.Paused)
                is AgentStreamBlock.ToolCall -> block.copy(status = StreamRunStatus.Paused)
                is AgentStreamBlock.Narration -> block.copy(isStreaming = false)
                else -> block
            }
        return copy(activeBlock = paused)
    }
}
