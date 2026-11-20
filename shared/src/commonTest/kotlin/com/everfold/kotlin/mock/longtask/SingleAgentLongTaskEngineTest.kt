package com.everfold.kotlin.mock.longtask

import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.DeviceTaskFault
import com.everfold.kotlin.feature.conversation.StepStatus
import com.everfold.kotlin.mock.MockWorkspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val MIN_LONG_TASK_MS = 48_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SingleAgentLongTaskEngineTest {
    @Test
    fun storageArchiveRunsFullHorizonPausesAtGateAndAppliesSelectedOption() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("深度整理本机 Download 与截图目录")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            advanceUntilIdle()
            val waitingSession = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Waiting, waitingSession.runState)
            val gateTrace = assertNotNull(waitingSession.messages.last().execution)
            assertEquals(4, gateTrace.currentStepIndex)
            assertNotNull(gateTrace.activeGate)

            workspace.taskController.resolveGateOption(sessionId, GATE_OPT_ARCHIVE_ONLY)
            advanceUntilIdle()

            val completed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, completed.runState)
            val finalTrace = assertNotNull(completed.messages.last().execution)
            assertTrue(finalTrace.elapsedMillis >= MIN_LONG_TASK_MS, "Expected >= 48s, actual=${finalTrace.elapsedMillis}")
            assertTrue(finalTrace.steps.all { it.status == StepStatus.Completed })
            assertTrue("未删除" in completed.messages.last().text)
        }

    @Test
    fun midStepStopPreservesSubChunkCheckpointOnResume() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("构建离线文档检索索引")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            advanceTimeBy(12_000L)
            workspace.stop(sessionId)
            val stopped = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Stopped, stopped.runState)
            val stoppedTrace = assertNotNull(stopped.messages.last().execution)
            assertEquals(2, stoppedTrace.currentStepIndex)
            val savedProgress = stoppedTrace.overallProgress
            assertTrue(savedProgress > 0.25f && savedProgress < 0.6f)

            workspace.retry(sessionId)
            advanceTimeBy(1_000L)
            val resumed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Running, resumed.runState)
            val resumedTrace = assertNotNull(resumed.messages.last().execution)
            assertTrue(resumedTrace.overallProgress > savedProgress)
            assertTrue(resumedTrace.steps[1].resumedFromCheckpoint)

            advanceUntilIdle()
            val done = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, done.runState)
            assertTrue(assertNotNull(done.messages.last().execution).elapsedMillis >= MIN_LONG_TASK_MS)
        }

    @Test
    fun waitingGateAcceptsNaturalLanguageConstraintViaSendAndPreservesOrder() =
        longTaskTest { workspace ->
            val sessionId = PAUSED_SAMPLE_CONVERSATION_ID
            assertEquals(AgentRunState.Stopped, workspace.conversations.first { it.id == sessionId }.runState)

            workspace.retry(sessionId)
            advanceUntilIdle()
            assertEquals(AgentRunState.Waiting, workspace.conversations.first { it.id == sessionId }.runState)

            workspace.updateDraft(sessionId, "保留最近 7 天下载的测试包")
            workspace.send(sessionId)
            advanceUntilIdle()

            val completed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, completed.runState)
            assertEquals(4, completed.messages.size)
            assertTrue("保留最近 7 天下载的测试包" in completed.messages.last().text)
        }

    @Test
    fun diagnosticsAutoFaultsOnceAndRecoversFromCheckpointPlusSupportsFaultInjection() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("诊断本机后台耗电与崩溃日志")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            advanceUntilIdle()
            val autoFailed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Failed, autoFailed.runState)
            val failedTrace = assertNotNull(autoFailed.messages.last().execution)
            assertEquals(3, failedTrace.currentStepIndex)
            assertEquals(StepStatus.Completed, failedTrace.steps[0].status)
            assertEquals(StepStatus.Completed, failedTrace.steps[1].status)
            assertEquals(StepStatus.Failed, failedTrace.steps[2].status)

            workspace.retry(sessionId)
            advanceTimeBy(3_000L)
            workspace.taskController.injectFault(sessionId, DeviceTaskFault.PowerSaverThrottle)
            workspace.taskController.injectFault(sessionId, DeviceTaskFault.StorageIoInterrupt)
            assertEquals(AgentRunState.Failed, workspace.conversations.first { it.id == sessionId }.runState)

            workspace.retry(sessionId)
            advanceUntilIdle()
            val finished = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, finished.runState)
            assertTrue(assertNotNull(finished.messages.last().execution).elapsedMillis >= 60_000L)
        }

    @Test
    fun platformBridgePublishesOnlyBuiltInTasksAndHandlesNotificationCommands() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            val bridge = RecordingLongTaskBridge()
            try {
                val workspace = MockWorkspace(enableLongTasks = true, taskBridge = bridge)
                store.put("workspace", workspace)
                runCurrent()
                assertTrue(bridge.published.isEmpty(), "Pre-seeded paused task does not spam notification on startup")

                workspace.updateNewConversationDraft("构建离线文档索引")
                val localId = assertNotNull(workspace.submitNewConversation())
                advanceTimeBy(2_000L)
                runCurrent()
                assertTrue(bridge.published.any { it.conversationId == localId && it.kind == TaskNotificationKind.Running })

                bridge.commandFlow.tryEmit(DeviceTaskCommand.Pause(localId))
                runCurrent()
                assertEquals(AgentRunState.Stopped, workspace.conversations.first { it.id == localId }.runState)
                assertEquals(TaskNotificationKind.Paused, bridge.published.last().kind)

                bridge.commandFlow.tryEmit(DeviceTaskCommand.Resume(localId))
                advanceTimeBy(1_000L)
                runCurrent()
                assertEquals(AgentRunState.Running, workspace.conversations.first { it.id == localId }.runState)

                bridge.commandFlow.tryEmit(DeviceTaskCommand.TimeoutStopAll)
                runCurrent()
                assertEquals(AgentRunState.Stopped, workspace.conversations.first { it.id == localId }.runState)

                val remoteConn = assertNotNull(workspace.connections.add("Cluster", ClusterSetup()))
                workspace.connections.connect(remoteConn)
                advanceUntilIdle()
                workspace.selectNewConversationConnection(remoteConn)
                workspace.updateNewConversationDraft("Remote task")
                val remoteSessionId = assertNotNull(workspace.submitNewConversation())
                advanceTimeBy(5_000L)
                runCurrent()
                assertTrue(bridge.published.none { it.conversationId == remoteSessionId })

                workspace.deleteConversation(localId)
                assertTrue(localId in bridge.dismissedIds)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }

    private class RecordingLongTaskBridge : PlatformLongTaskBridge {
        val commandFlow = MutableSharedFlow<DeviceTaskCommand>(extraBufferCapacity = 8)
        override val commands = commandFlow
        override val pendingOpenConversationId = MutableStateFlow<String?>(null)
        val published = mutableListOf<TaskNotificationPayload>()
        val dismissedIds = mutableListOf<String>()

        override fun publish(payload: TaskNotificationPayload) {
            published += payload
        }

        override fun dismiss(conversationId: String) {
            dismissedIds += conversationId
        }

        override fun consumeOpenConversation(conversationId: String) {
            if (pendingOpenConversationId.value == conversationId) {
                pendingOpenConversationId.value = null
            }
        }
    }

    private fun longTaskTest(block: suspend TestScope.(MockWorkspace) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val workspace = MockWorkspace(enableLongTasks = true)
                store.put("workspace", workspace)
                block(workspace)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
