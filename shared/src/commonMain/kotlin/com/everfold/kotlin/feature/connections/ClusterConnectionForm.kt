package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.choose_file
import everfold.shared.generated.resources.client_private_key
import everfold.shared.generated.resources.client_public_key
import everfold.shared.generated.resources.cluster_central
import everfold.shared.generated.resources.cluster_cloud
import everfold.shared.generated.resources.cluster_lan
import everfold.shared.generated.resources.error_account
import everfold.shared.generated.resources.error_certificate
import everfold.shared.generated.resources.error_ip
import everfold.shared.generated.resources.error_password
import everfold.shared.generated.resources.error_port
import everfold.shared.generated.resources.hide_password
import everfold.shared.generated.resources.import_hint
import everfold.shared.generated.resources.import_preview
import everfold.shared.generated.resources.ip_address
import everfold.shared.generated.resources.lan_hint
import everfold.shared.generated.resources.no_file
import everfold.shared.generated.resources.password
import everfold.shared.generated.resources.port
import everfold.shared.generated.resources.remove_file
import everfold.shared.generated.resources.replace_file
import everfold.shared.generated.resources.scan_file
import everfold.shared.generated.resources.scan_preview
import everfold.shared.generated.resources.server_certificate
import everfold.shared.generated.resources.show_password
import everfold.shared.generated.resources.use_sample
import org.jetbrains.compose.resources.StringResource

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
        FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClusterMethod.entries.forEach { method ->
                val label =
                    when (method) {
                        // 局域网
                        ClusterMethod.Lan -> Res.string.cluster_lan

                        // 中心节点
                        ClusterMethod.Central -> Res.string.cluster_central

                        // XX云
                        ClusterMethod.Cloud -> Res.string.cluster_cloud
                    }
                FilterChip(
                    method == form.method,
                    onClick = { onChange(form.copy(method = method)) },
                    label = { Text(localizedString(label)) },
                )
            }
        }
        if (form.method != ClusterMethod.Cloud) {
            // 可留空以发现局域网节点或手动填写 IP 和端口
            if (form.method == ClusterMethod.Lan) Text(localizedString(Res.string.lan_hint), style = MaterialTheme.typography.bodySmall)
            SetupColumns(wide, first = {
                SetupField(
                    // IP 地址
                    Res.string.ip_address,
                    form.ip,
                    { onChange(form.copy(ip = it)) },
                    ClusterField.Ip in errors,
                    // 请输入有效的 IPv4 或 IPv6 地址
                    Res.string.error_ip,
                    showErrors,
                )
            }, second = {
                SetupField(
                    // 端口
                    Res.string.port,
                    form.port,
                    { onChange(form.copy(port = it)) },
                    ClusterField.Port in errors,
                    // 请输入 1-65535 范围内的端口
                    Res.string.error_port,
                    showErrors,
                    keyboard = KeyboardType.Number,
                )
            })
        }
        if (form.method == ClusterMethod.Central) {
            // 验证方式
            Text(localizedString(Res.string.auth_method), style = MaterialTheme.typography.titleMedium)
            FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ClusterAuth.entries.forEach { auth ->
                    val label =
                        when (auth) {
                            // 无
                            ClusterAuth.None -> Res.string.auth_none

                            // 账号密码
                            ClusterAuth.Password -> Res.string.auth_password

                            // 服务器验证
                            ClusterAuth.Certificate -> Res.string.auth_certificate

                            // mTLS
                            ClusterAuth.Mtls -> Res.string.auth_mtls
                        }
                    FilterChip(form.auth == auth, onClick = { onChange(form.copy(auth = auth)) }, label = { Text(localizedString(label)) })
                }
            }
        }
        if (form.method == ClusterMethod.Cloud || (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Password)) {
            SetupColumns(wide, first = {
                // 用户名
                SetupField(
                    Res.string.account,
                    form.account,
                    {
                        onChange(form.copy(account = it))
                    },
                    ClusterField.Account in errors,
                    // 请输入用户名
                    Res.string.error_account,
                    showErrors,
                )
            }, second = {
                SetupField(
                    // 密码
                    Res.string.password,
                    form.password,
                    { onChange(form.copy(password = it)) },
                    ClusterField.Password in errors,
                    // 请输入密码
                    Res.string.error_password,
                    showErrors,
                    secret = true,
                )
            })
        }
        if (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Certificate) {
            // 服务器证书
            FileImportField(Res.string.server_certificate, form.serverCertificate, "server.pem", {
                onChange(form.copy(serverCertificate = it))
            }, showErrors)
        }
        if (form.method == ClusterMethod.Central && form.auth == ClusterAuth.Mtls) {
            FileImportField(
                // CA 证书
                Res.string.ca_certificate,
                form.caCertificate,
                "ca.pem",
                { onChange(form.copy(caCertificate = it)) },
                showErrors,
            )

            // 客户端证书
            FileImportField(Res.string.client_public_key, form.clientPublicKey, "client-cert.pem", {
                onChange(form.copy(clientPublicKey = it))
            }, showErrors)

            // 客户端私钥
            FileImportField(Res.string.client_private_key, form.clientPrivateKey, "client-private.pem", {
                onChange(form.copy(clientPrivateKey = it))
            }, showErrors)
        }
    }
}

