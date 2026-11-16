package com.everfold.kotlin.feature.connections

import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connected_preview
import everfold.shared.generated.resources.connection_connecting
import everfold.shared.generated.resources.connection_disconnected
import everfold.shared.generated.resources.connection_disconnecting
import everfold.shared.generated.resources.connection_failed
import kotlin.time.Instant

const val BUILT_IN_CONNECTION_ID = "built-in"

/** 保存的接入信息与运行状态分离内置连接由管理模块提供并固定排在首位 */
internal data class ConnectionProfile(
    val id: String,
    val name: UiText,
    val setup: ClusterSetup? = null,
    val autoConnect: Boolean = false,
) {
    val canRemove: Boolean get() = id != BUILT_IN_CONNECTION_ID
    val canEditEndpoint: Boolean get() = setup != null
}

internal enum class ConnectionStatus { Disconnected, Connecting, Connected, Disconnecting, Failed }

internal val ConnectionStatus.label
    get() =
        when (this) {
            ConnectionStatus.Disconnected -> Res.string.connection_disconnected
            ConnectionStatus.Connecting -> Res.string.connection_connecting
            ConnectionStatus.Connected -> Res.string.connected_preview
            ConnectionStatus.Disconnecting -> Res.string.connection_disconnecting
            ConnectionStatus.Failed -> Res.string.connection_failed
        }

/** 内核信息仅在成功连接后更新断开后保留最后一次快照 */
internal data class ConnectionRuntimeState(
    val status: ConnectionStatus = ConnectionStatus.Disconnected,
    val failure: ConnectionPreviewResult? = null,
    val lastConnectedAt: Instant? = null,
    val kernelVersion: String? = null,
)

internal data class Connection(
    val profile: ConnectionProfile,
    val runtime: ConnectionRuntimeState = ConnectionRuntimeState(),
) {
    val id: String get() = profile.id
    val isConnected: Boolean get() = runtime.status == ConnectionStatus.Connected
}
