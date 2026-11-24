package com.everfold.core.gateway.mock

import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopState
import com.everfold.core.model.NodeAttribution
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionRuntimeContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

internal class MockSessionLoopRunner(
    private val scope: CoroutineScope,
    private val modeProvider: () -> ConnectionMode,
    private val loopHostProvider: () -> String,
    private val idCounter: AtomicInteger,
    private val stepDelayMs: Long,
    private val sessionProvider: (String) -> AgentSessionDetail?,
    private val onUpdateSession: (String, (AgentSessionDetail) -> AgentSessionDetail) -> Unit,
) {
    private val activeLoops = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, MutableStateFlow<Boolean>>()

    fun start(
        sessionId: String,
        prompt: String,
        attachmentsCount: Int,
    ) {
        pauseFlags.getOrPut(sessionId) { MutableStateFlow(false) }.value = false
        activeLoops.remove(sessionId)?.cancel()
        activeLoops[sessionId] =
            scope.launch {
                run(sessionId = sessionId, prompt = prompt, attachmentsCount = attachmentsCount)
                activeLoops.remove(sessionId)
            }
    }

    fun setPaused(
        sessionId: String,
        paused: Boolean,
    ) {
        pauseFlags.getOrPut(sessionId) { MutableStateFlow(false) }.value = paused
    }

    fun isActive(sessionId: String): Boolean = activeLoops[sessionId]?.isActive == true

    fun cancel(sessionId: String) {
        pauseFlags[sessionId]?.value = false
        activeLoops.remove(sessionId)?.cancel()
    }

    fun cancelAll() {
        activeLoops.values.forEach { it.cancel() }
        activeLoops.clear()
        pauseFlags.clear()
    }

    private suspend fun run(
        sessionId: String,
        prompt: String,
        attachmentsCount: Int,
    ) {
        val snapshot = sessionProvider(sessionId)
        val context = snapshot?.summary?.runtimeContext
        val isLocal = context?.relayProfileId == "local" || modeProvider() == ConnectionMode.LOCAL_ONLY
        val hostAttribution = resolveHostAttribution(context = context, isLocal = isLocal)
        val llmNode = if (isLocal) MockFixtures.localAttribution else MockFixtures.cloudAttribution
        val groupId = "ag-${idCounter.incrementAndGet()}"

        val completedMcp = runMcpPhase(sessionId, prompt, groupId, hostAttribution)
        val llmAction = runLlmPreparePhase(sessionId, groupId, completedMcp, llmNode)

        awaitPauseGate(pauseFlags[sessionId])
        val latest = sessionProvider(sessionId)
        streamReply(
            sessionId = sessionId,
            prompt = prompt,
            attachmentsCount = attachmentsCount,
            loopHost = latest?.summary?.loopHostNodeName?.ifBlank { null } ?: loopHostProvider(),
            workspaceName = latest?.summary?.workspaceName?.ifBlank { null } ?: "Default",
            modelId = latest?.summary?.modelId.orEmpty(),
            effort = latest?.summary?.reasoningEffort,
            mcpNodeName = hostAttribution.nodeName,
            llmNodeName = llmNode.nodeName,
        )

        completeLoop(sessionId, prompt, groupId, completedMcp, llmAction)
    }

    private suspend fun runMcpPhase(
        sessionId: String,
        prompt: String,
        groupId: String,
        hostAttribution: NodeAttribution,
    ): ActionTrace {
        awaitPauseGate(pauseFlags[sessionId])
        val mcpAction =
            ActionTrace(
                id = "act-${idCounter.incrementAndGet()}",
                kind = ActionKind.MCP_CALL,
                title = "MCP · context.lookup",
                summary = "检索与「${prompt.ifBlank { "附件" }.take(SNIPPET_LEN)}」相关的上下文",
                status = ActionStatus.RUNNING,
                executedOn = hostAttribution,
            )
        onUpdateSession(sessionId) { detail ->
            detail.copy(
                summary = detail.summary.copy(loopState = LoopState.EXECUTING_ACTION),
                items = (detail.items + ObservedItem.ActionGroup(groupId, persistentListOf(mcpAction))).toImmutableList(),
            )
        }
        if (stepDelayMs > 0) delay(stepDelayMs)
        return mcpAction.copy(status = ActionStatus.SUCCEEDED, durationMs = MCP_MS)
    }

    private suspend fun runLlmPreparePhase(
        sessionId: String,
        groupId: String,
        completedMcp: ActionTrace,
        llmNode: NodeAttribution,
    ): ActionTrace {
        awaitPauseGate(pauseFlags[sessionId])
        val llmAction =
            ActionTrace(
                id = "act-${idCounter.incrementAndGet()}",
                kind = ActionKind.LLM_REQUEST,
                title = "LLM · synthesize",
                summary = "整合工具观测结果并流式生成回复",
                status = ActionStatus.RUNNING,
                executedOn = llmNode,
            )
        onUpdateSession(sessionId) { it.withActionGroup(groupId, persistentListOf(completedMcp, llmAction)) }
        if (stepDelayMs > 0) delay(stepDelayMs)
        return llmAction
    }

    private suspend fun streamReply(
        sessionId: String,
        prompt: String,
        attachmentsCount: Int,
        loopHost: String,
        workspaceName: String,
        modelId: String,
        effort: ReasoningEffort?,
        mcpNodeName: String,
        llmNodeName: String,
    ) {
        val assistantId = "a-${idCounter.incrementAndGet()}"
        val chunks =
            buildReplyChunks(
                prompt = prompt,
                attachmentsCount = attachmentsCount,
                loopHost = loopHost,
                workspaceName = workspaceName,
                modelId = modelId,
                effort = effort,
                mcpNodeName = mcpNodeName,
                llmNodeName = llmNodeName,
            )
        val builder = StringBuilder()
        chunks.forEachIndexed { index, chunk ->
            awaitPauseGate(pauseFlags[sessionId])
            builder.append(chunk)
            val streaming = index < chunks.lastIndex
            onUpdateSession(sessionId) { it.withAssistantMessage(assistantId, builder.toString(), streaming) }
            if (streaming && stepDelayMs > 0) delay(stepDelayMs)
        }
    }

    private suspend fun completeLoop(
        sessionId: String,
        prompt: String,
        groupId: String,
        completedMcp: ActionTrace,
        llmAction: ActionTrace,
    ) {
        awaitPauseGate(pauseFlags[sessionId])
        val completedLlm = llmAction.copy(status = ActionStatus.SUCCEEDED, durationMs = LLM_MS)
        onUpdateSession(sessionId) { detail ->
            detail
                .withActionGroup(groupId, persistentListOf(completedMcp, completedLlm))
                .copy(
                    summary =
                        detail.summary.copy(
                            loopState = LoopState.COMPLETED,
                            lastMessagePreview = "已完成：${prompt.ifBlank { "附件分析" }}",
                            updatedAtLabel = "刚刚",
                        ),
                )
        }
    }

    private companion object {
        const val SNIPPET_LEN = 16
        const val MCP_MS = 310L
        const val LLM_MS = 540L
    }
}

