package com.everfold.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutScreen(
    appInfo: AppInfo,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                title = { Text("关于 Everfold") },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { AboutHeroCard(versionName = appInfo.versionName) }
            item {
                AboutLinksCard(
                    appInfo = appInfo,
                    onOpenRepo = { openUrlInCustomTab(context, appInfo.repositoryUrl) },
                    onOpenLicense = {
                        openUrlInCustomTab(context, "${appInfo.repositoryUrl}/blob/main/LICENSE")
                    },
                )
            }
        }
    }
}

@Composable
private fun AboutHeroCard(versionName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Everfold",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "跨节点 AI 智能体控制与可观测性终端",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            EverfoldStatusPill(text = "v$versionName")
        }
    }
}

@Composable
private fun AboutLinksCard(
    appInfo: AppInfo,
    onOpenRepo: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AboutInfoRow(
                icon = Icons.Rounded.Info,
                title = "软件版本",
                subtitle = "v${appInfo.versionName} (Build ${appInfo.versionCode})",
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AboutInfoRow(
                icon = Icons.Rounded.Link,
                title = "GitHub 仓库",
                subtitle = appInfo.repositoryUrl,
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = onOpenRepo,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AboutInfoRow(
                icon = Icons.Rounded.Gavel,
                title = "开源许可证",
                subtitle = "${appInfo.licenseName} License",
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                onClick = onOpenLicense,
            )
        }
    }
}

@Composable
private fun AboutInfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
