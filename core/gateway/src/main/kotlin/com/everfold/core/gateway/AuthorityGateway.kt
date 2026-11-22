package com.everfold.core.gateway

import com.everfold.core.model.AuthorityEndpoint
import com.everfold.core.model.ConnectionState
import kotlinx.coroutines.flow.StateFlow

interface AuthorityGateway {
    val connectionState: StateFlow<ConnectionState>

    suspend fun connect(endpoint: AuthorityEndpoint)

    suspend fun disconnect()
}
