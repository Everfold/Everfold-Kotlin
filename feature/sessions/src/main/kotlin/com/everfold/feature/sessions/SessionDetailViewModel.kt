package com.everfold.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.ChatTextSize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val sessionId: String? = null,
    val detail: AgentSessionDetail? = null,
    val defaultModelName: String = "",
    val draft: String = "",
    val chatTextSize: ChatTextSize = ChatTextSize.MEDIUM,
    val autoCollapseLongMessages: Boolean = true,
    val expandRunningActionTraces: Boolean = true,
    val expandCompletedActionTraces: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModel(
    private val sessionGateway: SessionGateway,
    modelGateway: ModelGateway,
    settingsStore: AppSettingsStore,
) : ViewModel() {
    private val currentSessionId = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow("")

    private val activeDetail =
        currentSessionId.flatMapLatest { id ->
            if (id == null) flowOf(null) else sessionGateway.observeSession(id)
        }

    val uiState: StateFlow<SessionDetailUiState> =
        combine(
            currentSessionId,
            activeDetail,
            modelGateway.models,
            draft,
            settingsStore.preferences,
        ) { sessionId, detail, models, draftText, prefs ->
            val defaultModel = models.firstOrNull { it.isDefault } ?: models.firstOrNull()
            SessionDetailUiState(
                sessionId = sessionId,
                detail = detail,
                defaultModelName = defaultModel?.displayName.orEmpty(),
                draft = draftText,
                chatTextSize = prefs.chatTextSize,
                autoCollapseLongMessages = prefs.autoCollapseLongMessages,
                expandRunningActionTraces = prefs.expandRunningActionTraces,
                expandCompletedActionTraces = prefs.expandCompletedActionTraces,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SessionDetailUiState(),
        )

    fun bindSession(sessionId: String?) {
        if (currentSessionId.value != sessionId) {
            currentSessionId.value = sessionId
            if (sessionId == null) {
                draft.value = ""
            }
        }
    }

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun sendDraft(onSessionCreate: (String) -> Unit) {
        if (uiState.value.detail
                ?.summary
                ?.isArchived == true
        ) {
            return
        }
        val text = draft.value.trim()
        if (text.isEmpty()) return
        draft.value = ""

        viewModelScope.launch {
            val existingId = currentSessionId.value
            val targetId =
                if (existingId != null) {
                    existingId
                } else {
                    val createdId = sessionGateway.createSession()
                    currentSessionId.value = createdId
                    onSessionCreate(createdId)
                    createdId
                }
            sessionGateway.sendMessage(sessionId = targetId, content = text)
        }
    }

    fun restoreActiveSession() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.setSessionArchived(sessionId = sessionId, archived = false)
        }
    }

    fun cancelActiveLoop() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.cancelLoop(sessionId)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
