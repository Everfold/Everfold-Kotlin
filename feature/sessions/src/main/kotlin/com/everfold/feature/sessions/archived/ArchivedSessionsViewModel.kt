package com.everfold.feature.sessions.archived

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedSessionsViewModel(
    private val sessionGateway: SessionGateway,
) : ViewModel() {
    val archivedSessions: StateFlow<ImmutableList<AgentSessionSummary>> =
        sessionGateway.sessions
            .map { list -> list.filter { it.isArchived }.toImmutableList() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = persistentListOf(),
            )

    fun restoreSession(sessionId: String) {
        viewModelScope.launch {
            sessionGateway.setSessionArchived(sessionId = sessionId, archived = false)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionGateway.deleteSession(sessionId)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
