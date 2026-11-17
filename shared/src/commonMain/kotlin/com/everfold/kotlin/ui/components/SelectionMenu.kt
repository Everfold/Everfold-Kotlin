package com.everfold.kotlin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.copied
import everfold.shared.generated.resources.copy_path
import everfold.shared.generated.resources.selection_empty
import everfold.shared.generated.resources.selection_search

private const val SEARCH_THRESHOLD = 8

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
    var query by remember(title, expanded) { mutableStateOf("") }
    val searchable = choices.count { it.searchable && !it.readOnly } >= SEARCH_THRESHOLD
    val filtered = choices.filter { !it.searchable || it.label.contains(query, true) || it.detail.contains(query, true) }
    val content: @Composable () -> Unit = {
        if (searchable) {
            OutlinedTextField(
                query,
                { query = it },
                Modifier.fillMaxWidth().padding(12.dp),
                placeholder = { Text(localizedString(Res.string.selection_search)) },
                singleLine = true,
            )
        }
        if (filtered.none { it.searchable } && query.isNotEmpty()) {
            Text(localizedString(Res.string.selection_empty), Modifier.padding(12.dp))
        }
        SelectionItems(filtered, onSelect)
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
                    content()
                }
            }
        }
    } else {
        DropdownMenu(expanded, onDismissRequest = onDismiss) { content() }
    }
}

@Composable
private fun SelectionItems(
    choices: List<SelectionChoice>,
    onSelect: (String) -> Unit,
) {
    choices.forEach { choice ->
        if (choice.readOnly) {
            SelectionInformation(choice)
        } else {
            DropdownMenuItem(
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(choice.label)
                        if (choice.detail.isNotEmpty()) Text(choice.detail, style = MaterialTheme.typography.bodySmall)
                    }
                },
                enabled = choice.enabled,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                trailingIcon = { if (choice.selected) AppIcon(AppSymbol.Check) },
                onClick = { onSelect(choice.id) },
            )
        }
    }
}

@Composable
private fun SelectionInformation(choice: SelectionChoice) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(choice.copyValue) { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        SelectionContainer(Modifier.weight(1f)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(choice.label)
                Text(choice.detail, style = MaterialTheme.typography.bodySmall)
            }
        }
        choice.copyValue?.let { value ->
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(value))
                copied = true
            }) {
                Text(localizedString(if (copied) Res.string.copied else Res.string.copy_path))
            }
        }
    }
}
