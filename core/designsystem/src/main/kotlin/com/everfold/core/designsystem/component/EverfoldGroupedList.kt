package com.everfold.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val GROUP_OUTER_RADIUS_DP = 20
private const val GROUP_INNER_RADIUS_DP = 4
private const val GROUP_ITEM_GAP_DP = 2

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

@Composable
fun EverfoldGroupedList(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(GROUP_ITEM_GAP_DP.dp),
        content = content,
    )
}

@Composable
fun EverfoldGroupedItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = groupedItemShape(index = index, count = count)
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            content = content,
        )
    }
}
