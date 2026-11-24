package com.everfold.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NodesUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val savedProfiles: ImmutableList<ConnectionProfile> = persistentListOf(),
    val nodes: ImmutableList<NodeDescriptor> = persistentListOf(),
    val connectingProfileId: String? = null,
    val lastErrorCode: ConnectionErrorCode? = null,
)

class NodesViewModel(
    private val authorityGateway: AuthorityGateway,
    private val topologyGateway: TopologyGateway,
) : ViewModel() {
    private val connectingProfileId = MutableStateFlow<String?>(null)
    private val lastErrorCode = MutableStateFlow<ConnectionErrorCode?>(null)

    val uiState: StateFlow<NodesUiState> =
        combine(
            authorityGateway.connectionState,
            authorityGateway.savedProfiles,
            topologyGateway.nodes,
            connectingProfileId,
            lastErrorCode,
        ) { connection, profiles, nodes, connectingId, errorCode ->
            NodesUiState(
                connectionState = connection,
                savedProfiles = profiles,
                nodes = nodes,
                connectingProfileId = connectingId,
                lastErrorCode = errorCode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = NodesUiState(),
        )

    fun refresh() {
        viewModelScope.launch {
            topologyGateway.refreshNodes()
        }
    }

    fun disconnect() {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.disconnect()
        }
    }

    fun connectLocal() {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.connectLocal()
        }
    }

    fun connectProfile(profileId: String) {
        connectingProfileId.value = profileId
        lastErrorCode.value = null
        viewModelScope.launch {
            val ok = authorityGateway.connectProfile(profileId)
            if (!ok) {
                lastErrorCode.value =
                    (authorityGateway.connectionState.value as? ConnectionState.Error)?.errorCode
                        ?: ConnectionErrorCode.HANDSHAKE_FAILED
            }
            connectingProfileId.value = null
        }
    }

    fun updateProfileNote(
        profileId: String,
        note: String,
    ) {
        viewModelScope.launch {
            authorityGateway.updateProfileNote(profileId = profileId, note = note)
        }
    }

    fun deleteProfile(profileId: String) {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.deleteProfile(profileId)
        }
    }

    fun clearError() {
        lastErrorCode.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
