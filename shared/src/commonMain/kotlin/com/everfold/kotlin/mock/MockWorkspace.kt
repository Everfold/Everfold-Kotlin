package com.everfold.kotlin.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ChatMessage
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.DRAFT_CONVERSATION_ID
import com.everfold.kotlin.feature.conversation.DeviceTaskController
import com.everfold.kotlin.feature.conversation.MessageRole
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.TraceSpan
import com.everfold.kotlin.feature.conversation.TraceSpanKind
import com.everfold.kotlin.mock.longtask.DeviceTaskCommand
import com.everfold.kotlin.mock.longtask.NoopLongTaskBridge
import com.everfold.kotlin.mock.longtask.OnDeviceScenarioCatalog
import com.everfold.kotlin.mock.longtask.PlatformLongTaskBridge
import com.everfold.kotlin.mock.longtask.SingleAgentLongTaskEngine
import com.everfold.kotlin.mock.longtask.publishBridgeSnapshot
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.just_now
import everfold.shared.generated.resources.new_conversation_hint
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.reply_complete
import everfold.shared.generated.resources.reply_stopped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ActiveRunStates = setOf(AgentRunState.Running, AgentRunState.Waiting)
private const val STREAM_INITIAL_DELAY_MILLIS = 350L
private const val STREAM_CHAR_STEP = 3
private const val STREAM_STEP_DELAY_MILLIS = 45L

