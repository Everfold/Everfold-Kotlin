package com.everfold.core.gateway.mock

import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopControlCommand
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicInteger

class MockSessionGateway(
    modeProvider: () -> ConnectionMode,
    private val loopHostProvider: () -> String,
    private val defaultModelIdProvider: () -> String,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stepDelayMs: Long = MockAuthorityGateway.DEFAULT_STEP_DELAY_MS,
) : SessionGateway {
    private val idCounter = AtomicInteger(INITIAL_SESSION_ID)
    private val store = MockSessionStore()

    override val sessions: StateFlow<ImmutableList<AgentSessionSummary>> = store.sessions

    private val loopRunner =
        MockSessionLoopRunner(
            scope = scope,
            modeProvider = modeProvider,
            loopHostProvider = loopHostProvider,
            idCounter = idCounter,
            stepDelayMs = stepDelayMs,
            sessionProvider = store::get,
            onUpdateSession = store::update,
        )

    internal fun syncConnection(mode: ConnectionMode?) {
        if (mode == null) {
            loopRunner.cancelAll()
            store.clear()
        } else if (store.isEmpty()) {
            store.replaceAll(MockFixtures.initialSessions(mode))
        }
    }

    override fun observeSession(sessionId: String): Flow<AgentSessionDetail?> = store.observe(sessionId)

    override fun searchSessions(
        query: String,
        filter: SessionSearchFilter,
    ): Flow<ImmutableList<SessionSearchHit>> =
        store.snapshots.map { current ->
            filterAndSearchSessions(
                sessions = current.values,
                query = query,
                filter = filter,
            )
        }

    override suspend fun createSession(
        title: String,
        modelId: String?,
        reasoningEffort: ReasoningEffort?,
        runtimeContext: SessionRuntimeContext?,
    ): String {
        val id = "session-${idCounter.incrementAndGet()}"
        val resolvedHost = runtimeContext?.nodeName?.ifBlank { null } ?: loopHostProvider()
        val resolvedWorkspace = runtimeContext?.workspaceName?.ifBlank { null } ?: DEFAULT_WORKSPACE_NAME
        val detail =
            AgentSessionDetail(
                summary =
                    AgentSessionSummary(
                        id = id,
                        title = title.ifBlank { "新智能体会话" },
                        modelId = modelId ?: defaultModelIdProvider(),
                        loopState = LoopState.IDLE,
                        loopHostNodeName = resolvedHost,
                        lastMessagePreview = "等待输入指令…",
                        updatedAtLabel = "刚刚",
                        workspaceName = resolvedWorkspace,
                        reasoningEffort = reasoningEffort,
                        runtimeContext = runtimeContext,
                    ),
                items = persistentListOf(),
            )
        store.prepend(detail)
        return id
    }

    override suspend fun sendMessage(
        sessionId: String,
        content: String,
        attachments: ImmutableList<SessionAttachment>,
    ) {
        val existing = store.get(sessionId) ?: return
        val trimmed = content.trim()
        val hasInput = trimmed.isNotEmpty() || attachments.isNotEmpty()
        if (!existing.summary.isArchived && hasInput) {
            if (existing.summary.loopState == LoopState.PAUSED) {
                store.resumeLoop(
                    sessionId = sessionId,
                    command = LoopControlCommand.Resume(steeringMessage = trimmed, attachments = attachments),
                    runner = loopRunner,
                    idCounter = idCounter,
                )
            } else {
                store.appendAndLaunch(
                    sessionId = sessionId,
                    prompt = trimmed,
                    attachments = attachments,
                    runner = loopRunner,
                    idCounter = idCounter,
                )
            }
        }
    }

    override suspend fun controlLoop(
        sessionId: String,
        command: LoopControlCommand,
    ) {
        when (command) {
            LoopControlCommand.Pause -> store.pauseLoop(sessionId = sessionId, runner = loopRunner)
            is LoopControlCommand.Resume -> store.resumeLoop(sessionId, command, loopRunner, idCounter)
            LoopControlCommand.Stop -> store.stopLoop(sessionId = sessionId, runner = loopRunner)
        }
    }

    override suspend fun updateSessionModel(
        sessionId: String,
        modelId: String,
        reasoningEffort: ReasoningEffort?,
    ) {
        store.update(sessionId) { detail ->
            detail.copy(
                summary =
                    detail.summary.copy(
                        modelId = modelId,
                        reasoningEffort = reasoningEffort,
                    ),
            )
        }
    }

    override suspend fun setSessionPinned(
        sessionId: String,
        pinned: Boolean,
    ) {
        store.update(sessionId) { detail ->
            val nextArchived = if (pinned) false else detail.summary.isArchived
            detail.copy(
                summary =
                    detail.summary.copy(
                        isPinned = pinned,
                        isArchived = nextArchived,
                    ),
            )
        }
    }

    override suspend fun renameSession(
        sessionId: String,
        title: String,
    ) {
        val normalized = title.trim()
        if (normalized.isEmpty()) return
        store.update(sessionId) { detail ->
            detail.copy(summary = detail.summary.copy(title = normalized))
        }
    }

    override suspend fun setSessionArchived(
        sessionId: String,
        archived: Boolean,
    ) {
        if (archived) {
            loopRunner.cancel(sessionId)
        }
        store.update(sessionId) { detail ->
            val summary = detail.summary
            val isLoopActive =
                summary.loopState == LoopState.PLANNING ||
                    summary.loopState == LoopState.EXECUTING_ACTION ||
                    summary.loopState == LoopState.PAUSED
            val nextState = if (archived && isLoopActive) LoopState.IDLE else summary.loopState
            val base = if (archived && isLoopActive) detail.finishRunningActions(ActionStatus.FAILED) else detail
            base.copy(
                summary =
                    summary.copy(
                        isArchived = archived,
                        isPinned = if (archived) false else summary.isPinned,
                        loopState = nextState,
                    ),
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        loopRunner.cancel(sessionId)
        store.remove(sessionId)
    }

    private companion object {
        const val INITIAL_SESSION_ID = 100
        const val DEFAULT_WORKSPACE_NAME = "Everfold-Kotlin"
    }
}

private const val MAX_TITLE_LEN = 24

private fun MockSessionStore.appendAndLaunch(
    sessionId: String,
    prompt: String,
    attachments: ImmutableList<SessionAttachment>,
    runner: MockSessionLoopRunner,
    idCounter: AtomicInteger,
) {
    val userItem =
        ObservedItem.UserMessage(
            id = "u-${idCounter.incrementAndGet()}",
            content = prompt,
            timestampLabel = "刚刚",
            attachments = attachments,
        )
    val preview = buildUserMessagePreview(prompt, attachments)
    update(sessionId) { detail ->
        val nextTitle = if (detail.items.isEmpty()) preview.take(MAX_TITLE_LEN) else detail.summary.title
        detail.copy(
            summary =
                detail.summary.copy(
                    title = nextTitle,
                    loopState = LoopState.PLANNING,
                    lastMessagePreview = preview,
                    updatedAtLabel = "刚刚",
                ),
            items = (detail.items + userItem).toImmutableList(),
        )
    }
    runner.start(sessionId = sessionId, prompt = prompt, attachmentsCount = attachments.size)
}

private fun MockSessionStore.pauseLoop(
    sessionId: String,
    runner: MockSessionLoopRunner,
) {
    val state = get(sessionId)?.summary?.loopState ?: return
    if (state != LoopState.PLANNING && state != LoopState.EXECUTING_ACTION) return
    runner.setPaused(sessionId = sessionId, paused = true)
    update(sessionId) { detail ->
        detail.copy(
            summary =
                detail.summary.copy(
                    loopState = LoopState.PAUSED,
                    lastMessagePreview = "执行循环已暂停",
                    updatedAtLabel = "刚刚",
                ),
        )
    }
}

private fun MockSessionStore.resumeLoop(
    sessionId: String,
    command: LoopControlCommand.Resume,
    runner: MockSessionLoopRunner,
    idCounter: AtomicInteger,
) {
    val existing = get(sessionId) ?: return
    if (existing.summary.isArchived || existing.summary.loopState != LoopState.PAUSED) return
    val trimmed = command.steeringMessage?.trim().orEmpty()
    val attachments = command.attachments

    if (trimmed.isEmpty() && attachments.isEmpty()) {
        runner.setPaused(sessionId = sessionId, paused = false)
        update(sessionId) { detail ->
            detail.copy(
                summary =
                    detail.summary.copy(
                        loopState = LoopState.EXECUTING_ACTION,
                        updatedAtLabel = "刚刚",
                    ),
            )
        }
        if (!runner.isActive(sessionId)) {
            runner.start(sessionId = sessionId, prompt = "继续执行当前任务", attachmentsCount = 0)
        }
    } else {
        val userItem =
            ObservedItem.UserMessage(
                id = "u-${idCounter.incrementAndGet()}",
                content = trimmed,
                timestampLabel = "刚刚",
                attachments = attachments,
            )
        val preview = buildUserMessagePreview(trimmed, attachments)
        update(sessionId) { detail ->
            detail.finishRunningActions(ActionStatus.SUCCEEDED).copy(
                summary =
                    detail.summary.copy(
                        loopState = LoopState.PLANNING,
                        lastMessagePreview = preview,
                        updatedAtLabel = "刚刚",
                    ),
                items = (detail.items + userItem).toImmutableList(),
            )
        }
        runner.start(sessionId = sessionId, prompt = trimmed, attachmentsCount = attachments.size)
    }
}

private fun MockSessionStore.stopLoop(
    sessionId: String,
    runner: MockSessionLoopRunner,
) {
    runner.cancel(sessionId)
    update(sessionId) { detail ->
        detail.finishRunningActions(ActionStatus.FAILED).copy(
            summary =
                detail.summary.copy(
                    loopState = LoopState.IDLE,
                    lastMessagePreview = "已停止当前执行循环",
                    updatedAtLabel = "刚刚",
                ),
        )
    }
}

private fun buildUserMessagePreview(
    text: String,
    attachments: List<SessionAttachment>,
): String = text.ifBlank { attachments.firstOrNull()?.displayName ?: "已附带 ${attachments.size} 个文件" }
