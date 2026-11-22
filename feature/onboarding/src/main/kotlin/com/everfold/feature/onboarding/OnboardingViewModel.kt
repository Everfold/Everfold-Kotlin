package com.everfold.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedMode: ConnectionMode = ConnectionMode.REMOTE_AUTHORITY,
    val address: String = "wss://authority.everfold.local",
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
)

class OnboardingViewModel(
    private val authorityGateway: AuthorityGateway,
) : ViewModel() {
    private val selectedMode = MutableStateFlow(ConnectionMode.REMOTE_AUTHORITY)
    private val address = MutableStateFlow("wss://authority.everfold.local")

    val uiState: StateFlow<OnboardingUiState> =
        combine(
            selectedMode,
            address,
            authorityGateway.connectionState,
        ) { mode, addr, connState ->
            OnboardingUiState(
                selectedMode = mode,
                address = addr,
                isConnecting = connState is ConnectionState.Connecting,
                errorMessage = (connState as? ConnectionState.Error)?.reason,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = OnboardingUiState(),
        )

    fun selectMode(mode: ConnectionMode) {
        selectedMode.value = mode
        address.update { current ->
            when (mode) {
                ConnectionMode.REMOTE_AUTHORITY -> {
                    current.ifBlank { "wss://authority.everfold.local" }
                }

                ConnectionMode.RELAY -> {
                    "wss://relay.everfold.local/tunnel"
                }

                ConnectionMode.LOCAL_ONLY -> {
                    ""
                }
            }
        }
    }

    fun updateAddress(value: String) {
        address.value = value
    }

    fun connect() {
        val mode = selectedMode.value
        val endpoint =
            AuthorityEndpoint(
                mode = mode,
                address = if (mode == ConnectionMode.LOCAL_ONLY) "" else address.value.trim(),
                label =
                    when (mode) {
                        ConnectionMode.REMOTE_AUTHORITY -> "远程 Authority"
                        ConnectionMode.RELAY -> "中转节点"
                        ConnectionMode.LOCAL_ONLY -> "纯本地模式"
                    },
            )
        viewModelScope.launch {
            authorityGateway.connect(endpoint)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
