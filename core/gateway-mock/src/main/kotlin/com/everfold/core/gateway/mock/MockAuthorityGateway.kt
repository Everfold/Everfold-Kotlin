package com.everfold.core.gateway.mock

import com.everfold.core.gateway.AuthorityGateway
import com.everfold.core.gateway.TopologyGateway
import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NodeDescriptor
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.defaultProfileName
import com.everfold.core.model.displayAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicInteger

class MockAuthorityGateway(
    private val stepDelayMs: Long = DEFAULT_STEP_DELAY_MS,
    private val onConnected: (ConnectionMode) -> Unit = {},
    private val onDisconnected: () -> Unit = {},
) : AuthorityGateway,
    TopologyGateway {
    private val profileCounter = AtomicInteger(INITIAL_PROFILE_ID)
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedProfileIds = MutableStateFlow<ImmutableSet<String>>(persistentSetOf())
    override val connectedProfileIds: StateFlow<ImmutableSet<String>> = _connectedProfileIds.asStateFlow()

    private val _isLocalModeConnected = MutableStateFlow(false)
    override val isLocalModeConnected: StateFlow<Boolean> = _isLocalModeConnected.asStateFlow()

    private val _savedProfiles = MutableStateFlow(defaultMockProfiles())
    override val savedProfiles: StateFlow<ImmutableList<ConnectionProfile>> = _savedProfiles.asStateFlow()

    private val _nodes = MutableStateFlow<ImmutableList<NodeDescriptor>>(persistentListOf())
    override val nodes: StateFlow<ImmutableList<NodeDescriptor>> = _nodes.asStateFlow()

    override suspend fun connect(endpoint: AuthorityEndpoint) {
        _connectionState.value = ConnectionState.Connecting(endpoint)
        if (stepDelayMs > 0) delay(stepDelayMs)

        if (endpoint.mode == ConnectionMode.LOCAL_ONLY) {
            _isLocalModeConnected.value = true
        }
        val profileId = endpoint.profileId
        if (profileId != null) {
            _connectedProfileIds.update { (it + profileId).toImmutableSet() }
        }
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

    override suspend fun connectLocal() {
        connect(
            AuthorityEndpoint(
                mode = ConnectionMode.LOCAL_ONLY,
                label = MockFixtures.localAttribution.nodeName,
            ),
        )
    }

    override suspend fun connectRemote(
        transport: RemoteTransportConfig,
        name: String,
        note: String,
        profileId: String?,
    ): Boolean {
        val resolvedName = name.trim().ifEmpty { transport.defaultProfileName() }
        val draftProfile =
            ConnectionProfile(
                id = profileId?.ifBlank { null } ?: "profile-${profileCounter.incrementAndGet()}",
                name = resolvedName,
                note = note.trim(),
                transport = transport,
            )
        val endpoint =
            AuthorityEndpoint(
                mode = ConnectionMode.REMOTE,
                address = transport.displayAddress(),
                label = draftProfile.name,
                profileId = draftProfile.id,
                transport = transport,
            )
        _connectionState.value = ConnectionState.Connecting(endpoint)
        if (stepDelayMs > 0) delay(stepDelayMs)

        val errorCode = validateTransport(transport)
        return if (errorCode != null) {
            _connectionState.value = ConnectionState.Error(endpoint = endpoint, errorCode = errorCode)
            false
        } else {
            val saved = saveProfile(draftProfile)
            connect(endpoint.copy(profileId = saved.id, label = saved.name))
            true
        }
    }

    override suspend fun connectProfile(profileId: String): Boolean {
        val profile = _savedProfiles.value.firstOrNull { it.id == profileId }
        return if (profile == null) {
            _connectionState.value =
                ConnectionState.Error(
                    endpoint = AuthorityEndpoint(mode = ConnectionMode.REMOTE, profileId = profileId),
                    errorCode = ConnectionErrorCode.PROFILE_NOT_FOUND,
                )
            false
        } else {
            connectRemote(
                transport = profile.transport,
                name = profile.name,
                note = profile.note,
                profileId = profile.id,
            )
        }
    }

    override suspend fun disconnectProfile(profileId: String) {
        _connectedProfileIds.update { (it - profileId).toImmutableSet() }
        val remainingIds = _connectedProfileIds.value
        val nextProfile = _savedProfiles.value.firstOrNull { it.id in remainingIds }
        when {
            nextProfile != null -> {
                val remoteNodes = MockFixtures.nodesFor(ConnectionMode.REMOTE)
                _nodes.value = remoteNodes
                _connectionState.value =
                    ConnectionState.Connected(
                        endpoint =
                            AuthorityEndpoint(
                                mode = ConnectionMode.REMOTE,
                                address = nextProfile.transport.displayAddress(),
                                label = nextProfile.name,
                                profileId = nextProfile.id,
                                transport = nextProfile.transport,
                            ),
                        authorityNodeId = remoteNodes.firstOrNull()?.id ?: MockFixtures.cloudAttribution.nodeId,
                    )
            }

            _isLocalModeConnected.value -> {
                val localNodes = MockFixtures.nodesFor(ConnectionMode.LOCAL_ONLY)
                _nodes.value = localNodes
                _connectionState.value =
                    ConnectionState.Connected(
                        endpoint =
                            AuthorityEndpoint(
                                mode = ConnectionMode.LOCAL_ONLY,
                                label = MockFixtures.localAttribution.nodeName,
                            ),
                        authorityNodeId = localNodes.firstOrNull()?.id ?: MockFixtures.localAttribution.nodeId,
                    )
            }

            else -> {
                onDisconnected()
                _connectionState.value = ConnectionState.Disconnected
                _nodes.value = persistentListOf()
            }
        }
    }

    override suspend fun saveProfile(profile: ConnectionProfile): ConnectionProfile {
        val resolvedName = profile.name.trim().ifEmpty { profile.transport.defaultProfileName() }
        val resolvedId = profile.id.ifBlank { "profile-${profileCounter.incrementAndGet()}" }
        val normalized = profile.copy(id = resolvedId, name = resolvedName, note = profile.note.trim())
        _savedProfiles.update { current ->
            val exists = current.any { it.id == resolvedId }
            if (exists) {
                current.map { if (it.id == resolvedId) normalized else it }.toImmutableList()
            } else {
                (listOf(normalized) + current).toImmutableList()
            }
        }
        return normalized
    }

    override suspend fun updateProfileNote(
        profileId: String,
        note: String,
    ) {
        val trimmed = note.trim()
        _savedProfiles.update { current ->
            current.map { if (it.id == profileId) it.copy(note = trimmed) else it }.toImmutableList()
        }
    }

    override suspend fun deleteProfile(profileId: String) {
        _savedProfiles.update { current -> current.filterNot { it.id == profileId }.toImmutableList() }
        if (profileId in _connectedProfileIds.value) {
            disconnectProfile(profileId)
        }
    }

    override suspend fun disconnect() {
        onDisconnected()
        _connectedProfileIds.value = persistentSetOf()
        _isLocalModeConnected.value = false
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

    internal companion object {
        const val DEFAULT_STEP_DELAY_MS = 240L
        private const val INITIAL_PROFILE_ID = 10
    }
}
