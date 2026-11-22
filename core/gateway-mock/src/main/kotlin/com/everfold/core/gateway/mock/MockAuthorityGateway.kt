package com.everfold.core.gateway.mock

import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeDescriptor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MockAuthorityGateway(
    private val stepDelayMs: Long = DEFAULT_STEP_DELAY_MS,
    private val onConnected: (ConnectionMode) -> Unit = {},
    private val onDisconnected: () -> Unit = {},
) : AuthorityGateway,
    TopologyGateway {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _nodes = MutableStateFlow<ImmutableList<NodeDescriptor>>(persistentListOf())
    override val nodes: StateFlow<ImmutableList<NodeDescriptor>> = _nodes.asStateFlow()

    override suspend fun connect(endpoint: AuthorityEndpoint) {
        _connectionState.value = ConnectionState.Connecting(endpoint)
        if (stepDelayMs > 0) delay(stepDelayMs)

        val newNodes = MockFixtures.nodesFor(endpoint.mode)
        _nodes.value = newNodes
        val authorityId = newNodes.firstOrNull()?.id ?: MockFixtures.localAttribution.nodeId
        onConnected(endpoint.mode)
        _connectionState.value =
            ConnectionState.Connected(
                endpoint = endpoint,
                authorityNodeId = authorityId,
            )
    }

    override suspend fun disconnect() {
        onDisconnected()
        _connectionState.value = ConnectionState.Disconnected
        _nodes.value = persistentListOf()
    }

    override suspend fun refreshNodes() {
        val current = _connectionState.value
        if (current is ConnectionState.Connected) {
            if (stepDelayMs > 0) delay(stepDelayMs)
            _nodes.value = MockFixtures.nodesFor(current.endpoint.mode)
        }
    }

    internal fun currentMode(): ConnectionMode {
        val state = _connectionState.value
        return if (state is ConnectionState.Connected) {
            state.endpoint.mode
        } else {
            ConnectionMode.LOCAL_ONLY
        }
    }

    internal fun currentLoopHostName(): String =
        if (currentMode() == ConnectionMode.LOCAL_ONLY) {
            MockFixtures.localAttribution.nodeName
        } else {
            MockFixtures.cloudAttribution.nodeName
        }

    internal companion object {
        const val DEFAULT_STEP_DELAY_MS = 240L
    }
}
