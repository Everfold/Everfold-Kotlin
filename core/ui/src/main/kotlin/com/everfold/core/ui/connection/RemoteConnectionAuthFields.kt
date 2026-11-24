package com.everfold.core.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.everfold.core.model.CertificatePayload
import com.everfold.core.model.NatsAuthType
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.model.ZenohAuthType
import com.everfold.core.ui.R

@Composable
internal fun NatsAuthFields(
    config: RemoteTransportConfig.Nats,
    enabled: Boolean,
    onChange: (RemoteTransportConfig.Nats) -> Unit,
) {
    when (config.authType) {
        NatsAuthType.NONE -> {
            Unit
        }

        NatsAuthType.TOKEN -> {
            SecretTextField(
                value = config.token,
                onValueChange = { onChange(config.copy(token = it)) },
                label = stringResource(R.string.conn_token_label),
                enabled = enabled,
            )
        }

        NatsAuthType.USER_PASSWORD -> {
            UserPasswordFields(
                username = config.username,
                password = config.password,
                enabled = enabled,
                onUsernameChange = { onChange(config.copy(username = it)) },
                onPasswordChange = { onChange(config.copy(password = it)) },
            )
        }

        NatsAuthType.MTLS -> {
            MtlsCertSlots(
                caCert = config.caCert,
                clientCert = config.clientCert,
                clientKey = config.clientKey,
                enabled = enabled,
                onCaCertChange = { onChange(config.copy(caCert = it)) },
                onClientCertChange = { onChange(config.copy(clientCert = it)) },
                onClientKeyChange = { onChange(config.copy(clientKey = it)) },
            )
        }
    }
}

@Composable
internal fun ZenohAuthFields(
    config: RemoteTransportConfig.Zenoh,
    enabled: Boolean,
    onChange: (RemoteTransportConfig.Zenoh) -> Unit,
) {
    when (config.authType) {
        ZenohAuthType.NONE -> {
            Unit
        }

        ZenohAuthType.USER_PASSWORD -> {
            UserPasswordFields(
                username = config.username,
                password = config.password,
                enabled = enabled,
                onUsernameChange = { onChange(config.copy(username = it)) },
                onPasswordChange = { onChange(config.copy(password = it)) },
            )
        }

        ZenohAuthType.TLS_ONE_WAY -> {
            CertificateSlotCard(
                title = stringResource(R.string.conn_cert_ca_title),
                payload = config.caCert,
                enabled = enabled,
                onSelectPayload = { onChange(config.copy(caCert = it)) },
            )
        }

        ZenohAuthType.MTLS -> {
            MtlsCertSlots(
                caCert = config.caCert,
                clientCert = config.clientCert,
                clientKey = config.clientKey,
                enabled = enabled,
                onCaCertChange = { onChange(config.copy(caCert = it)) },
                onClientCertChange = { onChange(config.copy(clientCert = it)) },
                onClientKeyChange = { onChange(config.copy(clientKey = it)) },
            )
        }
    }
}

@Composable
private fun UserPasswordFields(
    username: String,
    password: String,
    enabled: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(stringResource(R.string.conn_username_label)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        SecretTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.conn_password_label),
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(
                onClick = { visible = !visible },
                shapes = IconButtonDefaults.shapes(),
                enabled = enabled,
            ) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription =
                        stringResource(
                            if (visible) R.string.conn_secret_hide else R.string.conn_secret_show,
                        ),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MtlsCertSlots(
    caCert: CertificatePayload?,
    clientCert: CertificatePayload?,
    clientKey: CertificatePayload?,
    enabled: Boolean,
    onCaCertChange: (CertificatePayload?) -> Unit,
    onClientCertChange: (CertificatePayload?) -> Unit,
    onClientKeyChange: (CertificatePayload?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CertificateSlotCard(
            title = stringResource(R.string.conn_cert_ca_title),
            payload = caCert,
            enabled = enabled,
            onSelectPayload = onCaCertChange,
        )
        CertificateSlotCard(
            title = stringResource(R.string.conn_cert_client_title),
            payload = clientCert,
            enabled = enabled,
            onSelectPayload = onClientCertChange,
        )
        CertificateSlotCard(
            title = stringResource(R.string.conn_cert_key_title),
            payload = clientKey,
            enabled = enabled,
            onSelectPayload = onClientKeyChange,
        )
    }
}
