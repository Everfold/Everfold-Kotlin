package com.everfold.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ConnectionMode {
    LOCAL_ONLY,
    REMOTE,
}

@Serializable
enum class TransportBackend {
    NATS,
    ZENOH,
}

@Serializable
enum class NatsProtocol {
    TCP,
    TLS,
    WEBSOCKET,
}

@Serializable
enum class NatsAuthType {
    NONE,
    TOKEN,
    USER_PASSWORD,
    MTLS,
}

@Serializable
enum class ZenohAccessMode {
    P2P,
    EXPLICIT,
}

@Serializable
enum class ZenohProtocol {
    TCP,
    TLS,
    QUIC,
    WEBSOCKET,
}

@Serializable
enum class ZenohAuthType {
    NONE,
    USER_PASSWORD,
    TLS_ONE_WAY,
    MTLS,
}

@Serializable
enum class CertificateSource {
    FILE,
    QR_CODE,
}

@Serializable
data class CertificatePayload(
    val displayName: String,
    val contentPreview: String = "",
    val source: CertificateSource = CertificateSource.FILE,
)

@Serializable
sealed interface RemoteTransportConfig {
    @Serializable
    data class Nats(
        val host: String = "",
        val port: String = DEFAULT_NATS_PORT,
        val protocol: NatsProtocol = NatsProtocol.TCP,
        val authType: NatsAuthType = NatsAuthType.NONE,
        val token: String = "",
        val username: String = "",
        val password: String = "",
        val caCert: CertificatePayload? = null,
        val clientCert: CertificatePayload? = null,
        val clientKey: CertificatePayload? = null,
    ) : RemoteTransportConfig

    @Serializable
    data class Zenoh(
        val accessMode: ZenohAccessMode = ZenohAccessMode.P2P,
        val host: String = "",
        val port: String = DEFAULT_ZENOH_PORT,
        val protocol: ZenohProtocol = ZenohProtocol.TCP,
        val authType: ZenohAuthType = ZenohAuthType.NONE,
        val username: String = "",
        val password: String = "",
        val caCert: CertificatePayload? = null,
        val clientCert: CertificatePayload? = null,
        val clientKey: CertificatePayload? = null,
    ) : RemoteTransportConfig

    companion object {
        const val DEFAULT_NATS_PORT = "4222"
        const val DEFAULT_ZENOH_PORT = "7447"
    }
}

@Serializable
data class ConnectionProfile(
    val id: String,
    val name: String,
    val note: String = "",
    val transport: RemoteTransportConfig,
)

@Serializable
enum class ConnectionErrorCode {
    HOST_REQUIRED,
    INVALID_PORT,
    TOKEN_REQUIRED,
    CREDENTIALS_REQUIRED,
    CA_CERT_REQUIRED,
    MTLS_CERTS_REQUIRED,
    HANDSHAKE_FAILED,
    PROFILE_NOT_FOUND,
}

@Serializable
data class AuthorityEndpoint(
    val mode: ConnectionMode,
    val address: String = "",
    val label: String = "",
    val profileId: String? = null,
    val transport: RemoteTransportConfig? = null,
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
        val reason: String = "",
        val errorCode: ConnectionErrorCode? = null,
    ) : ConnectionState
}
