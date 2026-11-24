package com.everfold.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.RemoteTransportConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodeEditorUiState(
    val profileId: String? = null,
    val name: String = "",
    val note: String = "",
    val transport: RemoteTransportConfig = RemoteTransportConfig.Nats(),
    val isConnecting: Boolean = false,
    val errorCode: ConnectionErrorCode? = null,
)

class NodeEditorViewModel(
    private val authorityGateway: AuthorityGateway,
) : ViewModel() {
    private var boundProfileKey: String? = null
    private val _uiState = MutableStateFlow(NodeEditorUiState())
    val uiState: StateFlow<NodeEditorUiState> = _uiState.asStateFlow()

    fun bindProfile(profileId: String?) {
        val key = profileId ?: NEW_PROFILE_KEY
        if (boundProfileKey == key) return
        boundProfileKey = key

        val existing = profileId?.let { id -> authorityGateway.savedProfiles.value.firstOrNull { it.id == id } }
        _uiState.value =
            if (existing != null) {
                NodeEditorUiState(
                    profileId = existing.id,
                    name = existing.name,
                    note = existing.note,
                    transport = existing.transport,
                )
            } else {
                NodeEditorUiState()
            }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorCode = null) }
    }

    fun updateNote(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun updateTransport(value: RemoteTransportConfig) {
        _uiState.update { it.copy(transport = value, errorCode = null) }
    }

    fun saveOnly(onSaveComplete: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            authorityGateway.saveProfile(
                ConnectionProfile(
                    id = state.profileId.orEmpty(),
                    name = state.name,
                    note = state.note,
                    transport = state.transport,
                ),
            )
            onSaveComplete()
        }
    }

    fun saveAndConnect(onConnectSuccess: () -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isConnecting = true, errorCode = null) }
        viewModelScope.launch {
            val ok =
                authorityGateway.connectRemote(
                    transport = state.transport,
                    name = state.name,
                    note = state.note,
                    profileId = state.profileId,
                )
            if (ok) {
                _uiState.update { it.copy(isConnecting = false) }
                onConnectSuccess()
            } else {
                val failedCode =
                    (authorityGateway.connectionState.value as? ConnectionState.Error)?.errorCode
                        ?: ConnectionErrorCode.HANDSHAKE_FAILED
                _uiState.update { it.copy(isConnecting = false, errorCode = failedCode) }
            }
        }
    }

    private companion object {
        const val NEW_PROFILE_KEY = "__new__"
    }
}
