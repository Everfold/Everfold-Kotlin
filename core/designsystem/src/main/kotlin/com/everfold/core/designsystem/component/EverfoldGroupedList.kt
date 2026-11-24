package com.everfold.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val GROUP_OUTER_RADIUS_DP = 20
private const val GROUP_INNER_RADIUS_DP = 4

fun groupedItemShape(
    index: Int,
    count: Int,
): RoundedCornerShape {
    val topRadius = if (index == 0) GROUP_OUTER_RADIUS_DP.dp else GROUP_INNER_RADIUS_DP.dp
    val bottomRadius = if (index == count - 1) GROUP_OUTER_RADIUS_DP.dp else GROUP_INNER_RADIUS_DP.dp
    return RoundedCornerShape(
        topStart = topRadius,
        topEnd = topRadius,
        bottomStart = bottomRadius,
        bottomEnd = bottomRadius,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EverfoldGroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EverfoldGroupedItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    selected: Boolean? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors =
        ListItemDefaults.segmentedColors(
            containerColor = containerColor,
            selectedContainerColor = containerColor,
        )
    val zeroPadding = PaddingValues(0.dp)
    when {
        onClick != null && selected != null -> {
            SegmentedListItem(
                selected = selected,
                onClick = onClick,
                shapes = shapes,
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                contentPadding = zeroPadding,
                content = content,
            )
        }

        onClick != null -> {
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                contentPadding = zeroPadding,
                content = content,
            )
        }

        else -> {
            SegmentedListItem(
                shapes = shapes,
                modifier = modifier.fillMaxWidth(),
                colors = colors,
                contentPadding = zeroPadding,
                content = content,
            )
        }
    }
}
