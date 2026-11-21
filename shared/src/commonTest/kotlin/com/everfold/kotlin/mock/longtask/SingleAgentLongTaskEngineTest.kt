package com.everfold.kotlin.mock.longtask

import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AgentStreamBlock
import com.everfold.kotlin.feature.conversation.DeviceTaskFault
import com.everfold.kotlin.feature.conversation.StreamRunStatus
import com.everfold.kotlin.feature.conversation.TraceSpanKind
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
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MIN_LONG_TASK_MS = 48_000L

@OptIn(ExperimentalCoroutinesApi::class)
class SingleAgentLongTaskEngineTest {
    @Test
    fun storageArchiveStreamsBlocksIncrementallyPausesAtApprovalAndCompletes() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("深度整理本机 Download 与截图目录")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            val initialBlocks =
                workspace.conversations
                    .first { it.id == sessionId }
                    .messages
                    .last()
                    .streamBlocks
            assertEquals(1, initialBlocks.size, "Stream starts with only the initial active block, never pre-listing future tools")
            assertIs<AgentStreamBlock.Thinking>(initialBlocks.single())

            advanceTimeBy(5_000L)
            val midBlocks =
                workspace.conversations
                    .first { it.id == sessionId }
                    .messages
                    .last()
                    .streamBlocks
            assertTrue(midBlocks.any { it is AgentStreamBlock.Thinking && it.status == StreamRunStatus.Completed })
            assertTrue(midBlocks.any { it is AgentStreamBlock.ToolCall && it.toolName == "storage.stat" })
            assertTrue(midBlocks.none { it is AgentStreamBlock.ToolCall && it.toolName == "fs.batchMove" })

