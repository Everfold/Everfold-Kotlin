package com.everfold.core.gateway

import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.RemoteTransportConfig
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.StateFlow

interface AuthorityGateway {
    val connectionState: StateFlow<ConnectionState>
    val connectedProfileIds: StateFlow<ImmutableSet<String>>
    val isLocalModeConnected: StateFlow<Boolean>
    val savedProfiles: StateFlow<ImmutableList<ConnectionProfile>>

    suspend fun connect(endpoint: AuthorityEndpoint)

    suspend fun connectLocal()

    suspend fun connectRemote(
        transport: RemoteTransportConfig,
        name: String = "",
        note: String = "",
        profileId: String? = null,
    ): Boolean

    suspend fun connectProfile(profileId: String): Boolean

    suspend fun disconnectProfile(profileId: String)

    suspend fun saveProfile(profile: ConnectionProfile): ConnectionProfile

    suspend fun updateProfileNote(
        profileId: String,
        note: String,
    )

    suspend fun deleteProfile(profileId: String)

    suspend fun disconnect()
}
