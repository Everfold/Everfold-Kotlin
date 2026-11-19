package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.github_repository
import everfold.shared.generated.resources.kernel_github_repository
import everfold.shared.generated.resources.logo
import everfold.shared.generated.resources.official_website
import everfold.shared.generated.resources.open_in_browser
import everfold.shared.generated.resources.open_source_license
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于页展示品牌可用版本信息以及官网源码和许可链接 */
@Composable
internal fun AboutSettingsPage() {
    val version = appVersion()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(75.dp),
            )
            Text("Everfold", style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.SemiBold)
            if (version != null) {
                Text(
                    text = "v$version",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            AboutLink(
                title = localizedString(Res.string.official_website),
                detail = "everfold.org",
                url = "https://everfold.org/",
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            AboutLink(
                title = localizedString(Res.string.github_repository),
                detail = "github.com/Everfold/Everfold-Kotlin",
                url = "https://github.com/Everfold/Everfold-Kotlin",
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            AboutLink(
                title = localizedString(Res.string.kernel_github_repository),
                detail = "github.com/Everfold/Beamlet",
                url = "https://github.com/Everfold/Beamlet",
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            AboutLink(
                title = localizedString(Res.string.open_source_license),
                detail = "Apache License, Version 2.0",
                url = "https://www.apache.org/licenses/LICENSE-2.0",
            )
        }
    }
}

@Composable
private fun AboutLink(
    title: String,
    detail: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    val openLabel = localizedString(Res.string.open_in_browser)
    ArrowPreference(
        title = title,
        summary = detail,
        modifier = Modifier.semantics { stateDescription = openLabel },
        endActions = {
            AppIcon(
                AppSymbol.OpenInNew,
                modifier = Modifier.size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
        },
        onClick = { uriHandler.openUri(url) },
    )
}
