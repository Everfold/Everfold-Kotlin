package com.everfold.feature.nodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NodesUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val nodes: ImmutableList<NodeDescriptor> = persistentListOf(),
)

class NodesViewModel(
    private val authorityGateway: AuthorityGateway,
    private val topologyGateway: TopologyGateway,
) : ViewModel() {
    val uiState: StateFlow<NodesUiState> =
        combine(
            authorityGateway.connectionState,
            topologyGateway.nodes,
        ) { connection, nodes ->
            NodesUiState(connectionState = connection, nodes = nodes)
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
        viewModelScope.launch {
            authorityGateway.disconnect()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
