package com.everfold.kotlin.feature.conversation

import androidx.compose.runtime.Immutable

enum class TraceSpanKind {
    LlmRequest,
    ToolCall,
    UserGate,
    SystemEvent,
}

/** 单条会话执行轨迹的时间跨度节点（LLM 请求/响应、工具调用、人工门控或系统事件）。 */
@Immutable
data class TraceSpan(
    val id: String,
    val kind: TraceSpanKind,
    val title: String,
    val detail: String = "",
    val startOffsetMs: Long,
    val endOffsetMs: Long? = null,
    val ttftMillis: Long? = null,
    val status: StreamRunStatus = StreamRunStatus.Completed,
) {
    fun effectiveEndMs(loopElapsedMs: Long): Long = (endOffsetMs ?: loopElapsedMs).coerceAtLeast(startOffsetMs)

    fun durationMs(loopElapsedMs: Long): Long = (effectiveEndMs(loopElapsedMs) - startOffsetMs).coerceAtLeast(0L)
}

@Immutable
data class AgentLoopTrace(
    val loopIndex: Int,
    val messageId: String,
    val promptPreview: String,
    val runState: AgentRunState,
    val totalDurationMs: Long,
    val spans: List<TraceSpan>,
)

@Immutable
data class SessionTraceSummary(
    val loopCount: Int,
    val totalDurationMs: Long,
    val llmDurationMs: Long,
    val llmCount: Int,
    val toolDurationMs: Long,
    val toolCount: Int,
    val otherDurationMs: Long,
    val loops: List<AgentLoopTrace>,
)

internal fun buildSessionTraceSummary(state: ConversationUiState): SessionTraceSummary {
    val loops = collectLoopTraces(state)
    val totalDurationMs = loops.sumOf { it.totalDurationMs }
    val llmDurationMs =
        loops.sumOf { loop ->
            loop.spans.filter { it.kind == TraceSpanKind.LlmRequest }.sumOf { it.durationMs(loop.totalDurationMs) }
        }
    val llmCount = loops.sumOf { loop -> loop.spans.count { it.kind == TraceSpanKind.LlmRequest } }
    val toolDurationMs =
        loops.sumOf { loop ->
            loop.spans.filter { it.kind == TraceSpanKind.ToolCall }.sumOf { it.durationMs(loop.totalDurationMs) }
        }
    val toolCount = loops.sumOf { loop -> loop.spans.count { it.kind == TraceSpanKind.ToolCall } }
    val otherDurationMs = (totalDurationMs - llmDurationMs - toolDurationMs).coerceAtLeast(0L)
    return SessionTraceSummary(
        loopCount = loops.size,
        totalDurationMs = totalDurationMs,
        llmDurationMs = llmDurationMs,
        llmCount = llmCount,
        toolDurationMs = toolDurationMs,
        toolCount = toolCount,
        otherDurationMs = otherDurationMs,
        loops = loops,
    )
}

private fun collectLoopTraces(state: ConversationUiState): List<AgentLoopTrace> {
    val latestAssistantId = state.messages.lastOrNull { it.role == MessageRole.Assistant }?.id
    var lastUserPrompt = ""
    var loopIndex = 0
    return buildList {
        state.messages.forEach { msg ->
            if (msg.role == MessageRole.User && msg.text.isNotBlank()) {
                lastUserPrompt = msg.text
            } else if (msg.role == MessageRole.Assistant && msg.traceSpans.isNotEmpty()) {
                loopIndex += 1
                val maxSpanEnd = msg.traceSpans.maxOfOrNull { it.endOffsetMs ?: it.startOffsetMs } ?: 0L
                val totalMs = maxOf(msg.elapsedMillis ?: 0L, maxSpanEnd, 1L)
                val loopState = resolveLoopRunState(msg.id == latestAssistantId, state.runState, msg.traceSpans)
                add(
                    AgentLoopTrace(
                        loopIndex = loopIndex,
                        messageId = msg.id,
                        promptPreview = lastUserPrompt,
                        runState = loopState,
                        totalDurationMs = totalMs,
                        spans = msg.traceSpans,
                    ),
                )
            }
        }
    }
}

private fun resolveLoopRunState(
    isLatestAssistant: Boolean,
    sessionRunState: AgentRunState,
    spans: List<TraceSpan>,
): AgentRunState {
    val lastSpan = spans.lastOrNull()
    return when {
        isLatestAssistant && sessionRunState != AgentRunState.Idle -> sessionRunState
        lastSpan?.status == StreamRunStatus.Failed -> AgentRunState.Failed
        lastSpan?.status == StreamRunStatus.Paused -> AgentRunState.Stopped
        spans.any { it.endOffsetMs == null && it.kind == TraceSpanKind.UserGate } -> AgentRunState.Waiting
        spans.any { it.endOffsetMs == null && it.status == StreamRunStatus.Running } -> AgentRunState.Running
        else -> AgentRunState.Completed
    }
}
