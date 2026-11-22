package com.everfold.core.gateway.mock

import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockGatewaysTest {
    @Test
    fun connectPopulatesNodesAndWelcomeSession() =
        runTest {
            lateinit var sessions: MockSessionGateway
            val authority =
                MockAuthorityGateway(
                    stepDelayMs = 0L,
                    onConnected = { mode -> sessions.seedIfEmpty(mode) },
                    onDisconnected = { sessions.clearAll() },
                )
            val models = MockModelGateway(hostNameProvider = authority::currentLoopHostName)
            sessions =
                MockSessionGateway(
                    modeProvider = authority::currentMode,
                    loopHostProvider = authority::currentLoopHostName,
                    defaultModelIdProvider = {
                        models.models.value
                            .first()
                            .id
                    },
                    scope = this,
                    stepDelayMs = 0L,
                )

            authority.connect(
                AuthorityEndpoint(
                    mode = ConnectionMode.REMOTE_AUTHORITY,
                    address = "wss://authority.example.com",
                ),
            )

            assertTrue(authority.connectionState.value is ConnectionState.Connected)
            assertTrue(authority.nodes.value.size >= 2)
            assertTrue(sessions.sessions.value.isNotEmpty())
        }

    @Test
    fun sendMessageStreamsActionGroupAndAssistantReply() =
        runTest {
            val authority = MockAuthorityGateway(stepDelayMs = 0L)
            val sessions =
                MockSessionGateway(
                    modeProvider = authority::currentMode,
                    loopHostProvider = authority::currentLoopHostName,
                    defaultModelIdProvider = { "model-claude-sonnet" },
                    scope = this,
                    stepDelayMs = 0L,
                )
            authority.connect(AuthorityEndpoint(mode = ConnectionMode.LOCAL_ONLY))

            val sessionId = sessions.createSession(title = "Test Loop")
            sessions.sendMessage(sessionId, "检查状态")
            advanceUntilIdle()

            val detail = sessions.observeSession(sessionId).first()
            requireNotNull(detail)
            assertEquals(LoopState.COMPLETED, detail.summary.loopState)
            assertTrue(detail.items.any { it is ObservedItem.UserMessage })
            assertTrue(detail.items.any { it is ObservedItem.ActionGroup })
            assertTrue(detail.items.any { it is ObservedItem.AssistantMessage && !it.isStreaming })
        }
}
