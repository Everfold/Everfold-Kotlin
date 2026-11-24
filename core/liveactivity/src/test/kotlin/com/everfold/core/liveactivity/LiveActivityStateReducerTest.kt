package com.everfold.core.liveactivity

import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.core.model.NotificationPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveActivityStateReducerTest {
    @Test
    fun activeStatesInBackgroundStaySilent() {
        val reducer = LiveActivityStateReducer()
        val sessions =
            listOf(
                sampleSummary("s-1", LoopState.PLANNING),
                sampleSummary("s-2", LoopState.EXECUTING_ACTION),
                sampleSummary("s-3", LoopState.PAUSED),
            )

        val fgPlan = reducer.reduce(sessions, isForeground = true, prefs = NotificationPreferences())
        val bgPlan = reducer.reduce(sessions, isForeground = false, prefs = NotificationPreferences())

        assertTrue(fgPlan.eventsToPost.isEmpty())
        assertTrue(bgPlan.eventsToPost.isEmpty())
        assertTrue(bgPlan.sessionIdsToDismiss.isEmpty())
    }

    @Test
    fun completionEmitsSingleNonCapsuleEventWithoutDuplicates() {
        val reducer = LiveActivityStateReducer()
        val running = sampleSummary("s-1", LoopState.EXECUTING_ACTION)
        val completed = running.copy(loopState = LoopState.COMPLETED, lastMessagePreview = "已完成检查")

        reducer.reduce(listOf(running), isForeground = false, prefs = NotificationPreferences())
        val transitionPlan = reducer.reduce(listOf(completed), isForeground = false, prefs = NotificationPreferences())

        assertEquals(1, transitionPlan.eventsToPost.size)
        val event = transitionPlan.eventsToPost.single()
        assertEquals(LiveActivityEventKind.COMPLETED, event.kind)
        assertFalse(event.promoteAsCapsule)
        assertEquals("已完成检查", event.detailText)

        val repeatPlan = reducer.reduce(listOf(completed), isForeground = false, prefs = NotificationPreferences())
        assertTrue(repeatPlan.eventsToPost.isEmpty())
        assertTrue(repeatPlan.sessionIdsToDismiss.isEmpty())
    }

    @Test
    fun attentionTransitionsRespectCapsuleToggleAndDismissOnForeground() {
        val reducer = LiveActivityStateReducer()
        val running = sampleSummary("s-err", LoopState.EXECUTING_ACTION)
        reducer.reduce(listOf(running), isForeground = false, prefs = NotificationPreferences())

        val failed = running.copy(loopState = LoopState.FAILED, lastMessagePreview = "沙箱写入受阻")
        val plan = reducer.reduce(listOf(failed), isForeground = false, prefs = NotificationPreferences())
        assertEquals(1, plan.eventsToPost.size)
        assertEquals(LiveActivityEventKind.FAILED, plan.eventsToPost.single().kind)
        assertTrue(plan.eventsToPost.single().promoteAsCapsule)

        val fgDismissPlan = reducer.reduce(listOf(failed), isForeground = true, prefs = NotificationPreferences())
        assertTrue(fgDismissPlan.eventsToPost.isEmpty())
        assertEquals(setOf("s-err"), fgDismissPlan.sessionIdsToDismiss)

        val noCapsulePrefs = NotificationPreferences(liveActivityEnabled = false)
        val s2Running = sampleSummary("s-input", LoopState.EXECUTING_ACTION)
        reducer.reduce(listOf(s2Running), isForeground = false, prefs = noCapsulePrefs)
        val s2Awaiting = s2Running.copy(loopState = LoopState.AWAITING_INPUT)
        val nonCapsulePlan = reducer.reduce(listOf(s2Awaiting), isForeground = false, prefs = noCapsulePrefs)
        assertEquals(1, nonCapsulePlan.eventsToPost.size)
        assertFalse(nonCapsulePlan.eventsToPost.single().promoteAsCapsule)
    }

    @Test
    fun concurrentSessionsEmitIndependentEventsAndDismissWhenResumedOrArchived() {
        val reducer = LiveActivityStateReducer()
        val s1Running = sampleSummary("s-1", LoopState.EXECUTING_ACTION)
        val s2Running = sampleSummary("s-2", LoopState.PLANNING)
        reducer.reduce(listOf(s1Running, s2Running), isForeground = false, prefs = NotificationPreferences())

        val s1Done = s1Running.copy(loopState = LoopState.COMPLETED)
        val s2Input = s2Running.copy(loopState = LoopState.AWAITING_INPUT)
        val batchPlan = reducer.reduce(listOf(s1Done, s2Input), isForeground = false, prefs = NotificationPreferences())

        assertEquals(setOf("s-1", "s-2"), batchPlan.eventsToPost.map { it.sessionId }.toSet())
        assertFalse(batchPlan.eventsToPost.first { it.sessionId == "s-1" }.promoteAsCapsule)
        assertTrue(batchPlan.eventsToPost.first { it.sessionId == "s-2" }.promoteAsCapsule)

        val s1Archived = s1Done.copy(isArchived = true)
        val s2Resumed = s2Input.copy(loopState = LoopState.EXECUTING_ACTION)
        val dismissPlan = reducer.reduce(listOf(s1Archived, s2Resumed), isForeground = false, prefs = NotificationPreferences())

        assertEquals(setOf("s-1", "s-2"), dismissPlan.sessionIdsToDismiss)
    }

    @Test
    fun alertPreferenceTogglesSuppressDisallowedNotifications() {
        val reducer = LiveActivityStateReducer()
        val s1 = sampleSummary("s-1", LoopState.EXECUTING_ACTION)
        val s2 = sampleSummary("s-2", LoopState.EXECUTING_ACTION)
        val prefs = NotificationPreferences(alertOnNeedsAttention = false, alertOnCompleted = false)

        reducer.reduce(listOf(s1, s2), isForeground = false, prefs = prefs)
        val plan =
            reducer.reduce(
                listOf(
                    s1.copy(loopState = LoopState.FAILED),
                    s2.copy(loopState = LoopState.COMPLETED),
                ),
                isForeground = false,
                prefs = prefs,
            )
        assertTrue(plan.eventsToPost.isEmpty())
    }

    private fun sampleSummary(
        id: String,
        state: LoopState,
    ): AgentSessionSummary =
        AgentSessionSummary(
            id = id,
            title = "会话 $id",
            modelId = "model-claude-sonnet",
            loopState = state,
            loopHostNodeName = "Workstation",
            lastMessagePreview = "处理中",
            updatedAtLabel = "刚刚",
            workspaceName = "Everfold-Kotlin",
        )
}
