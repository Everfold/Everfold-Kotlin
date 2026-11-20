package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DeviceEnvironmentNote
import com.everfold.kotlin.feature.conversation.DeviceNoteSeverity
import com.everfold.kotlin.feature.conversation.DeviceTaskController
import com.everfold.kotlin.feature.conversation.DeviceTaskFault
import com.everfold.kotlin.feature.conversation.TaskExecutionTrace
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.longtask_note_throttled
import everfold.shared.generated.resources.reply_complete
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 单智能体本机长程任务驱动器，按会话隔离运行协程并维护分片级 Checkpoint。 */
internal class SingleAgentLongTaskEngine(
    private val scope: CoroutineScope,
    private val readConversation: (String) -> ConversationUiState?,
    private val updateConversation: (String, (ConversationUiState) -> ConversationUiState) -> Unit,
    private val onTaskStateChanged: (TaskRunRecord?, String, AgentRunState) -> Unit = { _, _, _ -> },
) : DeviceTaskController {
    private var nextTaskNumber = 0
    private val records = mutableMapOf<String, TaskRunRecord>()
    private val jobs = mutableMapOf<String, Job>()

    fun seedRecord(record: TaskRunRecord) {
        records[record.conversationId] = record
    }

    val activeRunningIds: List<String>
        get() = jobs.keys.toList()

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
        records[conversationId] =
            existing.prepareForResume(
                newAssistantMessageId = newAssistantMessageId,
                selectedOptionId = selectedOptionId,
                addedConstraint = addedConstraint,
            )
        launchRunLoop(conversationId)
        return true
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
                val throttled =
                    existing.copy(
                        throttled = true,
                        deviceNote =
                            DeviceEnvironmentNote(
                                severity = DeviceNoteSeverity.Warning,
                                message = UiText.Resource(Res.string.longtask_note_throttled),
                            ),
                    )
                records[conversationId] = throttled
                applySnapshotToConversation(throttled, session.runState, streamedText = null)
            }

            DeviceTaskFault.StorageIoInterrupt -> {
                jobs.remove(conversationId)?.cancel()
                val failed = existing.withIoFault()
                records[conversationId] = failed
                applySnapshotToConversation(failed, AgentRunState.Failed, streamedText = null)
            }
        }
    }

    fun pauseRun(conversationId: String): TaskExecutionTrace? {
        jobs.remove(conversationId)?.cancel()
        val existing = records[conversationId] ?: return null
        val trace = existing.toTrace(AgentRunState.Stopped)
        onTaskStateChanged(existing, conversationId, AgentRunState.Stopped)
        return trace
    }

    fun clearRun(conversationId: String) {
        jobs.remove(conversationId)?.cancel()
        records.remove(conversationId)
        onTaskStateChanged(null, conversationId, AgentRunState.Idle)
    }

    private fun launchRunLoop(conversationId: String) {
        jobs.remove(conversationId)?.cancel()
        val initial = records[conversationId] ?: return
        applySnapshotToConversation(initial, AgentRunState.Running, streamedText = null)
        val callbacks =
            StepLoopCallbacks(
                readRecord = { records[conversationId] },
                writeRecord = { records[conversationId] = it },
                publishProgress = ::applySnapshotToConversation,
            )
        val job =
            scope.launch {
                val stepsFinished = runAllScenarioSteps(callbacks)
                if (!stepsFinished) return@launch
                val finishedRecord = streamScenarioReport(callbacks) ?: return@launch
                val fullReport = finishedRecord.scenario.reportBuilder(finishedRecord.toContext())
                records.remove(conversationId)
                applySnapshotToConversation(finishedRecord, AgentRunState.Completed, streamedText = fullReport)
            }
        jobs[conversationId] = job
        job.invokeOnCompletion { if (jobs[conversationId] === job) jobs.remove(conversationId) }
    }

    private fun applySnapshotToConversation(
        record: TaskRunRecord,
        runState: AgentRunState,
        streamedText: String?,
    ) {
        val trace = record.toTrace(runState)
        val activeStepTitle =
            record.scenario.steps
                .getOrNull(record.currentStepIndex.coerceIn(0, record.scenario.steps.lastIndex))
                ?.title
        val preview =
            when (runState) {
                AgentRunState.Completed -> UiText.Resource(Res.string.reply_complete)
                else -> activeStepTitle ?: record.scenario.headline
            }
        updateConversation(record.conversationId) { state ->
            state.copy(
                runState = runState,
                preview = preview,
                messages =
                    state.messages.map { msg ->
                        if (msg.id == record.assistantMessageId) {
                            msg.copy(
                                text = streamedText ?: msg.text,
                                execution = trace,
                            )
                        } else {
                            msg
                        }
                    },
            )
        }
        onTaskStateChanged(
            if (runState ==
                AgentRunState.Completed
            ) {
                record
            } else {
                records[record.conversationId]
            },
            record.conversationId,
            runState,
        )
    }
}
