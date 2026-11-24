package com.everfold.core.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.model.NatsAuthType
import com.everfold.core.model.NatsProtocol
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.TransportBackend
import com.everfold.core.model.ZenohAccessMode
import com.everfold.core.model.ZenohAuthType
import com.everfold.core.model.ZenohProtocol
import com.everfold.core.ui.R

private const val HOST_WEIGHT = 2f
private const val PORT_WEIGHT = 1f

@Composable
fun RemoteConnectionForm(
    transport: RemoteTransportConfig,
    onTransportChange: (RemoteTransportConfig) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showProfileFields: Boolean = false,
    profileName: String = "",
    profileNote: String = "",
    onProfileNameChange: (String) -> Unit = {},
    onProfileNoteChange: (String) -> Unit = {},
    errorText: String? = null,
) {
    val backend = if (transport is RemoteTransportConfig.Nats) TransportBackend.NATS else TransportBackend.ZENOH

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackendSelectorRow(
            selected = backend,
            enabled = enabled,
            onSelect = { next ->
                onTransportChange(
                    if (next == TransportBackend.NATS) RemoteTransportConfig.Nats() else RemoteTransportConfig.Zenoh(),
                )
            },
        )
        if (showProfileFields) {
            ProfileMetaFields(
                name = profileName,
                note = profileNote,
                enabled = enabled,
                onNameChange = onProfileNameChange,
                onNoteChange = onProfileNoteChange,
            )
        }
        when (transport) {
            is RemoteTransportConfig.Nats -> {
                NatsConfigSection(config = transport, enabled = enabled, onChange = onTransportChange)
            }

            is RemoteTransportConfig.Zenoh -> {
                ZenohConfigSection(config = transport, enabled = enabled, onChange = onTransportChange)
            }
        }
        if (errorText != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun BackendSelectorRow(
    selected: TransportBackend,
    enabled: Boolean,
    onSelect: (TransportBackend) -> Unit,
) {
    EverfoldConnectedButtonGroup(
        options = TransportBackend.entries,
        selected = selected,
        onSelect = { backend -> if (selected != backend) onSelect(backend) },
        label = { backend -> if (backend == TransportBackend.NATS) "NATS" else "Zenoh" },
        enabled = enabled,
    )
}

@Composable
private fun ProfileMetaFields(
    name: String,
    note: String,
    enabled: Boolean,
    onNameChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.conn_profile_name_label)) },
            placeholder = { Text(stringResource(R.string.conn_profile_name_hint)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.conn_profile_note_label)) },
            enabled = enabled,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NatsConfigSection(
    config: RemoteTransportConfig.Nats,
    enabled: Boolean,
    onChange: (RemoteTransportConfig.Nats) -> Unit,
) {
    val portPlaceholder =
        if (config.protocol == NatsProtocol.WEBSOCKET) "443" else RemoteTransportConfig.DEFAULT_NATS_PORT

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HostPortRow(
            host = config.host,
            port = config.port,
            portPlaceholder = portPlaceholder,
            hostRequired = true,
            enabled = enabled,
            onHostChange = { onChange(config.copy(host = it)) },
            onPortChange = { onChange(config.copy(port = it)) },
        )
        Text(text = stringResource(R.string.conn_protocol_label), style = MaterialTheme.typography.titleSmall)
        EverfoldConnectedButtonGroup(
            options = NatsProtocol.entries,
            selected = config.protocol,
            onSelect = { onChange(config.copy(protocol = it)) },
            label = { stringResource(it.labelRes()) },
            enabled = enabled,
            isItemEnabled = { proto -> !(config.authType == NatsAuthType.MTLS && proto == NatsProtocol.TCP) },
        )
        Text(text = stringResource(R.string.conn_auth_label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NatsAuthType.entries.forEach { auth ->
                FilterChip(
                    selected = config.authType == auth,
                    enabled = enabled,
                    onClick = {
                        val nextProto =
                            if (auth == NatsAuthType.MTLS && config.protocol == NatsProtocol.TCP) {
                                NatsProtocol.TLS
                            } else {
                                config.protocol
                            }
                        onChange(config.copy(authType = auth, protocol = nextProto))
                    },
                    label = { Text(stringResource(auth.labelRes())) },
                )
            }
        }
        if (config.authType == NatsAuthType.MTLS) {
            Text(
                text = stringResource(R.string.conn_nats_mtls_tls_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NatsAuthFields(config = config, enabled = enabled, onChange = onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ZenohConfigSection(
    config: RemoteTransportConfig.Zenoh,
    enabled: Boolean,
    onChange: (RemoteTransportConfig.Zenoh) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ZenohAccessSection(accessMode = config.accessMode, enabled = enabled) { mode ->
            onChange(config.copy(accessMode = mode))
        }
        HostPortRow(
            host = config.host,
            port = config.port,
            portPlaceholder = RemoteTransportConfig.DEFAULT_ZENOH_PORT,
            hostRequired = config.accessMode == ZenohAccessMode.EXPLICIT,
            enabled = enabled,
            onHostChange = { onChange(config.copy(host = it)) },
            onPortChange = { onChange(config.copy(port = it)) },
        )
        Text(text = stringResource(R.string.conn_protocol_label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenohProtocol.entries.forEach { proto ->
                FilterChip(
                    selected = config.protocol == proto,
                    enabled = enabled,
                    onClick = { onChange(config.copy(protocol = proto)) },
                    label = { Text(stringResource(proto.labelRes())) },
                )
            }
        }
        Text(text = stringResource(R.string.conn_auth_label), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ZenohAuthType.entries.forEach { auth ->
                FilterChip(
                    selected = config.authType == auth,
                    enabled = enabled,
                    onClick = { onChange(config.copy(authType = auth)) },
                    label = { Text(stringResource(auth.labelRes())) },
                )
            }
        }
        ZenohAuthFields(config = config, enabled = enabled, onChange = onChange)
    }
}

@Composable
private fun ZenohAccessSection(
    accessMode: ZenohAccessMode,
    enabled: Boolean,
    onSelect: (ZenohAccessMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = stringResource(R.string.conn_zenoh_access_label), style = MaterialTheme.typography.titleSmall)
        EverfoldConnectedButtonGroup(
            options = ZenohAccessMode.entries,
            selected = accessMode,
            onSelect = onSelect,
            label = { mode -> stringResource(mode.labelRes()) },
            enabled = enabled,
        )
        if (accessMode == ZenohAccessMode.P2P) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = stringResource(R.string.conn_zenoh_p2p_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

@Composable
private fun HostPortRow(
    host: String,
    port: String,
    portPlaceholder: String,
    hostRequired: Boolean,
    enabled: Boolean,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = host,
            onValueChange = onHostChange,
            label = {
                Text(
                    stringResource(
                        if (hostRequired) R.string.conn_host_label else R.string.conn_host_optional_label,
                    ),
                )
            },
            placeholder = { Text(stringResource(R.string.conn_host_hint)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.weight(HOST_WEIGHT),
        )
        OutlinedTextField(
            value = port,
            onValueChange = { value -> onPortChange(value.filter { it.isDigit() }) },
            label = { Text(stringResource(R.string.conn_port_label)) },
            placeholder = { Text(portPlaceholder) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(PORT_WEIGHT),
        )
    }
}
