package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.account
import everfold.shared.generated.resources.auth_certificate
import everfold.shared.generated.resources.auth_method
import everfold.shared.generated.resources.auth_mtls
import everfold.shared.generated.resources.auth_none
import everfold.shared.generated.resources.auth_password
import everfold.shared.generated.resources.ca_certificate
import everfold.shared.generated.resources.client_private_key
import everfold.shared.generated.resources.client_public_key
import everfold.shared.generated.resources.cluster_central
import everfold.shared.generated.resources.cluster_cloud
import everfold.shared.generated.resources.cluster_lan
import everfold.shared.generated.resources.error_account
import everfold.shared.generated.resources.error_ip
import everfold.shared.generated.resources.error_password
import everfold.shared.generated.resources.error_port
import everfold.shared.generated.resources.ip_address
import everfold.shared.generated.resources.lan_hint
import everfold.shared.generated.resources.password
import everfold.shared.generated.resources.port
import everfold.shared.generated.resources.server_certificate
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 根据连接方式和验证方式展示表单宽窄屏共用字段与校验反馈 */
@Composable
internal fun ClusterConnectionForm(
    form: ClusterSetup,
    wide: Boolean,
    onChange: (ClusterSetup) -> Unit,
    showErrors: Boolean = false,
) {
    val errors = form.invalidFields
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val methodLabels =
            ClusterMethod.entries.map { method ->
                localizedString(
                    when (method) {
                        ClusterMethod.Lan -> Res.string.cluster_lan
                        ClusterMethod.Central -> Res.string.cluster_central
                        ClusterMethod.Cloud -> Res.string.cluster_cloud
                    },
                )
            }
        TabRowWithContour(
            tabs = methodLabels,
            selectedTabIndex = ClusterMethod.entries.indexOf(form.method),
            onTabSelected = { index -> onChange(form.copy(method = ClusterMethod.entries[index])) },
        )
        if (form.method != ClusterMethod.Cloud) {
            ClusterEndpointFields(form, wide, errors, showErrors, onChange)
        }
        if (form.method == ClusterMethod.Central) {
            ClusterAuthSelector(form.auth) { onChange(form.copy(auth = it)) }
        }
        ClusterCredentialsSection(form, wide, errors, showErrors, onChange)
    }
}

@Composable
private fun ClusterEndpointFields(
    form: ClusterSetup,
    wide: Boolean,
    errors: Set<ClusterField>,
    showErrors: Boolean,
    onChange: (ClusterSetup) -> Unit,
) {
    if (form.method == ClusterMethod.Lan) {
        Text(
            text = localizedString(Res.string.lan_hint),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
    SetupColumns(
        wide = wide,
        first = {
            SetupField(
                label = Res.string.ip_address,
                value = form.ip,
                onChange = { onChange(form.copy(ip = it)) },
                invalid = ClusterField.Ip in errors,
                error = Res.string.error_ip,
                showErrors = showErrors,
            )
        },
        second = {
            SetupField(
                label = Res.string.port,
                value = form.port,
                onChange = { onChange(form.copy(port = it)) },
                invalid = ClusterField.Port in errors,
                error = Res.string.error_port,
                showErrors = showErrors,
                keyboard = KeyboardType.Number,
            )
        },
    )
}

@Composable
private fun ClusterAuthSelector(
    selectedAuth: ClusterAuth,
    onSelect: (ClusterAuth) -> Unit,
) {
    SmallTitle(text = localizedString(Res.string.auth_method), insideMargin = PaddingValues(horizontal = 4.dp, vertical = 4.dp))
    val authLabels =
        ClusterAuth.entries.map { auth ->
            localizedString(
                when (auth) {
                    ClusterAuth.None -> Res.string.auth_none
                    ClusterAuth.Password -> Res.string.auth_password
                    ClusterAuth.Certificate -> Res.string.auth_certificate
                    ClusterAuth.Mtls -> Res.string.auth_mtls
                },
            )
        }
    TabRowWithContour(
        tabs = authLabels,
        selectedTabIndex = ClusterAuth.entries.indexOf(selectedAuth),
        onTabSelected = { index -> onSelect(ClusterAuth.entries[index]) },
    )
}

@Composable
private fun ClusterCredentialsSection(
    form: ClusterSetup,
    wide: Boolean,
    errors: Set<ClusterField>,
    showErrors: Boolean,
    onChange: (ClusterSetup) -> Unit,
) {
    val showPasswordFields =
        form.method == ClusterMethod.Cloud || (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Password)
    if (showPasswordFields) {
        SetupColumns(
            wide = wide,
            first = {
                SetupField(
                    label = Res.string.account,
                    value = form.account,
                    onChange = { onChange(form.copy(account = it)) },
                    invalid = ClusterField.Account in errors,
                    error = Res.string.error_account,
                    showErrors = showErrors,
                )
            },
            second = {
                SetupField(
                    label = Res.string.password,
                    value = form.password,
                    onChange = { onChange(form.copy(password = it)) },
                    invalid = ClusterField.Password in errors,
                    error = Res.string.error_password,
                    showErrors = showErrors,
                    secret = true,
                )
            },
        )
    }
    if (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Certificate) {
        FileImportField(
            label = Res.string.server_certificate,
            value = form.serverCertificate,
            sampleName = "server.pem",
            onImport = { onChange(form.copy(serverCertificate = it)) },
            showErrors = showErrors,
        )
    }
    if (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Mtls) {
        ClusterMtlsFields(form, showErrors, onChange)
    }
}

@Composable
private fun ClusterMtlsFields(
    form: ClusterSetup,
    showErrors: Boolean,
    onChange: (ClusterSetup) -> Unit,
) {
    FileImportField(
        label = Res.string.ca_certificate,
        value = form.caCertificate,
        sampleName = "ca.pem",
        onImport = { onChange(form.copy(caCertificate = it)) },
        showErrors = showErrors,
    )
    FileImportField(
        label = Res.string.client_public_key,
        value = form.clientPublicKey,
        sampleName = "client-cert.pem",
        onImport = { onChange(form.copy(clientPublicKey = it)) },
        showErrors = showErrors,
    )
    FileImportField(
        label = Res.string.client_private_key,
        value = form.clientPrivateKey,
        sampleName = "client-private.pem",
        onImport = { onChange(form.copy(clientPrivateKey = it)) },
        showErrors = showErrors,
    )
}
