package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.connection_cached_info
import everfold.shared.generated.resources.connection_configuration
import everfold.shared.generated.resources.connection_connect
import everfold.shared.generated.resources.connection_disconnect
import everfold.shared.generated.resources.connection_disconnecting
import everfold.shared.generated.resources.connection_edit
import everfold.shared.generated.resources.connection_endpoint
import everfold.shared.generated.resources.connection_info_unavailable
import everfold.shared.generated.resources.connection_kernel_version
import everfold.shared.generated.resources.connection_last_connected
import everfold.shared.generated.resources.connection_never_connected
import everfold.shared.generated.resources.connection_new_session
import everfold.shared.generated.resources.connection_overview
import everfold.shared.generated.resources.connection_remove
import everfold.shared.generated.resources.connection_remove_hint
import everfold.shared.generated.resources.connection_remove_title
import everfold.shared.generated.resources.retry_connection
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 所有连接共享概览与配置离线时保留编辑入口和最后一次内核信息 */
@Composable
internal fun ConnectionDetails(
    connection: Connection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSave: (ConnectionDraft) -> Boolean,
    onRemove: () -> Unit,
    onNewConversation: () -> Unit,
    modifier: Modifier = Modifier,
    overviewExtra: @Composable () -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var removing by rememberSaveable { mutableStateOf(false) }
    val pages = rememberSaveableStateHolder()
    Column(modifier.fillMaxSize()) {
        ConnectionDetailsHeader(connection, onConnect, onDisconnect, onNewConversation)
        TabRowWithContour(
            tabs = listOf(localizedString(Res.string.connection_overview), localizedString(Res.string.connection_configuration)),
            selectedTabIndex = tab,
            onTabSelected = { tab = it },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Box(Modifier.weight(1f)) {
            pages.SaveableStateProvider(tab) {
                if (tab == 0) {
                    ConnectionOverview(connection, onEdit = { tab = 1 }, onRemove = { removing = true }, overviewExtra)
                } else {
                    ConnectionEditor(connection.profile, onSave = { draft, _ -> onSave(draft) })
                }
            }
        }
    }
    ConnectionRemoveDialog(
        show = removing,
        connectionName = connection.profile.name.resolve(),
        onDismiss = { removing = false },
        onConfirm = {
            removing = false
            onRemove()
        },
    )
}

@Composable
private fun ConnectionDetailsHeader(
    connection: Connection,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onNewConversation: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = connection.profile.name.resolve(),
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ConnectionStatusLabel(connection.runtime.status)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ConnectionAction(connection.runtime.status, onConnect, onDisconnect)
            Button(onClick = onNewConversation, enabled = connection.isConnected) {
                Text(localizedString(Res.string.connection_new_session), style = MiuixTheme.textStyles.button)
            }
        }
    }
}

@Composable
private fun ConnectionOverview(
    connection: Connection,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    extraContent: @Composable () -> Unit,
) {
    val runtime = connection.runtime
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (runtime.status == ConnectionStatus.Connecting || runtime.status == ConnectionStatus.Disconnecting) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), progress = null)
            }
            runtime.failure?.let { failure ->
                ConnectionFailureCard(localizedString(failure.description), onEdit)
            }
            ConnectionOverviewCard(connection, runtime)
            extraContent()
            if (connection.profile.canRemove) {
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.errorContainer),
                ) {
                    Text(
                        text = localizedString(Res.string.connection_remove),
                        style = MiuixTheme.textStyles.button,
                        color = MiuixTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionFailureCard(
    message: String,
    onEdit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.errorContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = message, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onErrorContainer)
            TextButton(
                text = localizedString(Res.string.connection_edit),
                onClick = onEdit,
                colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ConnectionOverviewCard(
    connection: Connection,
    runtime: ConnectionRuntimeState,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ConnectionInfoRow(localizedString(Res.string.connection_endpoint), connection.profile.endpointSummary())
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        if (!connection.isConnected && runtime.lastConnectedAt != null) {
            Text(
                text = localizedString(Res.string.connection_cached_info),
                modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        ConnectionInfoRow(
            localizedString(Res.string.connection_kernel_version),
            runtime.kernelVersion ?: localizedString(Res.string.connection_info_unavailable),
        )
        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        ConnectionInfoRow(
            localizedString(Res.string.connection_last_connected),
            runtime.lastConnectedAt
                ?.toString()
                ?.substringBefore('.')
                ?.replace('T', ' ')
                ?.removeSuffix("Z")
                ?.plus(" UTC")
                ?: localizedString(Res.string.connection_never_connected),
        )
    }
}

@Composable
private fun ConnectionInfoRow(
    label: String,
    value: String,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(text = value, style = MiuixTheme.textStyles.body1)
    }
}

@Composable
private fun ConnectionAction(
    status: ConnectionStatus,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val disconnect = status in listOf(ConnectionStatus.Connected, ConnectionStatus.Connecting, ConnectionStatus.Disconnecting)
    val colors = if (disconnect) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary()
    Button(
        onClick = if (disconnect) onDisconnect else onConnect,
        enabled = status != ConnectionStatus.Disconnecting,
        colors = colors,
    ) {
        Text(
            text =
                localizedString(
                    when (status) {
                        ConnectionStatus.Connected -> Res.string.connection_disconnect
                        ConnectionStatus.Connecting -> Res.string.cancel
                        ConnectionStatus.Disconnecting -> Res.string.connection_disconnecting
                        ConnectionStatus.Failed -> Res.string.retry_connection
                        ConnectionStatus.Disconnected -> Res.string.connection_connect
                    },
                ),
            style = MiuixTheme.textStyles.button,
        )
    }
}

@Composable
private fun ConnectionRemoveDialog(
    show: Boolean,
    connectionName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    GlassDialog(
        show = show,
        onDismissRequest = onDismiss,
        backdrop = LocalAppBackdrop.current,
        title = localizedString(Res.string.connection_remove_title, connectionName),
        summary = localizedString(Res.string.connection_remove_hint),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassDialogButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(text = localizedString(Res.string.cancel), style = MiuixTheme.textStyles.button)
            }
            GlassDialogButton(onClick = onConfirm, modifier = Modifier.weight(1f), role = GlassDialogButtonRole.Destructive) {
                Text(text = localizedString(Res.string.connection_remove), style = MiuixTheme.textStyles.button)
            }
        }
    }
}
