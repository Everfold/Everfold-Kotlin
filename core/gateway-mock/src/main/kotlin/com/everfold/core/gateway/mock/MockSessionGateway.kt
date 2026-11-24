package com.everfold.core.gateway.mock

import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MockSessionGateway(
    private val modeProvider: () -> ConnectionMode,
    private val loopHostProvider: () -> String,
    private val defaultModelIdProvider: () -> String,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val stepDelayMs: Long = MockAuthorityGateway.DEFAULT_STEP_DELAY_MS,
) : SessionGateway {
    private val idCounter = AtomicInteger(INITIAL_SESSION_ID)
    private val activeLoops = ConcurrentHashMap<String, Job>()
    private val sessionMap = MutableStateFlow<Map<String, AgentSessionDetail>>(emptyMap())

    private val _sessions = MutableStateFlow<ImmutableList<AgentSessionSummary>>(persistentListOf())
    override val sessions: StateFlow<ImmutableList<AgentSessionSummary>> = _sessions.asStateFlow()

    internal fun seedIfEmpty(mode: ConnectionMode) {
        if (sessionMap.value.isEmpty()) {
            val initial = MockFixtures.initialSessions(mode)
            sessionMap.value = initial.associateBy { it.summary.id }
            _sessions.value =
                sessionMap.value.values
                    .map { it.summary }
                    .toImmutableList()
        }
    }

    internal fun clearAll() {
        activeLoops.values.forEach { it.cancel() }
        activeLoops.clear()
        sessionMap.value = emptyMap()
        _sessions.value = persistentListOf()
    }

    override fun observeSession(sessionId: String): Flow<AgentSessionDetail?> = sessionMap.map { it[sessionId] }

    override fun searchSessions(
        query: String,
        filter: SessionSearchFilter,
    ): Flow<ImmutableList<SessionSearchHit>> =
        sessionMap.map { current ->
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
        sessionMap.update { mapOf(id to detail) + it }
        _sessions.value =
            sessionMap.value.values
                .map { it.summary }
                .toImmutableList()
        return id
    }

    override suspend fun sendMessage(
        sessionId: String,
        content: String,
    ) {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return

        activeLoops.remove(sessionId)?.cancel()
        val userItem =
            ObservedItem.UserMessage(
                id = "u-${idCounter.incrementAndGet()}",
                content = trimmed,
                timestampLabel = "刚刚",
            )
        updateSession(sessionId) { detail ->
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
                runSimulatedLoop(sessionId = sessionId, prompt = trimmed)
            }
    }

    override suspend fun cancelLoop(sessionId: String) {
        activeLoops.remove(sessionId)?.cancel()
        updateSession(sessionId) { detail ->
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

    private suspend fun runSimulatedLoop(
        sessionId: String,
        prompt: String,
    ) {
        val isLocal = modeProvider() == ConnectionMode.LOCAL_ONLY
        val mcpNode =
            if (isLocal) MockFixtures.localAttribution else MockFixtures.desktopAttribution
        val llmNode =
            if (isLocal) MockFixtures.localAttribution else MockFixtures.cloudAttribution
        val groupId = "ag-${idCounter.incrementAndGet()}"

        val mcpAction =
            ActionTrace(
                id = "act-${idCounter.incrementAndGet()}",
                kind = ActionKind.MCP_CALL,
                title = "MCP · context.lookup",
                summary = "检索与「${prompt.take(SNIPPET_LEN)}」相关的上下文",
                status = ActionStatus.RUNNING,
                executedOn = mcpNode,
            )
        updateSession(sessionId) { detail ->
            detail.copy(
                summary = detail.summary.copy(loopState = LoopState.EXECUTING_ACTION),
                items = (detail.items + ObservedItem.ActionGroup(groupId, persistentListOf(mcpAction))).toImmutableList(),
            )
        }
        if (stepDelayMs > 0) delay(stepDelayMs)

        val completedMcp = mcpAction.copy(status = ActionStatus.SUCCEEDED, durationMs = MCP_MS)
        val llmAction =
            ActionTrace(
                id = "act-${idCounter.incrementAndGet()}",
                kind = ActionKind.LLM_REQUEST,
                title = "LLM · synthesize",
                summary = "整合工具观测结果并流式生成回复",
                status = ActionStatus.RUNNING,
                executedOn = llmNode,
            )
        updateSession(sessionId) { it.withActionGroup(groupId, persistentListOf(completedMcp, llmAction)) }
        if (stepDelayMs > 0) delay(stepDelayMs)

        streamReply(sessionId, prompt, mcpNode.nodeName, llmNode.nodeName)

        val completedLlm = llmAction.copy(status = ActionStatus.SUCCEEDED, durationMs = LLM_MS)
        updateSession(sessionId) { detail ->
            detail
                .withActionGroup(groupId, persistentListOf(completedMcp, completedLlm))
                .copy(
                    summary =
                        detail.summary.copy(
                            loopState = LoopState.COMPLETED,
                            lastMessagePreview = "已完成：$prompt",
                            updatedAtLabel = "刚刚",
                        ),
                )
        }
        activeLoops.remove(sessionId)
    }

    private suspend fun streamReply(
        sessionId: String,
        prompt: String,
        mcpNodeName: String,
        llmNodeName: String,
    ) {
        val assistantId = "a-${idCounter.incrementAndGet()}"
        val chunks = buildReplyChunks(prompt, loopHostProvider(), mcpNodeName, llmNodeName)
        val builder = StringBuilder()
        chunks.forEachIndexed { index, chunk ->
            builder.append(chunk)
            val streaming = index < chunks.lastIndex
            updateSession(sessionId) { it.withAssistantMessage(assistantId, builder.toString(), streaming) }
            if (streaming && stepDelayMs > 0) delay(stepDelayMs)
        }
    }

    private fun updateSession(
        sessionId: String,
        transform: (AgentSessionDetail) -> AgentSessionDetail,
    ) {
        sessionMap.update { current ->
            val existing = current[sessionId] ?: return@update current
            current + (sessionId to transform(existing))
        }
        _sessions.value =
            sessionMap.value.values
                .map { it.summary }
                .toImmutableList()
    }

    private companion object {
        const val INITIAL_SESSION_ID = 100
        const val MAX_TITLE_LEN = 24
        const val SNIPPET_LEN = 16
        const val MCP_MS = 310L
        const val LLM_MS = 540L
        const val DEFAULT_WORKSPACE_NAME = "Everfold-Kotlin"
    }
}

private fun buildReplyChunks(
    prompt: String,
    loopHost: String,
    mcpNodeName: String,
    llmNodeName: String,
): List<String> =
    listOf(
        "已收到指令：**$prompt**\n\n",
        "本次执行循环已由 **$loopHost** 协调完成：\n",
        "1. 上下文检索已透明分发至 `$mcpNodeName`。\n",
        "2. 推理与汇总已由 `$llmNodeName` 流式返回。\n\n",
        "```kotlin\n// 未来接入 Kotlin 框架 SDK 后，此处将实时映射真实的 Loop 与 Action 观测流\n```",
    )

private fun AgentSessionDetail.withActionGroup(
    groupId: String,
    actions: ImmutableList<ActionTrace>,
): AgentSessionDetail =
    copy(
        items =
            items
                .map { item ->
                    if (item is ObservedItem.ActionGroup && item.id == groupId) {
                        item.copy(actions = actions)
                    } else {
                        item
                    }
                }.toImmutableList(),
    )

private fun AgentSessionDetail.withAssistantMessage(
    messageId: String,
    markdown: String,
    isStreaming: Boolean,
): AgentSessionDetail {
    val exists = items.any { it.id == messageId }
    val nextItems =
        if (exists) {
            items
                .map { item ->
                    if (item is ObservedItem.AssistantMessage && item.id == messageId) {
                        item.copy(markdown = markdown, isStreaming = isStreaming)
                    } else {
                        item
                    }
                }.toImmutableList()
        } else {
            (
                items +
                    ObservedItem.AssistantMessage(
                        id = messageId,
                        markdown = markdown,
                        isStreaming = isStreaming,
                        timestampLabel = "刚刚",
                    )
            ).toImmutableList()
        }
    return copy(items = nextItems)
}
