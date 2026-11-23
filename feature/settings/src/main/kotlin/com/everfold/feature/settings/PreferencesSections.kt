package com.everfold.feature.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AppLanguage
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize

private const val BASE_PREVIEW_SP = 14f

internal data class SystemPermissionStatus(
    val notificationsGranted: Boolean,
    val backgroundUnrestricted: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LanguageCard(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("应用语言", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = language == option,
                        onClick = {
                            onLanguageChange(option)
                            applyAppLanguage(context, option)
                        },
                        label = { Text(option.toLabel()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionPreferencesCard(
    prefs: AppPreferences,
    onChatTextSizeChange: (ChatTextSize) -> Unit,
    onAutoCollapseChange: (Boolean) -> Unit,
    onShowNodeBadgesChange: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("会话文字大小", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChatTextSize.entries.forEach { size ->
                    FilterChip(
                        selected = prefs.chatTextSize == size,
                        onClick = { onChatTextSizeChange(size) },
                        label = { Text(size.toLabel()) },
                    )
                }
            }
            Text(
                text = "预览：Everfold 会话正文与 Markdown 回复字号示例。",
                fontSize = (BASE_PREVIEW_SP * prefs.chatTextSize.scale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingSwitchRow(
                title = "自动折叠长消息",
                subtitle = "较长的用户或智能体回复默认折叠，点击展开全文",
                checked = prefs.autoCollapseLongMessages,
                onCheckedChange = onAutoCollapseChange,
            )
            SettingSwitchRow(
                title = "默认展开动作与节点轨迹",
                subtitle = "关闭时对用户无感折叠，点击步骤卡片再查看执行节点",
                checked = prefs.showNodeBadgesInline,
                onCheckedChange = onShowNodeBadgesChange,
            )
        }
    }
}

@Composable
internal fun PermissionsCard() {
    val context = LocalContext.current
    var status by remember { mutableStateOf(readSystemPermissionStatus(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        status = readSystemPermissionStatus(context)
    }

    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            status = readSystemPermissionStatus(context)
        }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PermissionActionRow(
                title = "通知权限",
                subtitle = "用于展示后台 Agent Loop 进度与审批提醒",
                statusText = if (status.notificationsGranted) "已授权" else "未授权",
                buttonText = if (status.notificationsGranted) "通知设置" else "申请权限",
                onAction = {
                    if (!status.notificationsGranted &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openNotificationSettings(context)
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionActionRow(
                title = "允许后台运行",
                subtitle = "将应用加入电池优化白名单，避免长任务被系统挂起",
                statusText = if (status.backgroundUnrestricted) "已允许" else "受限制",
                buttonText = if (status.backgroundUnrestricted) "查看设置" else "申请允许",
                onAction = {
                    if (status.backgroundUnrestricted) {
                        openBatteryManagementSettings(context)
                    } else {
                        requestIgnoreBatteryOptimizations(context)
                    }
                },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionActionRow(
                title = "耗电管理（允许完全后台行为）",
                subtitle = "一键前往系统耗电管理，手动开启「允许完全后台行为 / 无限制」",
                statusText = "手动配置",
                buttonText = "前往耗电管理",
                onAction = { openBatteryManagementSettings(context) },
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionActionRow(
    title: String,
    subtitle: String,
    statusText: String,
    buttonText: String,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            EverfoldStatusPill(text = statusText)
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onAction) {
            Text(buttonText)
        }
    }
}

private fun AppLanguage.toLabel(): String =
    when (this) {
        AppLanguage.SYSTEM -> "跟随系统"
        AppLanguage.CHINESE -> "中文"
        AppLanguage.ENGLISH -> "English"
    }

private fun ChatTextSize.toLabel(): String =
    when (this) {
        ChatTextSize.SMALL -> "小"
        ChatTextSize.MEDIUM -> "标准"
        ChatTextSize.LARGE -> "大"
        ChatTextSize.EXTRA_LARGE -> "特大"
    }
