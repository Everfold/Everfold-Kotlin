package com.everfold.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionSummary
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SessionsListViewModel(
    sessionGateway: SessionGateway,
) : ViewModel() {
    val sessions: StateFlow<ImmutableList<AgentSessionSummary>> =
        sessionGateway.sessions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = persistentListOf(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
