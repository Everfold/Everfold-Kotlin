package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.feature.conversation.DeviceTaskController
import com.everfold.kotlin.feature.conversation.DeviceTaskFault
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.TraceSpan
import com.everfold.kotlin.feature.conversation.TraceSpanKind
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_throttled
import everfold.shared.generated.resources.reply_complete
import everfold.shared.generated.resources.reply_stopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 单智能体本机长程任务驱动器，按时序动态追加实时交错流节点、时间轴轨迹与分片级断点。 */
internal class SingleAgentLongTaskEngine(
    private val scope: CoroutineScope,
    private val readConversation: (String) -> ConversationUiState?,
    private val updateConversation: (String, (ConversationUiState) -> ConversationUiState) -> Unit,
    private val onTaskStateChanged: (TaskRunRecord?, String, AgentRunState) -> Unit = { _, _, _ -> },
) : DeviceTaskController {
    private var nextTaskNumber = 0
    private val records = mutableMapOf<String, TaskRunRecord>()
    private val jobs = mutableMapOf<String, Job>()

    val activeRunningIds: List<String>
        get() = jobs.keys.toList()

    fun seedRecord(record: TaskRunRecord) {
        records[record.conversationId] = record
    }

    fun hasResumableRun(conversationId: String): Boolean = conversationId in records

    fun startNewRun(
        conversationId: String,
        assistantMessageId: String,
        session: ConversationUiState,
        prompt: String,
        attachmentNames: List<String>,
    ) {
        jobs.remove(conversationId)?.cancel()
        val workspacePath = session.workspace?.path ?: "/workspace"
        val scenario =
            OnDeviceScenarioCatalog.selectScenario(
                prompt = prompt,
                connectionId = session.connectionId,
                workspacePath = workspacePath,
                hasAttachments = attachmentNames.isNotEmpty(),
            )
        val firstThink = scenario.stages.firstOrNull() as? StreamStageSpec.ThinkStage
        val initialBlock =
            firstThink?.let {
                AgentStreamBlock.Thinking(id = it.id, text = "", status = StreamRunStatus.Running)
            }
        val initialSpan =
            firstThink?.let {
                val slices = it.totalUnits.coerceAtLeast(1)
                TraceSpan(
                    id = it.id,
                    kind = TraceSpanKind.LlmRequest,
                    title = textStageSpanTitle(it),
                    detail = "${it.thought.length} chars",
                    startOffsetMs = 0L,
                    ttftMillis = (it.durationMillis / slices).coerceAtLeast(1L),
                    status = StreamRunStatus.Running,
                )
            }
        val record =
            TaskRunRecord(
                taskId = "longtask-${++nextTaskNumber}",
                conversationId = conversationId,
                connectionId = session.connectionId,
                assistantMessageId = assistantMessageId,
                prompt = prompt,
                workspacePath = workspacePath,
                attachmentNames = attachmentNames,
                scenario = scenario,
                activeBlock = initialBlock,
                activeSpan = initialSpan,
            )
        records[conversationId] = record
        launchRunLoop(conversationId)
    }

    fun resumeRun(
        conversationId: String,
        newAssistantMessageId: String? = null,
        selectedOptionId: String? = null,
        addedConstraint: String? = null,
    ): Boolean {
        val existing = records[conversationId] ?: return false
        if (newAssistantMessageId != null) {
            lockCompletedApprovalMessage(existing, addedConstraint)
        }
        records[conversationId] =
            existing.prepareForResume(
                newAssistantMessageId = newAssistantMessageId,
                selectedOptionId = selectedOptionId,
                addedConstraint = addedConstraint,
            )
        launchRunLoop(conversationId)
        return true
    }

    private fun lockCompletedApprovalMessage(
        existing: TaskRunRecord,
        addedConstraint: String?,
    ) {
        val resolvedApproval = existing.resolveActiveApprovalBlock(addedConstraint) ?: return
        val lockedBlocks = existing.committedBlocks + resolvedApproval
        val resolvedGateSpan = existing.resolveActiveGateSpan(addedConstraint)
        val lockedSpans =
            if (resolvedGateSpan != null) existing.committedSpans + resolvedGateSpan else existing.committedSpans
        updateConversation(existing.conversationId) { state ->
            state.copy(
                messages =
                    state.messages.map { msg ->
                        if (msg.id == existing.assistantMessageId) {
                            msg.copy(streamBlocks = lockedBlocks, traceSpans = lockedSpans)
                        } else {
                            msg
                        }
                    },
            )
        }
    }

    override fun resolveGateOption(
        conversationId: String,
        optionId: String,
    ) {
        val state = readConversation(conversationId) ?: return
        if (state.runState != AgentRunState.Waiting) return
        resumeRun(conversationId, selectedOptionId = optionId)
    }

    override fun injectFault(
        conversationId: String,
        fault: DeviceTaskFault,
    ) {
        val existing = records[conversationId] ?: return
        val session = readConversation(conversationId) ?: return
        when (fault) {
            DeviceTaskFault.PowerSaverThrottle -> {
                val note = DeviceEnvironmentNote(DeviceNoteSeverity.Warning, UiText.Resource(Res.string.longtask_note_throttled))
                val throttled = existing.copy(throttled = true).appendNotice(note).appendEventSpan(title = "省电节流")
                records[conversationId] = throttled
                applySnapshotToConversation(throttled, session.runState)
            }

            DeviceTaskFault.StorageIoInterrupt -> {
                jobs.remove(conversationId)?.cancel()
                val failed = existing.withIoFault()
                records[conversationId] = failed
                applySnapshotToConversation(failed, AgentRunState.Failed)
            }
        }
    }

    fun pauseRun(conversationId: String): TaskRunRecord? {
        jobs.remove(conversationId)?.cancel()
        val existing = records[conversationId] ?: return null
        val paused = existing.withPausedActiveBlock()
        records[conversationId] = paused
        onTaskStateChanged(paused, conversationId, AgentRunState.Stopped)
        return paused
    }

    fun clearRun(conversationId: String) {
        jobs.remove(conversationId)?.cancel()
        records.remove(conversationId)
        onTaskStateChanged(null, conversationId, AgentRunState.Idle)
    }

    private fun launchRunLoop(conversationId: String) {
        jobs.remove(conversationId)?.cancel()
        val initial = records[conversationId] ?: return
        applySnapshotToConversation(initial, AgentRunState.Running)
        val callbacks =
            StageLoopCallbacks(
                readRecord = { records[conversationId] },
                writeRecord = { records[conversationId] = it },
                publishSnapshot = ::applySnapshotToConversation,
            )
        val job =
            scope.launch {
                val finished = runAllScenarioStages(callbacks)
                if (!finished) return@launch
                val completedRecord = records.remove(conversationId) ?: return@launch
                applySnapshotToConversation(completedRecord, AgentRunState.Completed)
            }
        jobs[conversationId] = job
        job.invokeOnCompletion { if (jobs[conversationId] === job) jobs.remove(conversationId) }
    }

    private fun applySnapshotToConversation(
        record: TaskRunRecord,
        runState: AgentRunState,
    ) {
        val visible = record.visibleBlocks()
        val visibleSpans = record.visibleSpans()
        val preview =
            when (runState) {
                AgentRunState.Completed -> UiText.Resource(Res.string.reply_complete)
                AgentRunState.Stopped -> UiText.Resource(Res.string.reply_stopped)
                else -> (record.activeBlock as? AgentStreamBlock.ToolCall)?.summary ?: record.scenario.headline
            }
        updateConversation(record.conversationId) { state ->
            state.copy(
                runState = runState,
                preview = preview,
                messages =
                    state.messages.map { msg ->
                        if (msg.id == record.assistantMessageId) {
                            msg.copy(
                                streamBlocks = visible,
                                traceSpans = visibleSpans,
                                elapsedMillis = record.elapsedMillis,
                            )
                        } else {
                            msg
                        }
                    },
            )
        }
        val activeOrCompleted = if (runState == AgentRunState.Completed) record else records[record.conversationId]
        onTaskStateChanged(activeOrCompleted, record.conversationId, runState)
    }
}
