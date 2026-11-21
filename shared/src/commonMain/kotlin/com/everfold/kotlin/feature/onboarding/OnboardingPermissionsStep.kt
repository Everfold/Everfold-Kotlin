package com.everfold.kotlin.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.permission_granted
import everfold.shared.generated.resources.permission_manage
import everfold.shared.generated.resources.setup_permissions_hint

@Composable
internal fun OnboardingPermissionsStep(
    items: List<OnboardingPermissionItem>,
    onRequest: (String) -> Unit,
    modifier: Modifier = Modifier,
    onManage: ((String) -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            localizedString(Res.string.setup_permissions_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        items.forEach { item ->
            val manageAction =
                if (onManage != null && item.id == PERMISSION_ID_BATTERY_OPTIMIZATION) {
                    { onManage(item.id) }
                } else {
                    null
                }
            OnboardingPermissionCard(
                item = item,
                onRequest = { onRequest(item.id) },
                onManage = manageAction,
            )
        }
    }
}

@Composable
internal fun OnboardingPermissionCard(
    item: OnboardingPermissionItem,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onManage: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title.resolve(), style = MaterialTheme.typography.titleMedium)
                Text(
                    item.description.resolve(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.granted) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                        AppIcon(AppSymbol.Check, Modifier.size(16.dp))
                    }
                    Text(
                        localizedString(Res.string.permission_granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (onManage != null) {
                        TextButton(onClick = onManage) {
                            Text(localizedString(Res.string.permission_manage))
                        }
                    }
                }
            } else {
                FilledTonalButton(onClick = onRequest) {
                    Text(item.actionLabel.resolve())
                }
            }
        }
    }
}
