package com.everfold.kotlin.mock

import androidx.compose.runtime.Composable
import com.everfold.kotlin.feature.connections.ConnectionPreviewOptions

/** 演示选项由装配层注入普通连接页面无需知道如何模拟结果 */
@Composable
internal fun MockConnectionOptions(
    connections: MockConnections,
    id: String,
) {
    ConnectionPreviewOptions(connections.previewResult(id)) { connections.selectPreviewResult(id, it) }
}