            advanceUntilIdle()
            val waitingSession = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Waiting, waitingSession.runState)
            val waitingBlocks = waitingSession.messages.last().streamBlocks
            val pendingApproval = assertIs<AgentStreamBlock.Approval>(waitingBlocks.last())
            assertNull(pendingApproval.resolvedChoice)
            assertTrue(waitingBlocks.none { it is AgentStreamBlock.ToolCall && it.toolName == "fs.batchMove" })

            workspace.taskController.resolveGateOption(sessionId, GATE_OPT_ARCHIVE_ONLY)
            advanceUntilIdle()

            val completed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, completed.runState)
            val finalMessage = completed.messages.last()
            val elapsed = assertNotNull(finalMessage.elapsedMillis)
            assertTrue(elapsed >= MIN_LONG_TASK_MS, "Expected >= 48s, actual=$elapsed")
            val resolvedApproval = finalMessage.streamBlocks.filterIsInstance<AgentStreamBlock.Approval>().single()
            assertNotNull(resolvedApproval.resolvedChoice)
            assertTrue(
                finalMessage.streamBlocks.any {
                    it is AgentStreamBlock.ToolCall && it.toolName == "fs.batchMove" && it.status == StreamRunStatus.Completed
                },
            )
            val summaryBlock = assertIs<AgentStreamBlock.Narration>(finalMessage.streamBlocks.last())
            assertTrue("未删除" in summaryBlock.text)

            val spans = finalMessage.traceSpans
            assertTrue(spans.any { it.kind == TraceSpanKind.LlmRequest && (it.ttftMillis ?: 0L) > 0L })
            assertTrue(
                spans.any {
                    it.kind == TraceSpanKind.ToolCall &&
                        it.title == "fs.batchMove" &&
                        (it.endOffsetMs ?: 0L) > it.startOffsetMs
                },
            )
            assertTrue(spans.zipWithNext().all { (a, b) -> a.startOffsetMs <= b.startOffsetMs })
        }

    @Test
    fun midToolStopPreservesLogsAndSubChunkCheckpointOnResume() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("构建离线文档检索索引")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            advanceTimeBy(14_000L)
            workspace.stop(sessionId)
            val stopped = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Stopped, stopped.runState)
            val stoppedTool =
                assertIs<AgentStreamBlock.ToolCall>(
                    stopped.messages
                        .last()
                        .streamBlocks
                        .last(),
                )
            assertEquals("doc.chunk", stoppedTool.toolName)
            assertEquals(StreamRunStatus.Paused, stoppedTool.status)
            val savedProgress = assertNotNull(stoppedTool.progress)
            assertTrue(savedProgress > 0.1f && savedProgress < 0.9f)
            assertTrue(stoppedTool.logLines.isNotEmpty())

            workspace.retry(sessionId)
            advanceTimeBy(1_500L)
            val resumed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Running, resumed.runState)
            val resumedBlocks = resumed.messages.last().streamBlocks
            assertEquals(1, resumedBlocks.count { it is AgentStreamBlock.ToolCall && it.toolName == "workspace.diff" })
            assertTrue(resumedBlocks.any { it is AgentStreamBlock.SystemNotice })
            val runningTool = assertIs<AgentStreamBlock.ToolCall>(resumedBlocks.last())
            assertEquals("doc.chunk", runningTool.toolName)
            assertTrue(runningTool.resumed)
            assertTrue(assertNotNull(runningTool.progress) > savedProgress)

            advanceUntilIdle()
            val done = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, done.runState)
            assertTrue(assertNotNull(done.messages.last().elapsedMillis) >= MIN_LONG_TASK_MS)
        }

    @Test
    fun waitingApprovalAcceptsComposerReplyLocksPriorTurnAndContinuesInNewTurn() =
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
            val firstTurnApproval = assertIs<AgentStreamBlock.Approval>(completed.messages[1].streamBlocks.last())
            assertNotNull(firstTurnApproval.resolvedChoice)
            val secondTurnSummary =
                assertIs<AgentStreamBlock.Narration>(
                    completed.messages
                        .last()
                        .streamBlocks
                        .last(),
                )
            assertTrue("保留最近 7 天下载的测试包" in secondTurnSummary.text)
        }

    @Test
    fun diagnosticsAutoFaultsOnceRecoversFromCheckpointAndSupportsInlineFaultInjection() =
        longTaskTest { workspace ->
            workspace.updateNewConversationDraft("诊断本机后台耗电与崩溃日志")
            val sessionId = assertNotNull(workspace.submitNewConversation())

            advanceUntilIdle()
            val autoFailed = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Failed, autoFailed.runState)
            val failedBlocks = autoFailed.messages.last().streamBlocks
            val failedTool = assertIs<AgentStreamBlock.ToolCall>(failedBlocks.last())
            assertEquals("trace.analyze", failedTool.toolName)
            assertEquals(StreamRunStatus.Failed, failedTool.status)
            assertTrue(failedBlocks.none { it is AgentStreamBlock.ToolCall && it.toolName == "power.audit" })

            workspace.retry(sessionId)
            advanceTimeBy(3_000L)
            workspace.taskController.injectFault(sessionId, DeviceTaskFault.PowerSaverThrottle)
            workspace.taskController.injectFault(sessionId, DeviceTaskFault.StorageIoInterrupt)
            assertEquals(AgentRunState.Failed, workspace.conversations.first { it.id == sessionId }.runState)

            workspace.retry(sessionId)
            advanceUntilIdle()
            val finished = workspace.conversations.first { it.id == sessionId }
            assertEquals(AgentRunState.Completed, finished.runState)
            assertTrue(assertNotNull(finished.messages.last().elapsedMillis) >= 60_000L)
            val summary =
                assertIs<AgentStreamBlock.Narration>(
                    finished.messages
                        .last()
                        .streamBlocks
                        .last(),
                )
            assertTrue("SyncPrefs.commit()" in summary.text)
            assertTrue(
                finished.messages
                    .last()
                    .traceSpans
                    .any { it.kind == TraceSpanKind.SystemEvent },
            )
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
