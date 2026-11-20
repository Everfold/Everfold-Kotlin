package com.everfold.kotlin.mock

import androidx.lifecycle.ViewModelStore
import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionStatus
import com.everfold.kotlin.feature.conversation.AgentRunState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AgentStateTest {
    @Test
    fun taskFailureDoesNotDisconnectOrStopAnotherSession() {
        val workspace = MockWorkspace()
        workspace.completeSetup(null)
        workspace.previewRunState("welcome", AgentRunState.Failed)
        assertTrue(workspace.connections.find(BUILT_IN_CONNECTION_ID)?.isConnected == true)
        assertEquals(AgentRunState.Failed, workspace.conversations.first { it.id == "welcome" }.runState)
        assertEquals(AgentRunState.Idle, workspace.conversations.first { it.id == "week" }.runState)
    }

    @Test
    fun waitingAndStoppingAreDistinctStates() {
        val workspace = MockWorkspace()
        workspace.previewRunState("welcome", AgentRunState.Waiting)
        val waiting = workspace.conversations.first { it.id == "welcome" }
        assertFalse(waiting.isResponding)
        assertFalse(waiting.responseStopped)
        workspace.stop("welcome")
        assertTrue(workspace.conversations.first { it.id == "welcome" }.responseStopped)
    }

    @Test
    fun retryRequiresConnectedEndpointAndIgnoresUnknownSessions() =
        workspaceTest { workspace ->
            val remote = assertNotNull(workspace.connections.add("Office", ClusterSetup()))
            workspace.connections.connect(remote)
            advanceUntilIdle()
            workspace.selectNewConversationConnection(remote)
            workspace.updateNewConversationDraft("Hello")
            workspace.replyText = "Short reply"
            val sessionId = assertNotNull(workspace.submitNewConversation())
            advanceUntilIdle()

            workspace.connections.disconnect(remote)
            advanceUntilIdle()
            val messageCountAfterDisconnect =
                workspace.conversations
                    .first { it.id == sessionId }
                    .messages.size
            workspace.retry(sessionId)
            workspace.retry("missing-session")
            assertEquals(AgentRunState.Completed, workspace.conversations.first { it.id == sessionId }.runState)
            assertEquals(
                messageCountAfterDisconnect,
                workspace.conversations
                    .first { it.id == sessionId }
                    .messages.size,
            )

            workspace.connections.connect(remote)
            advanceUntilIdle()
            workspace.retry(sessionId)
            assertEquals(AgentRunState.Running, workspace.conversations.first { it.id == sessionId }.runState)
            advanceUntilIdle()
            assertEquals(AgentRunState.Completed, workspace.conversations.first { it.id == sessionId }.runState)
            assertEquals(
                messageCountAfterDisconnect + 1,
                workspace.conversations
                    .first { it.id == sessionId }
                    .messages.size,
            )
        }

    @Test
    fun restartingReplyAndConnectionDoesNotEvictNewerJob() =
        workspaceTest { workspace ->
            workspace.replyText = "A long enough streaming reply for cancellation checks"
            workspace.updateDraft("welcome", "First prompt")
            workspace.send("welcome")
            advanceTimeBy(100)
            workspace.stop("welcome")
            workspace.retry("welcome")
            runCurrent()
            advanceTimeBy(100)
            assertEquals(AgentRunState.Running, workspace.conversations.first { it.id == "welcome" }.runState)
            workspace.stop("welcome")
            advanceUntilIdle()
            assertEquals(AgentRunState.Stopped, workspace.conversations.first { it.id == "welcome" }.runState)

            val remote = assertNotNull(workspace.connections.add("Office", ClusterSetup()))
            workspace.connections.connect(remote)
            advanceTimeBy(200)
            workspace.connections.disconnect(remote)
            workspace.connections.connect(remote)
            runCurrent()
            assertEquals(
                ConnectionStatus.Connecting,
                workspace.connections
                    .find(remote)
                    ?.runtime
                    ?.status,
            )
            workspace.connections.disconnect(remote)
            advanceUntilIdle()
            assertEquals(
                ConnectionStatus.Disconnected,
                workspace.connections
                    .find(remote)
                    ?.runtime
                    ?.status,
            )
        }

    private fun workspaceTest(block: suspend TestScope.(MockWorkspace) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val store = ViewModelStore()
            try {
                val workspace = MockWorkspace()
                store.put("workspace", workspace)
                block(workspace)
            } finally {
                store.clear()
                Dispatchers.resetMain()
            }
        }
}
