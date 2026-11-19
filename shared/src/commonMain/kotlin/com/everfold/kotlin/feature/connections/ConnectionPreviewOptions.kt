package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_success
import everfold.shared.generated.resources.preview_certificate
import everfold.shared.generated.resources.preview_credentials
import everfold.shared.generated.resources.preview_result
import everfold.shared.generated.resources.preview_success
import everfold.shared.generated.resources.preview_timeout
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            ConnectionPreviewResult.Success -> Res.string.preview_success
            ConnectionPreviewResult.Timeout -> Res.string.preview_timeout
            ConnectionPreviewResult.Credentials -> Res.string.preview_credentials
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
        Button(onClick = { expanded = true }) {
            Text(
                text = localizedString(Res.string.preview_result) + " · " + localizedString(selected.label),
                style = MiuixTheme.textStyles.button,
            )
        }
        OverlayListPopup(show = expanded, onDismissRequest = { expanded = false }) {
            ListPopupColumn {
                ConnectionPreviewResult.entries.forEach { result ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(result)
                                    expanded = false
                                }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = localizedString(result.label),
                            modifier = Modifier.weight(1f),
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = if (result == selected) FontWeight.SemiBold else FontWeight.Medium,
                            color =
                                if (result == selected) {
                                    MiuixTheme.colorScheme.primary
                                } else {
                                    MiuixTheme.colorScheme.onSurfaceContainer
                                },
                        )
                        if (result == selected) {
                            AppIcon(AppSymbol.Check, modifier = Modifier.size(16.dp), tint = MiuixTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
