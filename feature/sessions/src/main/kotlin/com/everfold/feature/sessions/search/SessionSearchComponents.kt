package com.everfold.feature.sessions.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.LoopState
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.labelRes
import kotlinx.collections.immutable.ImmutableList
import com.everfold.core.ui.R as CoreUiR

private const val MAX_TITLE_LINES = 2
private const val MAX_PREVIEW_LINES = 2

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SessionSearchTopBar(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(CoreUiR.string.common_back),
                    )
                }
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.session_search_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.session_search_clear_query),
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
internal fun SessionSearchFilterRow(
    filter: SessionSearchFilter,
    availableWorkspaces: ImmutableList<String>,
    availableNodes: ImmutableList<String>,
    onDismissIme: () -> Unit,
    onSelectWorkspace: (String?) -> Unit,
    onSelectNode: (String?) -> Unit,
    onSelectLoopState: (LoopState?) -> Unit,
    onToggleArchiveFilter: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "workspace") {
            OptionFilterChip(
                defaultLabel = stringResource(R.string.session_search_filter_workspace),
                allLabel = stringResource(R.string.session_search_filter_all_workspaces),
                options = availableWorkspaces,
                selectedOption = filter.workspaceName,
                onOpenMenu = onDismissIme,
                onSelect = onSelectWorkspace,
            )
        }
        item(key = "node") {
            OptionFilterChip(
                defaultLabel = stringResource(R.string.session_search_filter_node),
                allLabel = stringResource(R.string.session_search_filter_all_nodes),
                options = availableNodes,
                selectedOption = filter.nodeName,
                onOpenMenu = onDismissIme,
                onSelect = onSelectNode,
            )
        }
        item(key = "state") {
            SessionStateFilterChip(
                selectedState = filter.loopState,
                onOpenMenu = onDismissIme,
                onSelectLoopState = onSelectLoopState,
            )
        }
        item(key = "archived") {
            FilterChip(
                selected = filter.includeArchived,
                onClick = {
                    onDismissIme()
                    onToggleArchiveFilter()
                },
                label = { Text(stringResource(R.string.session_search_filter_include_archived)) },
            )
        }
        if (filter.hasActiveFilters) {
            item(key = "clear") {
                AssistChip(
                    onClick = {
                        onDismissIme()
                        onClearFilters()
                    },
                    label = { Text(stringResource(R.string.session_search_clear_filters)) },
                )
            }
        }
    }
}

@Composable
private fun SessionStateFilterChip(
    selectedState: LoopState?,
    onOpenMenu: () -> Unit,
    onSelectLoopState: (LoopState?) -> Unit,
) {
    val stateLabels = LoopState.entries.associateWith { stringResource(it.labelRes()) }
    OptionFilterChip(
        defaultLabel = stringResource(R.string.session_search_filter_state),
        allLabel = stringResource(R.string.session_search_filter_all_states),
        options = LoopState.entries,
        selectedOption = selectedState,
        optionLabel = { stateLabels[it].orEmpty() },
        onOpenMenu = onOpenMenu,
        onSelect = onSelectLoopState,
    )
}

@Composable
private fun <T> OptionFilterChip(
    defaultLabel: String,
    allLabel: String,
    options: List<T>,
    selectedOption: T?,
    onOpenMenu: () -> Unit,
    onSelect: (T?) -> Unit,
    optionLabel: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    val chipLabel = selectedOption?.let(optionLabel) ?: defaultLabel
    Box {
        FilterChip(
            selected = selectedOption != null,
            onClick = {
                onOpenMenu()
                expanded = true
            },
            label = {
                Text(
                    text = chipLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            FilterOptionMenuItem(
                text = allLabel,
                isSelected = selectedOption == null,
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            options.forEach { option ->
                FilterOptionMenuItem(
                    text = optionLabel(option),
                    isSelected = selectedOption == option,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterOptionMenuItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text) },
        trailingIcon =
            if (isSelected) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                null
            },
        onClick = onClick,
    )
}

@Composable
internal fun SessionSearchHitCard(
    index: Int,
    count: Int,
    hit: SessionSearchHit,
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = hit.summary
    val highlightBg = MaterialTheme.colorScheme.primaryContainer
    val highlightFg = MaterialTheme.colorScheme.onPrimaryContainer
    val previewText = hit.matchedSnippet ?: summary.lastMessagePreview
    val previewColor =
        if (hit.matchedSnippet != null) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    EverfoldGroupedItem(
        index = index,
        count = count,
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = buildHighlightedText(summary.title, query, highlightBg, highlightFg),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = MAX_TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = summary.updatedAtLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (previewText.isNotBlank()) {
                Text(
                    text = buildHighlightedText(previewText, query, highlightBg, highlightFg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = previewColor,
                    maxLines = MAX_PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SearchHitMetadataRow(
                workspaceName = summary.workspaceName,
                nodeName = summary.loopHostNodeName,
                loopState = summary.loopState,
                isArchived = summary.isArchived,
            )
        }
    }
}

@Composable
private fun SearchHitMetadataRow(
    workspaceName: String,
    nodeName: String,
    loopState: LoopState,
    isArchived: Boolean,
) {
    val (pillBg, pillFg) = loopStateColors(loopState)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetadataTag(
            icon = Icons.Rounded.FolderOpen,
            label = workspaceName,
            modifier = Modifier.weight(1f, fill = false),
        )
        MetadataTag(
            icon = Icons.Rounded.Hub,
            label = nodeName,
            modifier = Modifier.weight(1f),
        )
        if (isArchived) {
            EverfoldStatusPill(
                text = stringResource(R.string.session_archived_badge),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        EverfoldStatusPill(
            text = stringResource(loopState.labelRes()),
            containerColor = pillBg,
            contentColor = pillFg,
        )
    }
}

@Composable
private fun MetadataTag(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun loopStateColors(state: LoopState): Pair<Color, Color> =
    when (state) {
        LoopState.FAILED -> {
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        }

        LoopState.PLANNING, LoopState.EXECUTING_ACTION -> {
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }

        LoopState.PAUSED -> {
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        }

        LoopState.IDLE, LoopState.AWAITING_INPUT, LoopState.COMPLETED -> {
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        }
    }

private fun buildHighlightedText(
    text: String,
    query: String,
    highlightBg: Color,
    highlightFg: Color,
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val matchIndex = text.indexOf(trimmed, startIndex = cursor, ignoreCase = true)
            if (matchIndex < 0) {
                append(text.substring(cursor))
                break
            }
            if (matchIndex > cursor) {
                append(text.substring(cursor, matchIndex))
            }
            val matchEnd = matchIndex + trimmed.length
            withStyle(
                SpanStyle(
                    background = highlightBg,
                    color = highlightFg,
                    fontWeight = FontWeight.SemiBold,
                ),
            ) {
                append(text.substring(matchIndex, matchEnd))
            }
            cursor = matchEnd
        }
    }
}
