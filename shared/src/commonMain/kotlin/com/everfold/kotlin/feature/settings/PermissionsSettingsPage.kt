package com.everfold.kotlin.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.onboarding.OnboardingPermissionCard
import com.everfold.kotlin.feature.onboarding.OnboardingPermissionItem
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.permissions_settings_hint

/** 设置页中的系统权限检查与重新申请面板（仅在需要运行时系统权限的平台展示）。 */
@Composable
internal fun PermissionsSettingsPage(
    items: List<OnboardingPermissionItem>,
    onRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            localizedString(Res.string.permissions_settings_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { item ->
            OnboardingPermissionCard(item = item, onRequest = { onRequest(item.id) })
        }
    }
}
