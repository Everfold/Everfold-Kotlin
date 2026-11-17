package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.connection_status_description

private val ConnectedColor = Color(0xFF43A047)

/** 圆点表示会话所绑定连接的实时状态，文字提示与无障碍描述提供完整含义。 */
@Composable
internal fun ConnectionStatusIndicator(connection: Connection?) {
    val status = connection?.runtime?.status
    val description =
        localizedString(
            Res.string.connection_status_description,
            localizedString(status?.label ?: Res.string.connection_removed),
        )
    val color = connectionStatusColor(status)
    AppTooltip(description) {
        Box(Modifier.size(8.dp).background(color, CircleShape).semantics { contentDescription = description })
    }
}

@Composable
internal fun connectionStatusColor(status: ConnectionStatus?): Color =
    if (status == ConnectionStatus.Connected) ConnectedColor else MaterialTheme.colorScheme.error
