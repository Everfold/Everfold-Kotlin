package com.everfold.core.liveactivity

import com.everfold.core.model.ActionKind
import com.everfold.core.model.ActionStatus
import com.everfold.core.model.ActionTrace
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.core.model.NotificationPreferences
import com.everfold.core.model.ObservedItem
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveActivityStateReducerTest {
    @Test
    fun capsuleShowsOnlyWhenBackgroundedAndDismissesOnForeground() {
        val reducer = LiveActivityStateReducer()
        val running = sampleSummary(id = "s-1", state = LoopState.EXECUTING_ACTION)

        val foregroundPlan =
            reducer.reduce(
                sessions = listOf(running),
                primaryDetail = sampleDetail(running),
                isForeground = true,
                prefs = NotificationPreferences(),
            )
        assertNull(foregroundPlan.capsule)
        assertTrue(foregroundPlan.alerts.isEmpty())

        val backgroundPlan =
            reducer.reduce(
                sessions = listOf(running),
                primaryDetail = sampleDetail(running),
                isForeground = false,
                prefs = NotificationPreferences(),
            )
        assertNotNull(backgroundPlan.capsule)
        assertEquals("s-1", backgroundPlan.capsule!!.sessionId)
        assertEquals(LiveActivityPhase.EXECUTING, backgroundPlan.capsule.phase)
        assertEquals(1, backgroundPlan.capsule.completedSteps)
        assertEquals(2, backgroundPlan.capsule.totalSteps)

        val returnedPlan =
            reducer.reduce(
                sessions = listOf(running),
                primaryDetail = sampleDetail(running),
                isForeground = true,
                prefs = NotificationPreferences(),
            )
        assertNull(returnedPlan.capsule)
    }

    @Test
    fun terminalTransitionRetainsCapsuleAndEmitsSingleAlertInBackground() {
        val reducer = LiveActivityStateReducer()
        val running = sampleSummary(id = "s-1", state = LoopState.EXECUTING_ACTION)
        val completed = running.copy(loopState = LoopState.COMPLETED, lastMessagePreview = "已完成")

        reducer.reduce(listOf(running), null, isForeground = false, NotificationPreferences())
        val transitionPlan = reducer.reduce(listOf(completed), null, isForeground = false, NotificationPreferences())

        assertEquals(1, transitionPlan.alerts.size)
        assertEquals(LiveActivityPhase.COMPLETED, transitionPlan.alerts.first().phase)
        assertEquals(LiveActivityPhase.COMPLETED, transitionPlan.capsule?.phase)

        val subsequentPlan = reducer.reduce(listOf(completed), null, isForeground = false, NotificationPreferences())
        assertTrue(subsequentPlan.alerts.isEmpty())
        assertEquals(LiveActivityPhase.COMPLETED, subsequentPlan.capsule?.phase)

        reducer.markSessionOpened("s-1")
        val afterOpenPlan = reducer.reduce(listOf(completed), null, isForeground = false, NotificationPreferences())
        assertNull(afterOpenPlan.capsule)
    }

    @Test
    fun errorAndInterventionRespectPreferencesAndPrioritySelectsExecutingFirst() {
        val reducer = LiveActivityStateReducer()
        val paused = sampleSummary(id = "s-paused", state = LoopState.PAUSED)
        val planning = sampleSummary(id = "s-plan", state = LoopState.PLANNING)
        val executing = sampleSummary(id = "s-exec", state = LoopState.EXECUTING_ACTION)

        assertEquals("s-exec", reducer.selectPrimarySessionId(listOf(paused, planning, executing)))

        val initialPlan = reducer.reduce(listOf(paused, planning, executing), null, false, NotificationPreferences())
        assertEquals("s-exec", initialPlan.capsule?.sessionId)
        assertEquals(2, initialPlan.capsule?.otherActiveCount)

        val suppressedPrefs = NotificationPreferences(liveActivityEnabled = false, alertOnNeedsAttention = false)
        val failed = executing.copy(loopState = LoopState.FAILED)
        val suppressedPlan = reducer.reduce(listOf(failed), null, false, suppressedPrefs)
        assertNull(suppressedPlan.capsule)
        assertTrue(suppressedPlan.alerts.isEmpty())
    }

    @Test
    fun awaitingInputEmitsAttentionAlertWhenEnabled() {
        val reducer = LiveActivityStateReducer()
        val running = sampleSummary(id = "s-gw", state = LoopState.EXECUTING_ACTION)
        reducer.reduce(listOf(running), null, isForeground = true, NotificationPreferences())

        val awaiting = running.copy(loopState = LoopState.AWAITING_INPUT)
        val plan = reducer.reduce(listOf(awaiting), null, isForeground = false, NotificationPreferences())
        assertEquals(1, plan.alerts.size)
        assertEquals(LiveActivityPhase.AWAITING_INPUT, plan.alerts.first().phase)
        assertEquals(LiveActivityPhase.AWAITING_INPUT, plan.capsule?.phase)
    }

    private fun sampleSummary(
        id: String,
        state: LoopState,
    ): AgentSessionSummary =
        AgentSessionSummary(
            id = id,
            title = "测试会话",
            modelId = "model-claude-sonnet",
            loopState = state,
            loopHostNodeName = "Workstation",
            lastMessagePreview = "处理中",
            updatedAtLabel = "刚刚",
            workspaceName = "Everfold-Kotlin",
        )

    private fun sampleDetail(summary: AgentSessionSummary): AgentSessionDetail =
        AgentSessionDetail(
            summary = summary,
            items =
                persistentListOf(
                    ObservedItem.ActionGroup(
                        id = "ag-1",
                        actions =
                            persistentListOf(
                                ActionTrace("a1", ActionKind.MCP_CALL, "MCP · git.status", "done", ActionStatus.SUCCEEDED),
                                ActionTrace("a2", ActionKind.LLM_REQUEST, "LLM · synthesize", "running", ActionStatus.RUNNING),
                            ),
                    ),
                ),
        )
}
