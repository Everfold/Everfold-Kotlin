package com.everfold.feature.sessions.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.core.model.LoopState
import com.everfold.core.model.SessionSearchFilter
import com.everfold.core.model.SessionSearchHit
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.labelRes
import kotlinx.collections.immutable.ImmutableList

private const val MAX_TITLE_LINES = 2
private const val MAX_PREVIEW_LINES = 2
private const val CHEVRON_EXPANDED_DEGREES = 180f

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 6.dp),
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
            ArchivedToggleChip(
                selected = filter.includeArchived,
                onClick = { onDismissIme().also { onToggleArchiveFilter() } },
            )
        }
        if (filter.hasActiveFilters) {
            item(key = "clear") {
                AssistChip(
                    onClick = { onDismissIme().also { onClearFilters() } },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    label = { Text(stringResource(R.string.session_search_clear_filters)) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ArchivedToggleChip(
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                    )
                }
            } else {
                null
            },
        label = { Text(stringResource(R.string.session_search_filter_include_archived)) },
    )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val totalCount = options.size + 1
    val chevronRotation by
        animateFloatAsState(
            targetValue = if (expanded) CHEVRON_EXPANDED_DEGREES else 0f,
            animationSpec = LocalAnimationSpeed.current.spatialSpec(),
            label = "filter_chevron",
        )
    Box {
        FilterChip(
            selected = selectedOption != null,
            onClick = {
                onOpenMenu()
                expanded = true
            },
            label = { Text(text = chipLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).rotate(chevronRotation),
                )
            },
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(index = 0, count = 1)) {
                FilterOptionMenuItem(
                    text = allLabel,
                    isSelected = selectedOption == null,
                    index = 0,
                    count = totalCount,
                    onClick = { expanded = false.also { onSelect(null) } },
                )
                options.forEachIndexed { idx, option ->
                    FilterOptionMenuItem(
                        text = optionLabel(option),
                        isSelected = selectedOption == option,
                        index = idx + 1,
                        count = totalCount,
                        onClick = { expanded = false.also { onSelect(option) } },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FilterOptionMenuItem(
    text: String,
    isSelected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    SelectableDropdownMenuItem(
        selected = isSelected,
        onClick = onClick,
        text = { Text(text) },
        shapes = MenuDefaults.itemShape(index = index, count = count),
        selectedLeadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    style = MaterialTheme.typography.titleMediumEmphasized,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHitMetadataRow(
    workspaceName: String,
    nodeName: String,
    loopState: LoopState,
    isArchived: Boolean,
) {
    val (pillBg, pillFg) = loopStateColors(loopState)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        EverfoldStatusPill(
            text = stringResource(loopState.labelRes()),
            containerColor = pillBg,
            contentColor = pillFg,
        )
        if (isArchived) {
            EverfoldStatusPill(
                text = stringResource(R.string.session_archived_badge),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        MetadataTag(icon = Icons.Rounded.FolderOpen, label = workspaceName)
        MetadataTag(icon = Icons.Rounded.Hub, label = nodeName)
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
