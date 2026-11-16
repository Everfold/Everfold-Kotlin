package com.everfold.kotlin.feature.connections

import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_local
import everfold.shared.generated.resources.connection_remote
import org.jetbrains.compose.resources.StringResource

/** 运行方式及本地化标签供首次设置和连接页面复用 */
enum class ConnectionMode(
    val label: StringResource,
) {
    // 本地
    Local(Res.string.connection_local),

    // 集群
    Remote(Res.string.connection_remote),
}
