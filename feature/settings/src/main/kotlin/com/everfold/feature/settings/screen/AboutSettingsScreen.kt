package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.core.model.AppInfo
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import com.everfold.feature.settings.system.openUrlInCustomTab
import org.koin.androidx.compose.koinViewModel

@Composable
fun AboutSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val appInfo = viewModel.appInfo

    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_about_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { AboutBrandHeader() }
            item {
                AboutLinksGroup(
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutBrandHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                EverfoldLogo(modifier = Modifier.size(56.dp))
            }
        }
        Text(
            text = "Everfold",
            style = MaterialTheme.typography.headlineMediumEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AboutLinksGroup(
    appInfo: AppInfo,
    onOpenRepo: () -> Unit,
    onOpenLicense: () -> Unit,
) {
    val itemCount = 3
    EverfoldGroupedList {
        EverfoldGroupedItem(index = 0, count = itemCount) {
            AboutInfoRow(
                icon = Icons.Rounded.Info,
                title = stringResource(R.string.about_version_label),
                subtitle =
                    stringResource(
                        R.string.about_version_value,
                        appInfo.versionName,
                        appInfo.versionCode,
                    ),
            )
        }
        EverfoldGroupedItem(index = 1, count = itemCount, onClick = onOpenRepo) {
            AboutInfoRow(
                icon = Icons.Rounded.Link,
                title = stringResource(R.string.about_repo_label),
                subtitle = appInfo.repositoryUrl,
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
            )
        }
        EverfoldGroupedItem(index = 2, count = itemCount, onClick = onOpenLicense) {
            AboutInfoRow(
                icon = Icons.Rounded.Gavel,
                title = stringResource(R.string.about_license_label),
                subtitle = stringResource(R.string.about_license_value, appInfo.licenseName),
                trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
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
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
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
