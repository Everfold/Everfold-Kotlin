package com.everfold.feature.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AppSettingsStore
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.ModelGateway
import com.everfold.core.gateway.SessionGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.LoopControlCommand
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.core.model.SessionRuntimeContext
import com.everfold.core.model.WorkspaceDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SessionDetailViewModel(
    private val sessionGateway: SessionGateway,
    modelGateway: ModelGateway,
    topologyGateway: TopologyGateway,
    authorityGateway: AuthorityGateway,
    settingsStore: AppSettingsStore,
) : ViewModel() {
    private val currentSessionId = MutableStateFlow<String?>(null)
    private val draft = MutableStateFlow("")
    private val draftAttachments = MutableStateFlow<ImmutableList<SessionAttachment>>(persistentListOf())
    private val selectedContext = MutableStateFlow<SessionRuntimeContext?>(null)
    private val selectedModelId = MutableStateFlow<String?>(null)
    private val selectedEffort = MutableStateFlow<ReasoningEffort?>(null)

    private val activeDetail =
        currentSessionId.flatMapLatest { id ->
            if (id == null) flowOf(null) else sessionGateway.observeSession(id)
        }

    private val draftStateFlow =
        combine(draft, draftAttachments) { text, attachments ->
            DraftInputState(text = text, attachments = attachments)
        }

    private val relayConnectionFlow =
        combine(authorityGateway.connectedProfileIds, authorityGateway.isLocalModeConnected) { ids, local ->
            ids.isNotEmpty() || local
        }

    private val environmentStateFlow =
        combine(
            topologyGateway.nodes,
            modelGateway.models,
            relayConnectionFlow,
            combine(selectedContext, selectedModelId, selectedEffort) { ctx, modelId, effort ->
                Triple(ctx, modelId, effort)
            },
        ) { nodes, models, hasRelay, selection ->
            EnvironmentSelectionState(
                nodes = nodes,
                models = models,
                hasConnectedRelay = hasRelay || nodes.isNotEmpty(),
                selectedContext = selection.first,
                selectedModelId = selection.second,
                selectedEffort = selection.third,
            )
        }

    val uiState: StateFlow<SessionDetailUiState> =
        combine(
            currentSessionId,
            activeDetail,
            draftStateFlow,
            environmentStateFlow,
            settingsStore.preferences,
            ::buildSessionDetailUiState,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = SessionDetailUiState(),
        )

    fun bindSession(sessionId: String?) {
        if (currentSessionId.value != sessionId) {
            currentSessionId.value = sessionId
            if (sessionId == null) {
                draft.value = ""
                draftAttachments.value = persistentListOf()
            }
        }
    }

    fun updateDraft(value: String) {
        draft.value = value
    }

    fun addAttachments(items: List<SessionAttachment>) {
        if (items.isEmpty()) return
        draftAttachments.update { current ->
            val existingKeys = current.map { it.uriString.ifBlank { it.id } }.toSet()
            val fresh = items.filter { it.uriString.ifBlank { it.id } !in existingKeys }
            (current + fresh).toImmutableList()
        }
    }

    fun removeAttachment(attachmentId: String) {
        draftAttachments.update { current ->
            current.filterNot { it.id == attachmentId }.toImmutableList()
        }
    }

    fun selectRuntimeContext(
        node: NodeDescriptor,
        workspace: WorkspaceDescriptor?,
    ) {
        if (currentSessionId.value != null) return
        selectedContext.value = toRuntimeContext(node = node, workspace = workspace)
    }

    fun selectModelAndEffort(
        modelId: String,
        effort: ReasoningEffort?,
    ) {
        val model = uiState.value.availableModels.firstOrNull { it.id == modelId }
        val resolvedEffort =
            when {
                model == null || !model.supportsEffort -> null
                effort != null && effort in model.supportedEfforts -> effort
                else -> model.defaultEffort ?: model.supportedEfforts.firstOrNull()
            }
        selectedModelId.value = modelId
        selectedEffort.value = resolvedEffort
        val activeId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.updateSessionModel(
                sessionId = activeId,
                modelId = modelId,
                reasoningEffort = resolvedEffort,
            )
        }
    }

    fun sendDraft(onSessionCreate: (String) -> Unit) {
        val snapshot = uiState.value
        val isArchived = snapshot.detail?.summary?.isArchived == true
        val text = draft.value.trim()
        val attachments = draftAttachments.value
        if (isArchived || (text.isEmpty() && attachments.isEmpty())) return

        draft.value = ""
        draftAttachments.value = persistentListOf()

        viewModelScope.launch {
            val existingId = currentSessionId.value
            val targetId =
                if (existingId != null) {
                    existingId
                } else {
                    val createdId =
                        sessionGateway.createSession(
                            modelId = snapshot.activeModel?.id,
                            reasoningEffort = snapshot.activeEffort,
                            runtimeContext = snapshot.activeContext,
                        )
                    currentSessionId.value = createdId
                    onSessionCreate(createdId)
                    createdId
                }
            sessionGateway.sendMessage(
                sessionId = targetId,
                content = text,
                attachments = attachments,
            )
        }
    }

    fun pauseActiveLoop() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.controlLoop(sessionId, LoopControlCommand.Pause)
        }
    }

    fun resumeActiveLoop() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.controlLoop(sessionId, LoopControlCommand.Resume())
        }
    }

    fun cancelActiveLoop() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.controlLoop(sessionId, LoopControlCommand.Stop)
        }
    }

    fun restoreActiveSession() {
        val sessionId = currentSessionId.value ?: return
        viewModelScope.launch {
            sessionGateway.setSessionArchived(sessionId = sessionId, archived = false)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
