package com.everfold.core.model

fun RemoteTransportConfig.displayAddress(): String =
    when (this) {
        is RemoteTransportConfig.Nats -> {
            formatHostPort(host = host, port = port)
        }

        is RemoteTransportConfig.Zenoh -> {
            if (accessMode == ZenohAccessMode.P2P && host.isBlank()) {
                "Multicast P2P"
            } else {
                formatHostPort(host = host, port = port)
            }
        }
    }

fun RemoteTransportConfig.defaultProfileName(): String =
    when (this) {
        is RemoteTransportConfig.Nats -> {
            if (host.isNotBlank()) "NATS · ${host.trim()}" else "NATS Node"
        }

        is RemoteTransportConfig.Zenoh -> {
            if (accessMode == ZenohAccessMode.P2P && host.isBlank()) {
                "Zenoh P2P"
            } else if (host.isNotBlank()) {
                "Zenoh · ${host.trim()}"
            } else {
                "Zenoh Node"
            }
        }
    }

private fun formatHostPort(
    host: String,
    port: String,
): String {
    val cleanHost = host.trim()
    val cleanPort = port.trim()
    return when {
        cleanHost.isEmpty() -> ""
        cleanPort.isEmpty() -> cleanHost
        else -> "$cleanHost:$cleanPort"
    }
}
