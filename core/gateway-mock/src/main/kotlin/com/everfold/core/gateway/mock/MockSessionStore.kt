package com.everfold.core.gateway.mock

import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class MockSessionStore {
    private val sessionMap = MutableStateFlow<Map<String, AgentSessionDetail>>(emptyMap())
    private val _sessions = MutableStateFlow<ImmutableList<AgentSessionSummary>>(persistentListOf())
    val sessions: StateFlow<ImmutableList<AgentSessionSummary>> = _sessions.asStateFlow()

    val snapshots: Flow<Map<String, AgentSessionDetail>> = sessionMap

    fun isEmpty(): Boolean = sessionMap.value.isEmpty()

    fun get(sessionId: String): AgentSessionDetail? = sessionMap.value[sessionId]

    fun observe(sessionId: String): Flow<AgentSessionDetail?> = sessionMap.map { it[sessionId] }

    fun replaceAll(items: List<AgentSessionDetail>) {
        sessionMap.value = items.associateBy { it.summary.id }
        publishSummaries()
    }

    fun clear() {
        sessionMap.value = emptyMap()
        _sessions.value = persistentListOf()
    }

    fun prepend(detail: AgentSessionDetail) {
        sessionMap.update { mapOf(detail.summary.id to detail) + it }
        publishSummaries()
    }

    fun update(
        sessionId: String,
        transform: (AgentSessionDetail) -> AgentSessionDetail,
    ) {
        sessionMap.update { current ->
            val existing = current[sessionId] ?: return@update current
            current + (sessionId to transform(existing))
        }
        publishSummaries()
    }

    fun remove(sessionId: String) {
        sessionMap.update { it - sessionId }
        publishSummaries()
    }

    private fun publishSummaries() {
        _sessions.value =
            sessionMap.value.values
                .map { it.summary }
                .sortedByDescending { it.isPinned }
                .toImmutableList()
    }
}
