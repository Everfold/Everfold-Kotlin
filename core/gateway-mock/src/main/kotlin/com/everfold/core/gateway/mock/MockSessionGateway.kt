package com.everfold.core.gateway.mock

import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MockSessionGateway(
    modeProvider: () -> ConnectionMode,
    private val loopHostProvider: () -> String,
    private val defaultModelIdProvider: () -> String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stepDelayMs: Long = MockAuthorityGateway.DEFAULT_STEP_DELAY_MS,
) : SessionGateway {
    private val idCounter = AtomicInteger(INITIAL_SESSION_ID)
    private val activeLoops = ConcurrentHashMap<String, Job>()
    private val store = MockSessionStore()

    override val sessions: StateFlow<ImmutableList<AgentSessionSummary>> = store.sessions

    private val loopRunner =
        MockSessionLoopRunner(
            modeProvider = modeProvider,
            loopHostProvider = loopHostProvider,
            idCounter = idCounter,
            stepDelayMs = stepDelayMs,
            onUpdateSession = store::update,
        )

    internal fun seedIfEmpty(mode: ConnectionMode) {
        if (store.isEmpty()) {
            store.replaceAll(MockFixtures.initialSessions(mode))
        }
    }

    internal fun clearAll() {
        activeLoops.values.forEach { it.cancel() }
        activeLoops.clear()
        store.clear()
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
    ): String {
        val id = "session-${idCounter.incrementAndGet()}"
        val detail =
            AgentSessionDetail(
                summary =
                    AgentSessionSummary(
                        id = id,
                        title = title.ifBlank { "新智能体会话" },
                        modelId = modelId ?: defaultModelIdProvider(),
                        loopState = LoopState.IDLE,
                        loopHostNodeName = loopHostProvider(),
                        lastMessagePreview = "等待输入指令…",
                        updatedAtLabel = "刚刚",
                        workspaceName = DEFAULT_WORKSPACE_NAME,
                    ),
                items = persistentListOf(),
            )
        store.prepend(detail)
        return id
    }

    override suspend fun sendMessage(
        sessionId: String,
        content: String,
    ) {
        val existing = store.get(sessionId)
        val trimmed = content.trim()
        if (existing == null || existing.summary.isArchived || trimmed.isEmpty()) return

        activeLoops.remove(sessionId)?.cancel()
        val userItem =
            ObservedItem.UserMessage(
                id = "u-${idCounter.incrementAndGet()}",
                content = trimmed,
                timestampLabel = "刚刚",
            )
        store.update(sessionId) { detail ->
            val nextTitle =
                if (detail.items.isEmpty()) trimmed.take(MAX_TITLE_LEN) else detail.summary.title
            detail.copy(
                summary =
                    detail.summary.copy(
                        title = nextTitle,
                        loopState = LoopState.PLANNING,
                        lastMessagePreview = trimmed,
                        updatedAtLabel = "刚刚",
                    ),
                items = (detail.items + userItem).toImmutableList(),
            )
        }

        activeLoops[sessionId] =
            scope.launch {
                loopRunner.run(sessionId = sessionId, prompt = trimmed)
                activeLoops.remove(sessionId)
            }
    }

    override suspend fun cancelLoop(sessionId: String) {
        activeLoops.remove(sessionId)?.cancel()
        store.update(sessionId) { detail ->
            detail.copy(
                summary =
                    detail.summary.copy(
                        loopState = LoopState.IDLE,
                        lastMessagePreview = "已停止当前执行循环",
                        updatedAtLabel = "刚刚",
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
        if (archived) activeLoops.remove(sessionId)?.cancel()
        store.update(sessionId) { detail ->
            val summary = detail.summary
            val nextState =
                if (archived && (summary.loopState == LoopState.PLANNING || summary.loopState == LoopState.EXECUTING_ACTION)) {
                    LoopState.IDLE
                } else {
                    summary.loopState
                }
            detail.copy(
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
        activeLoops.remove(sessionId)?.cancel()
        store.remove(sessionId)
    }

    private companion object {
        const val INITIAL_SESSION_ID = 100
        const val MAX_TITLE_LEN = 24
        const val DEFAULT_WORKSPACE_NAME = "Everfold-Kotlin"
    }
}
