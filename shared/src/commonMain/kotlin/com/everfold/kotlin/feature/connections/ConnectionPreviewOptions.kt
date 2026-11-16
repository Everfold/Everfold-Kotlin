package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_success
import everfold.shared.generated.resources.preview_certificate
import everfold.shared.generated.resources.preview_credentials
import everfold.shared.generated.resources.preview_result
import everfold.shared.generated.resources.preview_success
import everfold.shared.generated.resources.preview_timeout

/** 连接测试的模拟结果界面演示不发起真实网络连接 */
internal enum class ConnectionPreviewResult { Success, Timeout, Credentials, Certificate }

internal val ConnectionPreviewResult.failure: ConnectionFailure?
    get() =
        when (this) {
            ConnectionPreviewResult.Success -> null
            ConnectionPreviewResult.Timeout -> ConnectionFailure.Timeout
            ConnectionPreviewResult.Credentials -> ConnectionFailure.Credentials
            ConnectionPreviewResult.Certificate -> ConnectionFailure.Certificate
        }

internal val ConnectionPreviewResult.description
    get() = failure?.description ?: Res.string.connection_success

private val ConnectionPreviewResult.label
    get() =
        when (this) {

            // 成功
            ConnectionPreviewResult.Success -> Res.string.preview_success

            // 连接超时
            ConnectionPreviewResult.Timeout -> Res.string.preview_timeout

            // 凭据错误
            ConnectionPreviewResult.Credentials -> Res.string.preview_credentials

            // 证书错误
            ConnectionPreviewResult.Certificate -> Res.string.preview_certificate
        }

/** 选择下一次模拟测试的结果标签和提示文案由上方资源映射提供 */
@Composable
internal fun ConnectionPreviewOptions(
    selected: ConnectionPreviewResult,
    onSelect: (ConnectionPreviewResult) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            // 模拟测试结果
            Text(localizedString(Res.string.preview_result) + " · " + localizedString(selected.label))
        }
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            ConnectionPreviewResult.entries.forEach { result ->
                DropdownMenuItem(text = { Text(localizedString(result.label)) }, onClick = {
                    onSelect(result)
                    expanded = false
                })
            }
        }
    }
}
