package com.everfold.core.gateway.mock

import com.everfold.core.model.CertificatePayload
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.ConnectionMode
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.model.ConnectionState
import com.everfold.core.model.NatsAuthType
import com.everfold.core.model.NatsProtocol
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.ZenohAccessMode
import com.everfold.core.model.ZenohAuthType
import com.everfold.core.model.ZenohProtocol
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val MIN_PORT = 1
private const val MAX_PORT = 65_535

internal fun validateTransport(transport: RemoteTransportConfig): ConnectionErrorCode? =
    when (transport) {
        is RemoteTransportConfig.Nats -> validateNats(transport)
        is RemoteTransportConfig.Zenoh -> validateZenoh(transport)
    }

private fun validateNats(config: RemoteTransportConfig.Nats): ConnectionErrorCode? {
    val hostError =
        when {
            config.host.isBlank() -> ConnectionErrorCode.HOST_REQUIRED
            !isValidPort(config.port) -> ConnectionErrorCode.INVALID_PORT
            else -> null
        }
    val authError =
        when (config.authType) {
            NatsAuthType.NONE -> {
                null
            }

            NatsAuthType.TOKEN -> {
                if (config.token.isBlank()) ConnectionErrorCode.TOKEN_REQUIRED else null
            }

            NatsAuthType.USER_PASSWORD -> {
                if (config.username.isBlank() || config.password.isBlank()) {
                    ConnectionErrorCode.CREDENTIALS_REQUIRED
                } else {
                    null
                }
            }

            NatsAuthType.MTLS -> {
                if (!hasMtlsCerts(config.caCert, config.clientCert, config.clientKey)) {
                    ConnectionErrorCode.MTLS_CERTS_REQUIRED
                } else {
                    null
                }
            }
        }
    return hostError ?: authError ?: checkSimulatedFailure(config.host)
}

private fun validateZenoh(config: RemoteTransportConfig.Zenoh): ConnectionErrorCode? =
    validateZenohHost(config) ?: validateZenohAuth(config) ?: checkSimulatedFailure(config.host)

private fun validateZenohHost(config: RemoteTransportConfig.Zenoh): ConnectionErrorCode? {
    val isExplicit = config.accessMode == ZenohAccessMode.EXPLICIT
    return when {
        isExplicit && config.host.isBlank() -> ConnectionErrorCode.HOST_REQUIRED
        isExplicit && !isValidPort(config.port) -> ConnectionErrorCode.INVALID_PORT
        !isExplicit && config.port.isNotBlank() && !isValidPort(config.port) -> ConnectionErrorCode.INVALID_PORT
        else -> null
    }
}

private fun validateZenohAuth(config: RemoteTransportConfig.Zenoh): ConnectionErrorCode? =
    when (config.authType) {
        ZenohAuthType.NONE -> {
            null
        }

        ZenohAuthType.USER_PASSWORD -> {
            if (config.username.isBlank() || config.password.isBlank()) {
                ConnectionErrorCode.CREDENTIALS_REQUIRED
            } else {
                null
            }
        }

        ZenohAuthType.TLS_ONE_WAY -> {
            if (config.caCert == null) ConnectionErrorCode.CA_CERT_REQUIRED else null
        }

        ZenohAuthType.MTLS -> {
            if (!hasMtlsCerts(config.caCert, config.clientCert, config.clientKey)) {
                ConnectionErrorCode.MTLS_CERTS_REQUIRED
            } else {
                null
            }
        }
    }

private fun isValidPort(portText: String): Boolean {
    val parsed = portText.trim().toIntOrNull()
    return parsed != null && parsed in MIN_PORT..MAX_PORT
}

private fun hasMtlsCerts(
    caCert: CertificatePayload?,
    clientCert: CertificatePayload?,
    clientKey: CertificatePayload?,
): Boolean = caCert != null && clientCert != null && clientKey != null

private fun checkSimulatedFailure(host: String): ConnectionErrorCode? {
    val normalized = host.trim()
    return if (normalized.contains("fail", ignoreCase = true) ||
        normalized.contains("error", ignoreCase = true)
    ) {
        ConnectionErrorCode.HANDSHAKE_FAILED
    } else {
        null
    }
}

internal fun ConnectionState.currentMode(): ConnectionMode =
    if (this is ConnectionState.Connected) endpoint.mode else ConnectionMode.LOCAL_ONLY

internal fun ConnectionState.currentLoopHostName(): String =
    if (currentMode() == ConnectionMode.LOCAL_ONLY) {
        MockFixtures.localAttribution.nodeName
    } else {
        MockFixtures.cloudAttribution.nodeName
    }

internal fun defaultMockProfiles(): ImmutableList<ConnectionProfile> =
    persistentListOf(
        ConnectionProfile(
            id = "profile-nats-cloud",
            name = "Cloud NATS Authority",
            note = "生产集群默认入口（Mock）",
            transport =
                RemoteTransportConfig.Nats(
                    host = "nats.everfold.local",
                    port = "4222",
                    protocol = NatsProtocol.TLS,
                    authType = NatsAuthType.TOKEN,
                    token = "ef-demo-token",
                ),
        ),
        ConnectionProfile(
            id = "profile-zenoh-p2p",
            name = "LAN Zenoh P2P",
            note = "局域网组播自动发现节点",
            transport =
                RemoteTransportConfig.Zenoh(
                    accessMode = ZenohAccessMode.P2P,
                    protocol = ZenohProtocol.QUIC,
                    authType = ZenohAuthType.NONE,
                ),
        ),
    )
