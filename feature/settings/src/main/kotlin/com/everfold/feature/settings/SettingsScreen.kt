package com.everfold.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.designsystem.theme.toColor
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.SeedColorPreset
import com.everfold.core.model.ThemeMode
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("设置") }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { EverfoldSectionHeader(title = "外观与 Material 3 主题") }
            item {
                AppearanceCard(
                    prefs = prefs,
                    onThemeModeChange = viewModel::setThemeMode,
                    onDynamicColorChange = viewModel::setDynamicColor,
                    onSeedColorChange = viewModel::setSeedColor,
                )
            }
            item { EverfoldSectionHeader(title = "可观测性展示") }
            item {
                ObservabilityCard(
                    showInline = prefs.showNodeBadgesInline,
                    onToggleInline = viewModel::setShowNodeBadgesInline,
                )
            }
            item { EverfoldSectionHeader(title = "连接会话") }
            item {
                OutlinedButton(
                    onClick = viewModel::disconnectAuthority,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("断开当前连接并返回引导页")
                }
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    prefs: AppPreferences,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onSeedColorChange: (SeedColorPreset) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text(mode.toLabel()) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("系统动态取色 (Material You)", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "关闭后使用下方自定义主题种子色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = prefs.useDynamicColor, onCheckedChange = onDynamicColorChange)
            }
            if (!prefs.useDynamicColor) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeedColorPreset.entries.forEach { preset ->
                        val selected = prefs.seedColor == preset
                        Box(
                            modifier =
                                Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(preset.toColor())
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                3.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape,
                                            )
                                        } else {
                                            Modifier
                                        },
                                    ).clickable { onSeedColorChange(preset) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservabilityCard(
    showInline: Boolean,
    onToggleInline: (Boolean) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("默认展开动作与节点轨迹", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "关闭时对用户无感折叠，点击步骤卡片再查看执行节点",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = showInline, onCheckedChange = onToggleInline)
        }
    }
}

private fun ThemeMode.toLabel(): String =
    when (this) {
        ThemeMode.SYSTEM -> "跟随系统"
        ThemeMode.LIGHT -> "浅色"
        ThemeMode.DARK -> "深色"
    }
