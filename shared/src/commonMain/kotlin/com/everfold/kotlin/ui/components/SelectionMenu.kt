package com.everfold.kotlin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** 窄窗口使用底部面板，宽窗口在触发按钮旁展开；选项内容完全共用。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectionMenu(
    title: String,
    choices: List<SelectionChoice>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val compact =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width
                .toDp() < 600.dp
        }
    if (compact) {
        if (expanded) {
            ModalBottomSheet(onDismissRequest = onDismiss) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = 420.dp,
                        ).verticalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 24.dp),
                ) {
                    Text(title, Modifier.padding(horizontal = 12.dp, vertical = 12.dp), style = MaterialTheme.typography.titleMedium)
                    SelectionItems(choices, onSelect)
                }
            }
        }
    } else {
        DropdownMenu(expanded, onDismissRequest = onDismiss) { SelectionItems(choices, onSelect) }
    }
}

@Composable
private fun SelectionItems(
    choices: List<SelectionChoice>,
    onSelect: (String) -> Unit,
) {
    choices.forEach { choice ->
        DropdownMenuItem(
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(choice.label)
                    if (choice.detail.isNotEmpty()) Text(choice.detail, style = MaterialTheme.typography.bodySmall)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            trailingIcon = { if (choice.selected) AppIcon(AppSymbol.Check) },
            onClick = { onSelect(choice.id) },
        )
    }
}
