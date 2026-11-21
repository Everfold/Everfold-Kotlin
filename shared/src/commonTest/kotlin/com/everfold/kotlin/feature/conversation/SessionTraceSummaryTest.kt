package com.everfold.kotlin.feature.conversation

import com.everfold.kotlin.ui.localization.UiText
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionTraceSummaryTest {
    @Test
    fun formatTraceDurationHandlesMillisecondsSecondsAndMinutes() {
        assertEquals("0ms", formatTraceDuration(0L))
        assertEquals("350ms", formatTraceDuration(350L))
        assertEquals("1.5s", formatTraceDuration(1_500L))
        assertEquals("56.5s", formatTraceDuration(56_500L))
        assertEquals("1m5s", formatTraceDuration(65_000L))
    }

    @Test
    fun buildSessionTraceSummaryAggregatesLoopsLlmAndToolTimings() {
        val liveSpan =
            TraceSpan(
                id = "t2",
                kind = TraceSpanKind.LlmRequest,
                title = "LLM 最终回答",
                startOffsetMs = 0L,
                endOffsetMs = null,
                ttftMillis = 300L,
                status = StreamRunStatus.Running,
            )
        val state =
            ConversationUiState(
                id = "conv-1",
                title = UiText.Literal("Trace sample"),
                preview = UiText.Literal(""),
                updatedLabel = UiText.Literal("now"),
                runState = AgentRunState.Running,
                messages =
                    listOf(
                        ChatMessage("u1", MessageRole.User, "Scan downloads"),
                        ChatMessage("a1", MessageRole.Assistant, "", traceSpans = sampleLoopOneSpans(), elapsedMillis = 7_500L),
                        ChatMessage("u2", MessageRole.User, "Keep recent APKs"),
                        ChatMessage("a2", MessageRole.Assistant, "", traceSpans = listOf(liveSpan), elapsedMillis = 1_500L),
                    ),
            )

        val summary = buildSessionTraceSummary(state)
        assertEquals(2, summary.loopCount)
        assertEquals(9_000L, summary.totalDurationMs)
        assertEquals(2, summary.llmCount)
        assertEquals(3_500L, summary.llmDurationMs)
        assertEquals(1, summary.toolCount)
        assertEquals(5_000L, summary.toolDurationMs)
        assertEquals(500L, summary.otherDurationMs)
        assertEquals(AgentRunState.Completed, summary.loops[0].runState)
        assertEquals(AgentRunState.Running, summary.loops[1].runState)
        assertEquals("Keep recent APKs", summary.loops[1].promptPreview)
    }

    private fun sampleLoopOneSpans(): List<TraceSpan> =
        listOf(
            TraceSpan("t1", TraceSpanKind.LlmRequest, "LLM 推理", startOffsetMs = 0L, endOffsetMs = 2_000L, ttftMillis = 250L),
            TraceSpan("tool1", TraceSpanKind.ToolCall, "fs.walk", "walk /Download", startOffsetMs = 2_000L, endOffsetMs = 7_000L),
            TraceSpan("evt1", TraceSpanKind.SystemEvent, "断点恢复", startOffsetMs = 7_000L, endOffsetMs = 7_000L),
        )
}
