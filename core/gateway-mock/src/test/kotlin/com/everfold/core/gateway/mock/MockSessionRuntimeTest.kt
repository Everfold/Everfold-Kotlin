@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.everfold.core.gateway.mock

import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.LoopControlCommand
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceCapability
import com.everfold.core.model.WorkspacePermissionLevel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockSessionRuntimeTest {
    @Test
    fun multiRelayAggregatesNodesAndWorkspacesAndRemovesOnDisconnect() =
        runTest {
            val authority = MockAuthorityGateway(stepDelayMs = 0L)
            assertTrue(authority.connectProfile("profile-nats-cloud"))
            assertTrue(authority.connectProfile("profile-zenoh-p2p"))
            authority.connectLocal()

            val relayIds =
                authority.nodes.value
                    .mapNotNull { it.relayProfileId }
                    .toSet()
            assertEquals(setOf("profile-nats-cloud", "profile-zenoh-p2p", "local"), relayIds)
            assertTrue(authority.nodes.value.all { it.workspaces.isNotEmpty() })

            authority.disconnectProfile("profile-zenoh-p2p")
            val remainingRelayIds =
                authority.nodes.value
                    .mapNotNull { it.relayProfileId }
                    .toSet()
            assertEquals(setOf("profile-nats-cloud", "local"), remainingRelayIds)
        }

    @Test
    fun createSessionBindsContextAndUpdateSessionModelChangesEffort() =
        runTest {
            val context =
                SessionRuntimeContext(
                    relayProfileId = "profile-zenoh-p2p",
                    relayName = "LAN Zenoh P2P",
                    nodeId = "node-zenoh-edge-pi",
                    nodeName = "Edge Pi 5",
                    nodePlatform = "Linux ARM64",
                    workspaceId = "ws-edge-homelab",
                    workspaceName = "Home Lab",
                    workspaceFileScope = "/opt/homelab/services",
                    workspacePermission = WorkspacePermissionLevel.WORKSPACE_WRITE,
                    workspaceCapabilities = persistentSetOf(WorkspaceCapability.SHELL, WorkspaceCapability.FILE_EDIT),
                )
            val sessions =
                MockSessionGateway(
                    modeProvider = { ConnectionMode.REMOTE },
                    loopHostProvider = { "Cloud Authority" },
                    defaultModelIdProvider = { "model-claude-sonnet" },
                    scope = this,
                    stepDelayMs = 0L,
                )

            val sessionId =
                sessions.createSession(
                    modelId = "model-claude-sonnet",
                    reasoningEffort = ReasoningEffort.MAX,
                    runtimeContext = context,
                )
            val initial = sessions.observeSession(sessionId).first()!!
            assertEquals("Edge Pi 5", initial.summary.loopHostNodeName)
            assertEquals("Home Lab", initial.summary.workspaceName)
            assertEquals(ReasoningEffort.MAX, initial.summary.reasoningEffort)
            assertEquals("/opt/homelab/services", initial.summary.runtimeContext?.workspaceFileScope)

            sessions.updateSessionModel(sessionId, "model-gemini-pro", ReasoningEffort.LOW)
            val updated = sessions.observeSession(sessionId).first()!!
            assertEquals("model-gemini-pro", updated.summary.modelId)
            assertEquals(ReasoningEffort.LOW, updated.summary.reasoningEffort)
        }

    @Test
    fun pauseAndResumeLoopWithAndWithoutSteeringMessage() =
        runTest {
            val sessions =
                MockSessionGateway(
                    modeProvider = { ConnectionMode.REMOTE },
                    loopHostProvider = { "Cloud Authority" },
                    defaultModelIdProvider = { "model-claude-sonnet" },
                    scope = this,
                    stepDelayMs = 100L,
                )
            val sessionId = sessions.createSession()
            sessions.sendMessage(sessionId, "检查节点拓扑")
            advanceTimeBy(50L)
            runCurrent()

            sessions.controlLoop(sessionId, LoopControlCommand.Pause)
            assertEquals(
                LoopState.PAUSED,
                sessions
                    .observeSession(sessionId)
                    .first()!!
                    .summary.loopState,
            )

            sessions.controlLoop(sessionId, LoopControlCommand.Resume())
            advanceUntilIdle()
            assertEquals(
                LoopState.COMPLETED,
                sessions
                    .observeSession(sessionId)
                    .first()!!
                    .summary.loopState,
            )

            sessions.sendMessage(sessionId, "第二轮执行")
            advanceTimeBy(50L)
            runCurrent()
            sessions.controlLoop(sessionId, LoopControlCommand.Pause)
            val attachment =
                SessionAttachment(
                    id = "att-1",
                    displayName = "spec.md",
                    mimeType = "text/markdown",
                    sizeBytes = 512L,
                )
            sessions.controlLoop(
                sessionId = sessionId,
                command =
                    LoopControlCommand.Resume(
                        steeringMessage = "改用精简模式",
                        attachments = persistentListOf(attachment),
                    ),
            )
            advanceUntilIdle()

            val detail = sessions.observeSession(sessionId).first()!!
            assertEquals(LoopState.COMPLETED, detail.summary.loopState)
            val steeredMessage = detail.items.filterIsInstance<ObservedItem.UserMessage>().last()
            assertEquals("改用精简模式", steeredMessage.content)
            assertEquals(1, steeredMessage.attachments.size)
        }

    @Test
    fun sendMessageSupportsAttachmentsOnly() =
        runTest {
            val sessions =
                MockSessionGateway(
                    modeProvider = { ConnectionMode.LOCAL_ONLY },
                    loopHostProvider = { "本机 Android" },
                    defaultModelIdProvider = { "model-claude-sonnet" },
                    scope = this,
                    stepDelayMs = 0L,
                )
            val sessionId = sessions.createSession()
            val attachment =
                SessionAttachment(
                    id = "att-img",
                    displayName = "screenshot.png",
                    mimeType = "image/png",
                    sizeBytes = 4096L,
                )
            sessions.sendMessage(
                sessionId = sessionId,
                content = "   ",
                attachments = persistentListOf(attachment),
            )
            advanceUntilIdle()

            val detail = sessions.observeSession(sessionId).first()!!
            assertEquals(LoopState.COMPLETED, detail.summary.loopState)
            assertEquals("screenshot.png", detail.summary.title)
            val userMsg = detail.items.filterIsInstance<ObservedItem.UserMessage>().single()
            assertEquals("", userMsg.content)
            assertEquals(listOf(attachment), userMsg.attachments)
        }
}