/** 内存模拟工作区驱动界面演示，可选启用单智能体本机实时流式长程任务引擎与平台通知桥接。 */
internal class MockWorkspace(
    private val enableLongTasks: Boolean = false,
    externalScope: CoroutineScope? = null,
    private val taskBridge: PlatformLongTaskBridge = NoopLongTaskBridge,
) : ViewModel(),
    Workspace {
    private val runtimeScope: CoroutineScope = externalScope ?: viewModelScope
    override var initialized by mutableStateOf(false)
        private set
    override val connections = MockConnections(runtimeScope, ::stopConnectionSessions)

    override fun completeSetup(setup: ClusterSetup?) {
        if (initialized) return
        if (setup != null) {
            val id = connections.add(setup.ip.ifBlank { "Beamlet" }, setup, autoConnect = true)
            if (id != null) {
                connections.connect(id)
                selectNewConversationConnection(id)
            }
        }
        initialized = true
    }

    override val conversationOptions = MockConversationOptions(runtimeScope, connections, ::read, ::update)

    override var conversations by mutableStateOf(
        sampleConversations(includePausedLongTask = enableLongTasks).map(MockConversationCatalog::initialize),
    )
        private set
    var replyText: String = "Preview reply"

    private var nextId = 0
    private val replies = mutableMapOf<String, Job>()
    private val longTaskEngine =
        SingleAgentLongTaskEngine(
            scope = runtimeScope,
            readConversation = ::read,
            updateConversation = ::update,
            onTaskStateChanged = { record, id, state ->
                publishBridgeSnapshot(taskBridge, record, id, state)
            },
        )

    override val taskController: DeviceTaskController
        get() = if (enableLongTasks) longTaskEngine else DeviceTaskController.Noop

    init {
        if (enableLongTasks) {
            longTaskEngine.seedRecord(OnDeviceScenarioCatalog.preseededPausedStorageRun())
            if (taskBridge !== NoopLongTaskBridge) {
                runtimeScope.launch {
                    taskBridge.commands.collect { command ->
                        when (command) {
                            is DeviceTaskCommand.Pause -> {
                                stop(command.conversationId)
                            }

                            is DeviceTaskCommand.Resume -> {
                                retry(command.conversationId)
                            }

                            is DeviceTaskCommand.ResolveGateOption -> {
                                longTaskEngine.resolveGateOption(command.conversationId, command.optionId)
                            }

                            is DeviceTaskCommand.ReplyGateConstraint -> {
                                val text = command.constraintText.trim()
                                if (text.isNotEmpty()) {
                                    updateDraft(command.conversationId, text)
                                    send(command.conversationId)
                                }
                            }

                            DeviceTaskCommand.TimeoutStopAll -> {
                                longTaskEngine.activeRunningIds.forEach(::stop)
                            }
                        }
                    }
                }
            }
        }
    }

    override var newConversation by mutableStateOf(
        MockConversationCatalog.initialize(
            ConversationUiState(
                id = DRAFT_CONVERSATION_ID,
                // 新建会话
                title = UiText.Resource(Res.string.new_session),
                // 等待输入
                preview = UiText.Resource(Res.string.new_conversation_hint),
                // 刚刚
                updatedLabel = UiText.Resource(Res.string.just_now),
            ),
        ),
    )
        private set

    override fun updateNewConversationDraft(value: String) {
        newConversation = newConversation.copy(draft = value)
    }

    /** 首次发送时把草稿转为正式会话置顶会话仍排在列表前方 */
    override fun submitNewConversation(): String? {
        val input = newConversation.draft.trim()
        if (!newConversation.hasSendableContent || connections.find(newConversation.connectionId)?.isConnected != true) return null
        val id = "new-${++nextId}"
        val session =
            newConversation.copy(
                id = id,
                title =
                    UiText.Literal(
                        input
                            .ifBlank {
                                newConversation.attachments
                                    .first()
                                    .source.name
                            }.take(24),
                    ),
            )
        conversations = (listOf(session) + conversations).sortedByDescending { it.isPinned }
        newConversation =
            newConversation.copy(draft = "", attachments = emptyList(), attachmentRevision = newConversation.attachmentRevision + 1)
        send(id)
        return id
    }

    override fun renameConversation(
        id: String,
        title: String,
    ) {
        if (title.isBlank()) return
        update(id) { it.copy(title = UiText.Literal(title.trim())) }
    }

    override fun togglePinned(id: String) {
        update(id) { it.copy(isPinned = !it.isPinned) }
        conversations = conversations.sortedByDescending { it.isPinned }
    }

    /** 保留归档状态切换能力当前界面不提供归档入口 */
    fun toggleArchived(id: String) {
        update(id) { it.copy(isArchived = !it.isArchived) }
    }

    override fun deleteConversation(id: String) {
        conversationOptions.attachments.interrupt(id)
        replies.remove(id)?.cancel()
        longTaskEngine.clearRun(id)
        conversations = conversations.filterNot { it.id == id }
    }

    override fun updateDraft(
        id: String,
        value: String,
    ) = update(id) { it.copy(draft = value) }

    override fun selectNewConversationConnection(id: String) {
        if (connections.find(id) != null && id != newConversation.connectionId) {
            conversationOptions.attachments.interrupt(newConversation.id)
            newConversation =
                MockConversationCatalog.initialize(
                    newConversation.copy(
                        connectionId = id,
                        attachments = emptyList(),
                        attachmentRevision =
                            newConversation.attachmentRevision + 1,
                    ),
                )
        }
    }

    private fun stopConnectionSessions(connectionId: String) {
        (conversations + newConversation)
            .filter { it.connectionId == connectionId }
            .forEach { conversationOptions.attachments.interrupt(it.id) }
        conversations
            .filter { it.connectionId == connectionId && it.runState in ActiveRunStates }
            .forEach { stop(it.id) }
    }

    override fun send(id: String) {
        val session = conversations.firstOrNull { it.id == id } ?: return
        val input = session.draft.trim()
        if (!session.hasSendableContent || session.isResponding || connections.find(session.connectionId)?.isConnected != true) return
        val messageId = "message-${++nextId}"
        val attachmentNames = session.attachments.map { it.source.name }
        val resumingWaitingGate = enableLongTasks && session.runState == AgentRunState.Waiting && longTaskEngine.hasResumableRun(id)
        update(id) {
            it.copy(
                preview = UiText.Literal(input.ifBlank { attachmentNames.first() }),
                // 刚刚
                updatedLabel = UiText.Resource(Res.string.just_now),
                draft = "",
                attachments = emptyList(),
                attachmentRevision = it.attachmentRevision + 1,
                runState = AgentRunState.Running,
                messages =
                    it.messages +
                        listOf(
                            ChatMessage("$messageId-user", MessageRole.User, input, attachments = it.attachments, model = it.model),
                            ChatMessage(messageId, MessageRole.Assistant, "", model = session.model),
                        ),
            )
        }
        when {
            resumingWaitingGate -> longTaskEngine.resumeRun(id, newAssistantMessageId = messageId, addedConstraint = input)
            enableLongTasks -> longTaskEngine.startNewRun(id, messageId, session, input, attachmentNames)
            else -> streamReply(id, messageId, replyText)
        }
    }

    override fun retry(id: String) {
        val session = conversations.firstOrNull { it.id == id } ?: return
        val hasTarget = session.workspace != null && session.model != null
        if (session.isResponding || !hasTarget || connections.find(session.connectionId)?.isConnected != true) return
        if (!enableLongTasks || !longTaskEngine.resumeRun(id)) {
            val messageId = "message-${++nextId}"
            val lastPrompt =
                session.messages
                    .lastOrNull { it.role == MessageRole.User }
                    ?.text
                    .orEmpty()
            update(id) {
                it.copy(
                    runState = AgentRunState.Running,
                    messages =
                        it.messages + ChatMessage(messageId, MessageRole.Assistant, "", model = session.model),
                )
            }
            if (enableLongTasks) {
                longTaskEngine.startNewRun(id, messageId, session, lastPrompt, emptyList())
            } else {
                streamReply(id, messageId, replyText)
            }
        }
    }

    /** 切换演示状态前取消模拟回复并移除尚无内容与实时流节点的助手占位消息 */
    fun previewRunState(
        id: String,
        state: AgentRunState,
    ) {
        replies.remove(id)?.cancel()
        val pausedRecord = if (enableLongTasks) longTaskEngine.pauseRun(id) else null
        update(id) {
            it.copy(
                runState = state,
                messages = freezePausedMessages(it.messages, pausedRecord),
            )
        }
    }

    /** 用协程逐段追加示例回复完成后更新状态任务可被停止或删除取消 */
    private fun streamReply(
        id: String,
        messageId: String,
        reply: String,
    ) {
        replies.remove(id)?.cancel()
        val job =
            runtimeScope.launch {
                var elapsed = STREAM_INITIAL_DELAY_MILLIS
                delay(STREAM_INITIAL_DELAY_MILLIS)
                for (length in 1..reply.length step STREAM_CHAR_STEP) {
                    val text = reply.take((length + STREAM_CHAR_STEP - 1).coerceAtMost(reply.length))
                    val liveSpan = replyTraceSpan(messageId, reply.length, endOffsetMs = null, status = StreamRunStatus.Running)
                    update(id) { s ->
                        s.copy(
                            messages =
                                s.messages.map {
                                    if (it.id ==
                                        messageId
                                    ) {
                                        it.copy(text = text, traceSpans = listOf(liveSpan), elapsedMillis = elapsed)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                    elapsed += STREAM_STEP_DELAY_MILLIS
                    delay(STREAM_STEP_DELAY_MILLIS)
                }
                val doneSpan = replyTraceSpan(messageId, reply.length, endOffsetMs = elapsed, status = StreamRunStatus.Completed)
                update(id) { s ->
                    s.copy(
                        runState = AgentRunState.Completed,
                        preview = UiText.Resource(Res.string.reply_complete),
                        messages =
                            s.messages.map {
                                if (it.id == messageId) it.copy(traceSpans = listOf(doneSpan), elapsedMillis = elapsed) else it
                            },
                    )
                }
            }
        replies[id] = job
        job.invokeOnCompletion { if (replies[id] === job) replies.remove(id) }
    }

    private fun replyTraceSpan(
        messageId: String,
        charCount: Int,
        endOffsetMs: Long?,
        status: StreamRunStatus,
    ): TraceSpan =
        TraceSpan(
            id = "$messageId-llm",
            kind = TraceSpanKind.LlmRequest,
            title = "LLM 响应",
            detail = "$charCount chars",
            startOffsetMs = 0L,
            endOffsetMs = endOffsetMs,
            ttftMillis = STREAM_INITIAL_DELAY_MILLIS,
            status = status,
        )

    override fun stop(id: String) {
        replies.remove(id)?.cancel()
        val pausedRecord = if (enableLongTasks) longTaskEngine.pauseRun(id) else null
        update(id) {
            it.copy(
                runState = AgentRunState.Stopped,
                // 已停止
                preview = UiText.Resource(Res.string.reply_stopped),
                messages = freezePausedMessages(it.messages, pausedRecord),
            )
        }
    }

    private fun freezePausedMessages(
        messages: List<ChatMessage>,
        pausedRecord: com.everfold.kotlin.mock.longtask.TaskRunRecord?,
    ): List<ChatMessage> =
        messages
            .map { msg ->
                when {
                    pausedRecord != null && msg.id == pausedRecord.assistantMessageId -> {
                        msg.copy(
                            streamBlocks = pausedRecord.visibleBlocks(),
                            traceSpans = pausedRecord.visibleSpans(),
                            elapsedMillis = pausedRecord.elapsedMillis,
                        )
                    }

                    msg.traceSpans.any { it.endOffsetMs == null } -> {
                        val closedSpans =
                            msg.traceSpans.map { span ->
                                if (span.endOffsetMs == null) {
                                    span.copy(endOffsetMs = msg.elapsedMillis ?: span.startOffsetMs, status = StreamRunStatus.Paused)
                                } else {
                                    span
                                }
                            }
                        msg.copy(traceSpans = closedSpans)
                    }

                    else -> {
                        msg
                    }
                }
            }.filterNot { msg ->
                msg.role == MessageRole.Assistant && msg.text.isEmpty() && msg.streamBlocks.isEmpty()
            }

    private fun read(id: String) = if (id == newConversation.id) newConversation else conversations.find { it.id == id }

    private fun update(
        id: String,
        transform: (ConversationUiState) -> ConversationUiState,
    ) {
        if (id == newConversation.id) {
            newConversation = transform(newConversation)
        } else {
            conversations = conversations.map { if (it.id == id) transform(it) else it }
        }
    }
}