/** 字段失焦或提交后才显示错误支持密码显隐标签与错误中文文案由调用处传入 */
@Composable
private fun SetupField(
    label: StringResource,
    value: String,
    onChange: (String) -> Unit,
    invalid: Boolean,
    error: StringResource,
    showErrors: Boolean,
    secret: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    var focusedOnce by remember(label) { mutableStateOf(false) }
    var touched by remember(label) { mutableStateOf(false) }
    var reveal by remember(label) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val showError = invalid && (showErrors || touched)
    OutlinedTextField(
        value,
        onChange,
        modifier =
            Modifier.fillMaxWidth().onFocusChanged {
                if (it.isFocused) {
                    focusedOnce = true
                } else if (focusedOnce) {
                    touched = true
                }
            },
        label = { Text(localizedString(label)) },
        singleLine = true,
        isError = showError,
        supportingText = { if (showError) Text(localizedString(error)) },
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (secret) KeyboardType.Password else keyboard,
                imeAction = if (secret) ImeAction.Done else ImeAction.Next,
            ),
        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }, onDone = { focus.clearFocus() }),
        visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = {
            if (secret) {
                TextButton(onClick = { reveal = !reveal }) {
                    // 隐藏 / 显示
                    Text(localizedString(if (reveal) Res.string.hide_password else Res.string.show_password))
                }
            }
        },
    )
}

/** 模拟证书导入与扫码只回传示例文件名不访问设备文件或相机 */
@Composable
private fun FileImportField(
    label: StringResource,
    value: String?,
    sampleName: String,
    onImport: (String?) -> Unit,
    showErrors: Boolean,
) {
    var importing by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(localizedString(label), style = MaterialTheme.typography.titleSmall)

        // 尚未导入
        Text(value ?: localizedString(Res.string.no_file), style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                scanning = false
                importing = true
            }) {
                // 选择本地文件 / 替换文件
                Text(localizedString(if (value == null) Res.string.choose_file else Res.string.replace_file))
            }
            OutlinedButton(onClick = {
                scanning = true
                importing = true
            }) {
                // 扫描二维码
                Text(localizedString(Res.string.scan_file))
            }
            if (value != null) {
                TextButton(onClick = {
                    touched = true
                    onImport(null)
                }) {
                    // 移除
                    Text(localizedString(Res.string.remove_file))
                }
            }
        }
        if ((showErrors || touched) &&
            value.isNullOrBlank()
        ) {
            Text(
                // 请导入此项需要的文件
                localizedString(Res.string.error_certificate),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (importing) {
        AlertDialog(
            onDismissRequest = {
                importing = false
            },
            // 模拟扫码导入 / 模拟文件导入
            title = { Text(localizedString(if (scanning) Res.string.scan_preview else Res.string.import_preview)) },
            // 当前为界面预览使用示例文件完成导入不读取设备文件或相机
            text = { Text(localizedString(Res.string.import_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    onImport(sampleName)
                    importing = false
                }) {
                    // 使用示例文件
                    Text(localizedString(Res.string.use_sample))
                }
            },
            // 取消
            dismissButton = { TextButton(onClick = { importing = false }) { Text(localizedString(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun SetupColumns(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        maxItemsInEachRow = if (wide) 2 else 1,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) { first() }
        Column(Modifier.weight(1f)) { second() }
    }
}
