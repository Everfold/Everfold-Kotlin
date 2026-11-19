package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.connections.Connection
import com.everfold.kotlin.feature.connections.ConnectionFailure
import com.everfold.kotlin.feature.connections.ConnectionStatus
import com.everfold.kotlin.feature.connections.description
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.composer_connecting
import everfold.shared.generated.resources.composer_connection_failed
import everfold.shared.generated.resources.composer_disconnected
import everfold.shared.generated.resources.composer_disconnecting
import everfold.shared.generated.resources.composer_failed_uploads
import everfold.shared.generated.resources.composer_model_missing
import everfold.shared.generated.resources.composer_reconnect
import everfold.shared.generated.resources.composer_removed
import everfold.shared.generated.resources.composer_retry_uploads
import everfold.shared.generated.resources.composer_workspace_missing
import everfold.shared.generated.resources.connection_manage
import everfold.shared.generated.resources.retry_connection
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 优先说明阻止发送的原因，恢复动作不自动发送草稿。 */
@Composable
internal fun ComposerFeedback(
    state: ConversationUiState,
    connection: Connection?,
    onConnect: () -> Unit,
    onOpenConnection: () -> Unit,
    onRetryUploads: () -> Unit,
) {
    if (state.isResponding) return
    when {
        connection?.isConnected != true -> {
            ConnectionFeedback(connection, onConnect, onOpenConnection)
        }

        state.workspace == null -> {
            ComposerNotice(
                localizedString(Res.string.composer_workspace_missing),
                localizedString(Res.string.connection_manage),
                onOpenConnection,
            )
        }

        state.model == null -> {
            ComposerNotice(
                localizedString(Res.string.composer_model_missing),
                localizedString(Res.string.connection_manage),
                onOpenConnection,
            )
        }

        state.attachments.any { it.status == AttachmentStatus.Failed } -> {
            ComposerNotice(
                localizedString(Res.string.composer_failed_uploads),
                localizedString(Res.string.composer_retry_uploads),
                onRetryUploads,
            )
        }
    }
}

@Composable
private fun ConnectionFeedback(
    connection: Connection?,
    onConnect: () -> Unit,
    onOpen: () -> Unit,
) {
    when (connection?.runtime?.status) {
        null -> {
            ComposerNotice(localizedString(Res.string.composer_removed), localizedString(Res.string.connection_manage), onOpen)
        }

        ConnectionStatus.Connecting -> {
            ComposerNotice(localizedString(Res.string.composer_connecting))
        }

        ConnectionStatus.Disconnecting -> {
            ComposerNotice(localizedString(Res.string.composer_disconnecting))
        }

        ConnectionStatus.Disconnected -> {
            ComposerNotice(localizedString(Res.string.composer_disconnected), localizedString(Res.string.composer_reconnect), onConnect)
        }

        ConnectionStatus.Failed -> {
            val failure = connection.runtime.failure
            val needsConfiguration = failure == ConnectionFailure.Credentials || failure == ConnectionFailure.Certificate
            ComposerNotice(
                localizedString(failure?.description ?: Res.string.composer_connection_failed),
                localizedString(if (needsConfiguration) Res.string.connection_manage else Res.string.retry_connection),
                if (needsConfiguration) onOpen else onConnect,
            )
        }

        ConnectionStatus.Connected -> {
            return
        }
    }
}

@Composable
private fun ComposerNotice(
    message: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        if (actionLabel != null) {
            TextButton(
                text = actionLabel,
                onClick = onAction,
                colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}
