package com.everfold.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionMode {
    REMOTE_AUTHORITY,
    RELAY,
    LOCAL_ONLY,
}

@Serializable
data class AuthorityEndpoint(
    val mode: ConnectionMode,
    val address: String = "",
    val label: String = "",
)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState

    data class Connecting(
        val endpoint: AuthorityEndpoint,
    ) : ConnectionState

    data class Connected(
        val endpoint: AuthorityEndpoint,
        val authorityNodeId: String,
    ) : ConnectionState

    data class Error(
        val endpoint: AuthorityEndpoint,
        val reason: String,
    ) : ConnectionState
}
