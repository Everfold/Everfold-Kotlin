package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_success
import everfold.shared.generated.resources.preview_certificate
import everfold.shared.generated.resources.preview_credentials
import everfold.shared.generated.resources.preview_result
import everfold.shared.generated.resources.preview_success
import everfold.shared.generated.resources.preview_timeout
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.glass.GlassDropdownPreference

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

/** 选择下一次模拟测试的结果：采用 HyperOS 卡片内 GlassDropdownPreference。 */
@Composable
internal fun ConnectionPreviewOptions(
    selected: ConnectionPreviewResult,
    onSelect: (ConnectionPreviewResult) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        GlassDropdownPreference(
            title = localizedString(Res.string.preview_result),
            items = ConnectionPreviewResult.entries.map { localizedString(it.label) },
            selectedIndex = ConnectionPreviewResult.entries.indexOf(selected),
            onSelectedIndexChange = { onSelect(ConnectionPreviewResult.entries[it]) },
            backdrop = LocalAppBackdrop.current,
        )
    }
}
