package com.everfold.core.ui.connection

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.NatsAuthType
import com.everfold.core.model.NatsProtocol
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.ZenohAccessMode
import com.everfold.core.model.ZenohAuthType
import com.everfold.core.model.ZenohProtocol
import com.everfold.core.model.displayAddress
import com.everfold.core.ui.R

@StringRes
fun NatsProtocol.labelRes(): Int =
    when (this) {
        NatsProtocol.TCP -> R.string.conn_protocol_tcp
        NatsProtocol.TLS -> R.string.conn_protocol_tls
        NatsProtocol.WEBSOCKET -> R.string.conn_protocol_websocket
    }

@StringRes
fun NatsAuthType.labelRes(): Int =
    when (this) {
        NatsAuthType.NONE -> R.string.conn_auth_none
        NatsAuthType.TOKEN -> R.string.conn_auth_token
        NatsAuthType.USER_PASSWORD -> R.string.conn_auth_user_password
        NatsAuthType.MTLS -> R.string.conn_auth_mtls
    }

@StringRes
fun ZenohAccessMode.labelRes(): Int =
    when (this) {
        ZenohAccessMode.P2P -> R.string.conn_zenoh_mode_p2p
        ZenohAccessMode.EXPLICIT -> R.string.conn_zenoh_mode_explicit
    }

@StringRes
fun ZenohProtocol.labelRes(): Int =
    when (this) {
        ZenohProtocol.TCP -> R.string.conn_protocol_tcp
        ZenohProtocol.TLS -> R.string.conn_protocol_tls
        ZenohProtocol.QUIC -> R.string.conn_protocol_quic
        ZenohProtocol.WEBSOCKET -> R.string.conn_protocol_websocket
    }

@StringRes
fun ZenohAuthType.labelRes(): Int =
    when (this) {
        ZenohAuthType.NONE -> R.string.conn_auth_none
        ZenohAuthType.USER_PASSWORD -> R.string.conn_auth_user_password
        ZenohAuthType.TLS_ONE_WAY -> R.string.conn_auth_tls_one_way
        ZenohAuthType.MTLS -> R.string.conn_auth_mtls
    }

@StringRes
fun ConnectionErrorCode.labelRes(): Int =
    when (this) {
        ConnectionErrorCode.HOST_REQUIRED -> R.string.conn_error_host_required
        ConnectionErrorCode.INVALID_PORT -> R.string.conn_error_invalid_port
        ConnectionErrorCode.TOKEN_REQUIRED -> R.string.conn_error_token_required
        ConnectionErrorCode.CREDENTIALS_REQUIRED -> R.string.conn_error_credentials_required
        ConnectionErrorCode.CA_CERT_REQUIRED -> R.string.conn_error_ca_cert_required
        ConnectionErrorCode.MTLS_CERTS_REQUIRED -> R.string.conn_error_mtls_certs_required
        ConnectionErrorCode.HANDSHAKE_FAILED -> R.string.conn_error_handshake_failed
        ConnectionErrorCode.PROFILE_NOT_FOUND -> R.string.conn_error_profile_not_found
    }

@Composable
fun RemoteTransportConfig.summaryLine(): String =
    when (this) {
        is RemoteTransportConfig.Nats -> {
            val proto = stringResource(protocol.labelRes())
            val auth = stringResource(authType.labelRes())
            val addr = displayAddress()
            if (addr.isNotBlank()) "NATS · $proto · $auth · $addr" else "NATS · $proto · $auth"
        }

        is RemoteTransportConfig.Zenoh -> {
            val mode = stringResource(accessMode.labelRes())
            val proto = stringResource(protocol.labelRes())
            val auth = stringResource(authType.labelRes())
            val addr = displayAddress()
            if (addr.isNotBlank()) "Zenoh · $mode · $proto · $auth · $addr" else "Zenoh · $mode · $proto · $auth"
        }
    }
