package com.everfold.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.ToggleButtonShapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private const val HORIZONTAL_PADDING_DP = 8
private const val VERTICAL_PADDING_DP = 10
private const val ICON_GAP_DP = 6

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> EverfoldConnectedButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isItemEnabled: (T) -> Boolean = { enabled },
    icon: (@Composable (T) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val shapes =
                when {
                    options.size <= 1 -> {
                        ToggleButtonShapes(
                            shape = ToggleButtonDefaults.shape,
                            pressedShape = ToggleButtonDefaults.pressedShape,
                            checkedShape = ToggleButtonDefaults.checkedShape,
                        )
                    }

                    index == 0 -> {
                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                    }

                    index == options.lastIndex -> {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    }

                    else -> {
                        ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                }
            FilledTonalToggleButton(
                checked = option == selected,
                onCheckedChange = { checked -> if (checked || option != selected) onSelect(option) },
                enabled = enabled && isItemEnabled(option),
                shapes = shapes,
                contentPadding = PaddingValues(horizontal = HORIZONTAL_PADDING_DP.dp, vertical = VERTICAL_PADDING_DP.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (icon != null) {
                    icon(option)
                    Spacer(modifier = Modifier.width(ICON_GAP_DP.dp))
                }
                Text(
                    text = label(option),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