private suspend fun awaitPauseGate(flag: MutableStateFlow<Boolean>?) {
    if (flag != null && flag.value) {
        flag.first { paused -> !paused }
    }
}

private fun resolveHostAttribution(
    context: SessionRuntimeContext?,
    isLocal: Boolean,
): NodeAttribution =
    when {
        context != null && context.nodeName.isNotBlank() -> {
            NodeAttribution(
                nodeId = context.nodeId.ifBlank { "node-runtime" },
                nodeName = context.nodeName,
                platform = context.nodePlatform.ifBlank { "Remote Node" },
            )
        }

        isLocal -> {
            MockFixtures.localAttribution
        }

        else -> {
            MockFixtures.desktopAttribution
        }
    }

private fun buildReplyChunks(
    prompt: String,
    attachmentsCount: Int,
    loopHost: String,
    workspaceName: String,
    modelId: String,
    effort: ReasoningEffort?,
    mcpNodeName: String,
    llmNodeName: String,
): List<String> {
    val promptLine =
        when {
            prompt.isNotBlank() && attachmentsCount > 0 -> "已收到指令：**$prompt**（附带 $attachmentsCount 个文件）\n\n"
            prompt.isNotBlank() -> "已收到指令：**$prompt**\n\n"
            else -> "已收到 $attachmentsCount 个附件并完成解析。\n\n"
        }
    val effortText = effort?.name ?: "Standard"
    val modelLine = if (modelId.isNotBlank()) "- **推理配置**：`$modelId` · `$effortText`\n" else ""
    return listOf(
        promptLine,
        "本次执行循环已由 **$loopHost**（工作空间 `$workspaceName`）协调完成：\n",
        modelLine,
        "1. 上下文检索已透明分发至 `$mcpNodeName`。\n",
        "2. 推理与汇总已由 `$llmNodeName` 流式返回。\n\n",
        "```kotlin\n// 未来接入 Kotlin 框架 SDK 后，此处将实时映射真实的 Loop 与 Action 观测流\n```",
    )
}

internal fun AgentSessionDetail.finishRunningActions(targetStatus: ActionStatus): AgentSessionDetail =
    copy(
        items =
            items
                .map { item ->
                    if (item is ObservedItem.ActionGroup) {
                        val updated =
                            item.actions
                                .map { action ->
                                    if (action.status == ActionStatus.RUNNING) {
                                        action.copy(status = targetStatus)
                                    } else {
                                        action
                                    }
                                }.toImmutableList()
                        item.copy(actions = updated)
                    } else {
                        item
                    }
                }.toImmutableList(),
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
