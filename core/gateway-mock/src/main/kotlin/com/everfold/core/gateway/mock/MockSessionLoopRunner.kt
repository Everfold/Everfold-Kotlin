package com.everfold.core.gateway.mock

import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger

internal class MockSessionLoopRunner(
    private val modeProvider: () -> ConnectionMode,
    private val loopHostProvider: () -> String,
    private val idCounter: AtomicInteger,
    private val stepDelayMs: Long,
    private val onUpdateSession: (String, (AgentSessionDetail) -> AgentSessionDetail) -> Unit,
) {
    suspend fun run(
        sessionId: String,
        prompt: String,
    ) {
        val isLocal = modeProvider() == ConnectionMode.LOCAL_ONLY
        val mcpNode = if (isLocal) MockFixtures.localAttribution else MockFixtures.desktopAttribution
        val llmNode = if (isLocal) MockFixtures.localAttribution else MockFixtures.cloudAttribution
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
        onUpdateSession(sessionId) { detail ->
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
        onUpdateSession(sessionId) { it.withActionGroup(groupId, persistentListOf(completedMcp, llmAction)) }
        if (stepDelayMs > 0) delay(stepDelayMs)

        streamReply(sessionId, prompt, mcpNode.nodeName, llmNode.nodeName)

        val completedLlm = llmAction.copy(status = ActionStatus.SUCCEEDED, durationMs = LLM_MS)
        onUpdateSession(sessionId) { detail ->
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
            onUpdateSession(sessionId) { it.withAssistantMessage(assistantId, builder.toString(), streaming) }
            if (streaming && stepDelayMs > 0) delay(stepDelayMs)
        }
    }

    private companion object {
        const val SNIPPET_LEN = 16
        const val MCP_MS = 310L
        const val LLM_MS = 540L
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
