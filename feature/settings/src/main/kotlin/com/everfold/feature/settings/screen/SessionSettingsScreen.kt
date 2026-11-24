package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.AppPreferences
import com.everfold.core.model.ChatTextSize
import com.everfold.feature.settings.R
import com.everfold.feature.settings.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

private const val BASE_PREVIEW_SP = 14f

@Composable
fun SessionSettingsScreen(
    onBack: () -> Unit,
    onOpenArchivedSessions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val archivedCount by viewModel.archivedSessionCount.collectAsStateWithLifecycle()

    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_entry_sessions_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { EverfoldSectionHeader(stringResource(R.string.session_pref_section_reading)) }
            item {
                SessionReadingGroup(
                    prefs = prefs,
                    onChatTextSizeChange = viewModel::setChatTextSize,
                    onAutoCollapseChange = viewModel::setAutoCollapseLongMessages,
                )
            }
            item { EverfoldSectionHeader(stringResource(R.string.session_pref_section_traces)) }
            item {
                ActionTracesGroup(
                    expandWhenRunning = prefs.expandRunningActionTraces,
                    expandWhenCompleted = prefs.expandCompletedActionTraces,
                    onExpandRunningChange = viewModel::setExpandRunningActionTraces,
                    onExpandCompletedChange = viewModel::setExpandCompletedActionTraces,
                )
            }
            item { EverfoldSectionHeader(stringResource(R.string.session_pref_section_management)) }
            item {
                ArchivedSessionsEntryGroup(
                    archivedCount = archivedCount,
                    onClick = onOpenArchivedSessions,
                )
            }
        }
    }
}

@Composable
private fun ArchivedSessionsEntryGroup(
    archivedCount: Int,
    onClick: () -> Unit,
) {
    val subtitle =
        if (archivedCount == 0) {
            stringResource(R.string.session_pref_archived_empty)
        } else {
            pluralStringResource(R.plurals.session_pref_archived_count, archivedCount, archivedCount)
        }
    EverfoldGroupedList {
        EverfoldGroupedItem(index = 0, count = 1, onClick = onClick) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.session_pref_archived_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SessionReadingGroup(
    prefs: AppPreferences,
    onChatTextSizeChange: (ChatTextSize) -> Unit,
    onAutoCollapseChange: (Boolean) -> Unit,
) {
    EverfoldGroupedList {
        EverfoldGroupedItem(index = 0, count = 2) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.session_pref_text_size_label),
                    style = MaterialTheme.typography.titleSmall,
                )
                EverfoldConnectedButtonGroup(
                    options = ChatTextSize.entries,
                    selected = prefs.chatTextSize,
                    onSelect = onChatTextSizeChange,
                    label = { size -> stringResource(size.labelRes()) },
                )
                Text(
                    text = stringResource(R.string.session_pref_text_preview),
                    fontSize = (BASE_PREVIEW_SP * prefs.chatTextSize.scale).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        EverfoldGroupedItem(index = 1, count = 2) {
            SettingSwitchRow(
                title = stringResource(R.string.session_pref_auto_collapse_title),
                subtitle = stringResource(R.string.session_pref_auto_collapse_subtitle),
                checked = prefs.autoCollapseLongMessages,
                onCheckedChange = onAutoCollapseChange,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun ActionTracesGroup(
    expandWhenRunning: Boolean,
    expandWhenCompleted: Boolean,
    onExpandRunningChange: (Boolean) -> Unit,
    onExpandCompletedChange: (Boolean) -> Unit,
) {
    EverfoldGroupedList {
        EverfoldGroupedItem(index = 0, count = 2) {
            SettingSwitchRow(
                title = stringResource(R.string.session_pref_expand_running_traces_title),
                subtitle = stringResource(R.string.session_pref_expand_running_traces_subtitle),
                checked = expandWhenRunning,
                onCheckedChange = onExpandRunningChange,
                modifier = Modifier.padding(16.dp),
            )
        }
        EverfoldGroupedItem(index = 1, count = 2) {
            SettingSwitchRow(
                title = stringResource(R.string.session_pref_expand_completed_traces_title),
                subtitle = stringResource(R.string.session_pref_expand_completed_traces_subtitle),
                checked = expandWhenCompleted,
                onCheckedChange = onExpandCompletedChange,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@StringRes
internal fun ChatTextSize.labelRes(): Int =
    when (this) {
        ChatTextSize.SMALL -> R.string.text_size_small
        ChatTextSize.MEDIUM -> R.string.text_size_medium
        ChatTextSize.LARGE -> R.string.text_size_large
        ChatTextSize.EXTRA_LARGE -> R.string.text_size_extra_large
    }
