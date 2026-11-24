package com.everfold.core.gateway.mock

import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.core.model.SessionMatchSource
import com.everfold.core.model.SessionSearchFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun searchSessionsMatchesTitleAndMessagesAndExcludesActionTrace() =
        runTest {
            val sessions = createSeededSessionsGateway()

            val titleHits = sessions.searchSessions(query = "Gateway").first()
            assertEquals(1, titleHits.size)
            assertEquals("session-gateway-refactor", titleHits.first().summary.id)
            assertEquals(SessionMatchSource.TITLE, titleHits.first().matchSource)

            val messageHits = sessions.searchSessions(query = "MotionDurationScale").first()
            assertEquals(1, messageHits.size)
            assertEquals("session-compose-perf", messageHits.first().summary.id)
            assertEquals(SessionMatchSource.MESSAGE, messageHits.first().matchSource)
            assertNotNull(messageHits.first().matchedItemId)
            assertTrue(messageHits.first().matchedSnippet?.contains("MotionDurationScale") == true)

            val traceOnlyHits = sessions.searchSessions(query = "trace-only-token-99").first()
            assertTrue(traceOnlyHits.isEmpty())
        }

    @Test
    fun searchSessionsFiltersByWorkspaceNodeAndLoopState() =
        runTest {
            val sessions = createSeededSessionsGateway()

            val workspaceHits =
                sessions
                    .searchSessions(
                        query = "",
                        filter = SessionSearchFilter(workspaceName = "Everfold-Kotlin"),
                    ).first()
            assertEquals(
                listOf("session-welcome", "session-compose-perf"),
                workspaceHits.map { it.summary.id },
            )

            val nodeHits =
                sessions
                    .searchSessions(
                        query = "",
                        filter = SessionSearchFilter(nodeName = MockFixtures.localAttribution.nodeName),
                    ).first()
            assertTrue(nodeHits.any { it.summary.id == "session-compose-perf" })

            val combinedHits =
                sessions
                    .searchSessions(
                        query = "权限",
                        filter =
                            SessionSearchFilter(
                                workspaceName = "Spec Research",
                                nodeName = MockFixtures.cloudAttribution.nodeName,
                                loopState = LoopState.FAILED,
                            ),
                    ).first()
            assertEquals(listOf("session-mcp-sandbox"), combinedHits.map { it.summary.id })
        }

    @Test
    fun pinRenameArchiveAndDeleteMutateSessionsAsExpected() =
        runTest {
            val sessions = createSeededSessionsGateway()

            sessions.setSessionPinned("session-compose-perf", pinned = true)
            assertEquals(
                "session-compose-perf",
                sessions.sessions.value
                    .first()
                    .id,
            )

            sessions.renameSession("session-compose-perf", "  重命名后的性能会话  ")
            sessions.renameSession("session-compose-perf", "   ")
            assertEquals(
                "重命名后的性能会话",
                sessions.sessions.value
                    .first()
                    .title,
            )

            sessions.setSessionArchived("session-compose-perf", archived = true)
            val archivedSummary = sessions.sessions.value.first { it.id == "session-compose-perf" }
            assertTrue(archivedSummary.isArchived)
            assertFalse(archivedSummary.isPinned)

            val defaultSearchIds =
                sessions.searchSessions(query = "重命名").first().map { it.summary.id }
            assertTrue(defaultSearchIds.isEmpty())

            val archivedSearchIds =
                sessions
                    .searchSessions(
                        query = "重命名",
                        filter = SessionSearchFilter(includeArchived = true),
                    ).first()
                    .map { it.summary.id }
            assertEquals(listOf("session-compose-perf"), archivedSearchIds)

            val countBeforeSend =
                sessions
                    .observeSession("session-compose-perf")
                    .first()
                    ?.items
                    ?.size
            sessions.sendMessage("session-compose-perf", "归档状态下不应允许发送")
            advanceUntilIdle()
            assertEquals(
                countBeforeSend,
                sessions
                    .observeSession("session-compose-perf")
                    .first()
                    ?.items
                    ?.size,
            )

            sessions.setSessionArchived("session-compose-perf", archived = false)
            assertFalse(
                sessions.sessions.value
                    .first { it.id == "session-compose-perf" }
                    .isArchived,
            )

            sessions.deleteSession("session-compose-perf")
            assertNull(sessions.observeSession("session-compose-perf").first())
            assertFalse(sessions.sessions.value.any { it.id == "session-compose-perf" })
        }

    private suspend fun TestScope.createSeededSessionsGateway(): MockSessionGateway {
        lateinit var sessions: MockSessionGateway
        val authority =
            MockAuthorityGateway(
                stepDelayMs = 0L,
                onConnected = { mode -> sessions.seedIfEmpty(mode) },
                onDisconnected = { sessions.clearAll() },
            )
        sessions =
            MockSessionGateway(
                modeProvider = authority::currentMode,
                loopHostProvider = authority::currentLoopHostName,
                defaultModelIdProvider = { "model-claude-sonnet" },
                scope = this,
                stepDelayMs = 0L,
            )
        authority.connect(
            AuthorityEndpoint(
                mode = ConnectionMode.REMOTE_AUTHORITY,
                address = "wss://authority.example.com",
            ),
        )
        return sessions
    }
}
