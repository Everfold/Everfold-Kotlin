package com.everfold.feature.settings.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val TILE_WIDTH_DP = 76
private const val TILE_CORNER_DP = 18
private const val UNSELECTED_BORDER_ALPHA = 0.45f

@Composable
internal fun MonetOptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable () -> Unit,
) {
    val border =
        if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = UNSELECTED_BORDER_ALPHA),
            )
        }
    Surface(
        onClick = onClick,
        modifier = modifier.width(TILE_WIDTH_DP.dp),
        shape = RoundedCornerShape(TILE_CORNER_DP.dp),
        color = containerColor,
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
