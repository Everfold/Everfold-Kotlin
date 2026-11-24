package com.everfold.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NodesUiState(
    val isLocalConnected: Boolean = false,
    val connectedProfileIds: ImmutableSet<String> = persistentSetOf(),
    val savedProfiles: ImmutableList<ConnectionProfile> = persistentListOf(),
    val connectingProfileIds: ImmutableSet<String> = persistentSetOf(),
    val lastErrorCode: ConnectionErrorCode? = null,
)

class NodesViewModel(
    private val authorityGateway: AuthorityGateway,
) : ViewModel() {
    private val connectingProfileIds = MutableStateFlow<ImmutableSet<String>>(persistentSetOf())
    private val lastErrorCode = MutableStateFlow<ConnectionErrorCode?>(null)

    val uiState: StateFlow<NodesUiState> =
        combine(
            authorityGateway.isLocalModeConnected,
            authorityGateway.connectedProfileIds,
            authorityGateway.savedProfiles,
            connectingProfileIds,
            lastErrorCode,
        ) { isLocal, connectedIds, profiles, connectingIds, errorCode ->
            NodesUiState(
                isLocalConnected = isLocal,
                connectedProfileIds = connectedIds,
                savedProfiles = profiles,
                connectingProfileIds = connectingIds,
                lastErrorCode = errorCode,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = NodesUiState(),
        )

    fun disconnectAll() {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.disconnect()
        }
    }

    fun disconnectProfile(profileId: String) {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.disconnectProfile(profileId)
        }
    }

    fun connectLocal() {
        lastErrorCode.value = null
        viewModelScope.launch {
            authorityGateway.connectLocal()
        }
    }

    fun connectProfile(profileId: String) {
        connectingProfileIds.update { (it + profileId).toImmutableSet() }
        lastErrorCode.value = null
        viewModelScope.launch {
            val ok = authorityGateway.connectProfile(profileId)
            if (!ok) {
                lastErrorCode.value =
                    (authorityGateway.connectionState.value as? ConnectionState.Error)?.errorCode
                        ?: ConnectionErrorCode.HANDSHAKE_FAILED
            }
            connectingProfileIds.update { (it - profileId).toImmutableSet() }
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

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
