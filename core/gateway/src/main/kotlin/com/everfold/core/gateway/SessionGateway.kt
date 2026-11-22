package com.everfold.core.gateway

import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SessionGateway {
    val sessions: StateFlow<ImmutableList<AgentSessionSummary>>

    fun observeSession(sessionId: String): Flow<AgentSessionDetail?>

    suspend fun createSession(
        title: String = "",
        modelId: String? = null,
    ): String

    suspend fun sendMessage(
        sessionId: String,
        content: String,
    )

    suspend fun cancelLoop(sessionId: String)
}
