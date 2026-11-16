package com.everfold.kotlin.mock

import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ClusterSetup
import com.everfold.kotlin.feature.connections.ConnectionNavigationState
import com.everfold.kotlin.feature.conversation.AgentRunState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionSessionTest {
    @Test
    fun onboardingAddsAConnectionWithoutReplacingTheBuiltInConnection() =
        workspaceTest { workspace ->
            val setup = ClusterSetup(ip = "192.168.1.2", port = "8080")
            workspace.completeSetup(setup)
            val id = workspace.newConversation.connectionId
            assertTrue(id != BUILT_IN_CONNECTION_ID)
            assertEquals(
                setup,
                workspace.connections
                    .find(id)
                    ?.profile
                    ?.setup,
            )
            assertEquals(
                BUILT_IN_CONNECTION_ID,
                workspace.connections.connections
                    .first()
                    .id,
            )
            advanceUntilIdle()
            assertTrue(workspace.connections.connections.all { it.isConnected })
            workspace.completeSetup(setup)
            assertEquals(2, workspace.connections.connections.size)
        }

    @Test
    fun selectingAndBrowsingConnectionsNeverReassignsExistingSessions() =
        workspaceTest { workspace ->
            val remote = assertNotNull(workspace.connections.add("Office", ClusterSetup()))
            workspace.connections.connect(remote)
            val navigation = ConnectionNavigationState()
            navigation.open(remote)
            navigation.add()
            assertTrue(navigation.back())
            assertEquals(remote, navigation.selectedId)
            assertTrue(navigation.back())
            assertNull(navigation.selectedId)
            assertFalse(navigation.back())
            advanceUntilIdle()
            workspace.selectNewConversationConnection(remote)
            workspace.updateNewConversationDraft("Remote question")
            workspace.replyText = "Reply"
            val id = assertNotNull(workspace.submitNewConversation())
            workspace.selectNewConversationConnection(BUILT_IN_CONNECTION_ID)
            assertEquals(remote, workspace.conversations.first { it.id == id }.connectionId)
            assertEquals(BUILT_IN_CONNECTION_ID, workspace.conversations.first { it.id == "welcome" }.connectionId)
            advanceUntilIdle()
        }

    @Test
    fun offlineTargetsKeepDraftsAndCannotSubmitOrSilentlyFallBack() =
        workspaceTest { workspace ->
            val remote = assertNotNull(workspace.connections.add("Office", ClusterSetup()))
            workspace.selectNewConversationConnection(remote)
            workspace.updateNewConversationDraft("Keep this draft")
            val count = workspace.conversations.size
            workspace.replyText = "Reply"
            assertNull(workspace.submitNewConversation())
            workspace.connections.remove(remote)
            workspace.replyText = "Reply"
            assertNull(workspace.submitNewConversation())
            assertEquals(count, workspace.conversations.size)
            assertEquals(remote, workspace.newConversation.connectionId)
            assertEquals("Keep this draft", workspace.newConversation.draft)
        }

    @Test
    fun disconnectStopsOnlyBoundSessionsAndRemovingPreservesTheirHistory() =
        workspaceTest { workspace ->
            val remote = assertNotNull(workspace.connections.add("Office", ClusterSetup()))
            workspace.connections.connect(remote)
            advanceUntilIdle()
            workspace.selectNewConversationConnection(remote)
            workspace.updateNewConversationDraft("Remote question")
            workspace.replyText = "Remote reply is streaming"
            val remoteSession = assertNotNull(workspace.submitNewConversation())
            workspace.updateDraft("welcome", "Local question")
            workspace.replyText = "Local reply continues"
            workspace.send("welcome")
            advanceTimeBy(400)
            workspace.updateDraft(remoteSession, "Next question")
            workspace.connections.disconnect(remote)
            val stopped = workspace.conversations.first { it.id == remoteSession }
            assertEquals(AgentRunState.Stopped, stopped.runState)
            assertEquals("Next question", stopped.draft)
            advanceUntilIdle()
            assertEquals(AgentRunState.Completed, workspace.conversations.first { it.id == "welcome" }.runState)
            workspace.connections.remove(remote)
            assertEquals(stopped, workspace.conversations.first { it.id == remoteSession })
            workspace.replyText = "Must not send"
            workspace.retry(remoteSession)
            workspace.replyText = "Must not send"
            workspace.send(remoteSession)
            assertEquals(stopped, workspace.conversations.first { it.id == remoteSession })
        }

    private fun workspaceTest(block: suspend TestScope.(MockWorkspace) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                block(MockWorkspace())
            } finally {
                Dispatchers.resetMain()
            }
        }
}
