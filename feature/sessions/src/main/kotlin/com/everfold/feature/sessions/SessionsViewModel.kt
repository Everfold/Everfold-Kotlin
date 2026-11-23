package com.everfold.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.ChatTextSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: ImmutableList<AgentSessionSummary> = persistentListOf(),
    val activeDetail: AgentSessionDetail? = null,
    val draft: String = "",
    val chatTextSize: ChatTextSize = ChatTextSize.MEDIUM,
    val autoCollapseLongMessages: Boolean = true,
    val showNodeBadgesInline: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionsViewModel(
    private val sessionGateway: SessionGateway,
    settingsStore: AppSettingsStore,
) : ViewModel() {
    private val selectedSessionId = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow("")

    private val activeDetail =
        selectedSessionId.flatMapLatest { id ->
            if (id == null) flowOf(null) else sessionGateway.observeSession(id)
        }

    val uiState: StateFlow<SessionsUiState> =
        combine(
            sessionGateway.sessions,
            activeDetail,
            draft,
            settingsStore.preferences,
        ) { sessions, detail, draftText, prefs ->
            SessionsUiState(
                sessions = sessions,
                activeDetail = detail,
                draft = draftText,
                chatTextSize = prefs.chatTextSize,
                autoCollapseLongMessages = prefs.autoCollapseLongMessages,
                showNodeBadgesInline = prefs.showNodeBadgesInline,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SessionsUiState(),
        )

    fun openSession(sessionId: String) {
        selectedSessionId.value = sessionId
    }

    fun closeSession() {
        selectedSessionId.value = null
    }

    fun createAndOpenSession() {
        viewModelScope.launch {
            val id = sessionGateway.createSession()
            selectedSessionId.value = id
        }
    }

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun sendDraft() {
        val sessionId = selectedSessionId.value ?: return
        val text = draft.value.trim()
        if (text.isEmpty()) return
        draft.value = ""
        viewModelScope.launch {
            sessionGateway.sendMessage(sessionId = sessionId, content = text)
        }
    }

    fun cancelActiveLoop() {
        val sessionId = selectedSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.cancelLoop(sessionId)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
