package com.everfold.kotlin.mock.longtask

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.conversation.AgentRunState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LongTaskNotificationAggregatorTest {
    @Test
    fun stickyPrimaryAvoidsFlickerDuringConcurrentRunningAndPreemptsOnWaiting() {
        val aggregator = LongTaskNotificationAggregator()

        val firstA = samplePayload("conv-a", TaskNotificationKind.Running, progressPercent = 20)
        val update1 = aggregator.onPublish(firstA, english = false)
        val primary1 = assertNotNull(update1.primaryOngoing)
        assertEquals("conv-a", primary1.conversationId)
        assertEquals(1, primary1.concurrentActiveCount)
        assertNull(primary1.concurrentSummary)
        assertEquals("20%", primary1.shortChipLabel)

        val firstB = samplePayload("conv-b", TaskNotificationKind.Running, progressPercent = 10)
        val update2 = aggregator.onPublish(firstB, english = false)
        val primary2 = assertNotNull(update2.primaryOngoing)
        assertEquals("conv-a", primary2.conversationId, "Sticky primary keeps conv-a while both are Running")
        assertEquals(2, primary2.concurrentActiveCount)
        assertEquals("另有 1 个会话任务", primary2.concurrentSummary)
        assertEquals("2·20%", primary2.shortChipLabel)

        val waitingB = samplePayload("conv-b", TaskNotificationKind.Waiting, progressPercent = 50)
        val update3 = aggregator.onPublish(waitingB, english = false)
        val primary3 = assertNotNull(update3.primaryOngoing)
        assertEquals("conv-b", primary3.conversationId, "Waiting task preempts Running task immediately")
        assertEquals("待确认", primary3.shortChipLabel)
        assertEquals("另有 1 个会话任务", primary3.concurrentSummary)
    }

    @Test
    fun completingPrimaryEmitsCompletedEventAndHandsOffOngoingCapsuleToRemainingSession() {
        val aggregator = LongTaskNotificationAggregator()
        aggregator.onPublish(samplePayload("conv-a", TaskNotificationKind.Running, progressPercent = 65), english = false)
        aggregator.onPublish(samplePayload("conv-b", TaskNotificationKind.Running, progressPercent = 40), english = false)

        val completedA = samplePayload("conv-a", TaskNotificationKind.Completed, progressPercent = 100)
        val handoff = aggregator.onPublish(completedA, english = false)

        val completedEvent = assertNotNull(handoff.completedEvent)
        assertEquals("conv-a", completedEvent.conversationId)
        assertEquals(TaskNotificationKind.Completed, completedEvent.kind)

        val nextOngoing = assertNotNull(handoff.primaryOngoing)
        assertEquals("conv-b", nextOngoing.conversationId)
        assertEquals(1, nextOngoing.concurrentActiveCount)
        assertNull(nextOngoing.concurrentSummary)
        assertEquals("40%", nextOngoing.shortChipLabel)

        val finishAll =
            aggregator.onPublish(
                samplePayload("conv-b", TaskNotificationKind.Completed, progressPercent = 100),
                english = false,
            )
        assertNotNull(finishAll.completedEvent)
        assertNull(finishAll.primaryOngoing)
    }

    @Test
    fun bridgePublisherBuildsMultiStageSegmentsAndGateMilestonePoint() {
        val captured = mutableListOf<TaskNotificationPayload>()
        val bridge =
            object : PlatformLongTaskBridge {
                override val commands = MutableSharedFlow<DeviceTaskCommand>()
                override val pendingOpenConversationId = MutableStateFlow<String?>(null)

                override fun publish(payload: TaskNotificationPayload) {
                    captured += payload
                }

                override fun dismiss(conversationId: String) = Unit

                override fun consumeOpenConversation(conversationId: String) = Unit
            }
        val scenario = buildStorageArchiveScenario()
        val record =
            TaskRunRecord(
                taskId = "task-1",
                conversationId = "conv-1",
                connectionId = BUILT_IN_CONNECTION_ID,
                assistantMessageId = "msg-1",
                prompt = "深度整理本机 Download 与截图目录",
                workspacePath = "/storage/emulated/0/Download",
                attachmentNames = emptyList(),
                scenario = scenario,
                currentStageIndex = 2,
                currentChunkOffset = 2,
            )

        publishBridgeSnapshot(bridge, record, "conv-1", AgentRunState.Running)
        val payload = captured.single()
        assertEquals(scenario.stages.size, payload.stageSegments.size)
        assertEquals(PERCENT_HUNDRED, payload.stageSegments.sumOf { it.weight })
        assertTrue(payload.stageSegments[0].reached)
        assertTrue(payload.stageSegments[2].reached)
        assertTrue(!payload.stageSegments.last().reached)
        assertEquals(TaskStageSegmentKind.Tool, payload.activeStageKind)
        val gatePoint = assertNotNull(payload.gatePointPercent)
        assertTrue(gatePoint in 1 until PERCENT_HUNDRED)
        assertTrue(payload.gateOptions.isEmpty())

        val approvalIndex = scenario.stages.indexOfFirst { it is StreamStageSpec.ApprovalStage }
        val waitingRecord = record.copy(currentStageIndex = approvalIndex, currentChunkOffset = 0)
        publishBridgeSnapshot(bridge, waitingRecord, "conv-1", AgentRunState.Waiting)
        val waitingPayload = captured.last()
        assertEquals(2, waitingPayload.gateOptions.size)
        assertEquals(GATE_OPT_CLEAN_ALL, waitingPayload.gateOptions[0].id)
        assertEquals(GATE_OPT_ARCHIVE_ONLY, waitingPayload.gateOptions[1].id)
    }

    private fun samplePayload(
        conversationId: String,
        kind: TaskNotificationKind,
        progressPercent: Int,
    ): TaskNotificationPayload =
        TaskNotificationPayload(
            conversationId = conversationId,
            title = "Task $conversationId",
            body = "Progress $progressPercent%",
            progressPercent = progressPercent,
            kind = kind,
            actionLabel = null,
        )
}
