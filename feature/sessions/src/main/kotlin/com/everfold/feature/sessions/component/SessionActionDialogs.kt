package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.feature.sessions.R

private const val DELETE_CONTAINER_ALPHA = 0.45f

private data class SheetActionEntry(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionActionBottomSheet(
    summary: AgentSessionSummary,
    onDismiss: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = buildSessionSheetActions(summary, onTogglePin, onRename, onToggleArchive)
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetSessionHeader(summary = summary)
            EverfoldGroupedList {
                actions.forEachIndexed { index, action ->
                    EverfoldGroupedItem(index = index, count = actions.size, onClick = action.onClick) {
                        SheetActionRow(icon = action.icon, label = action.label)
                    }
                }
            }
            EverfoldGroupedList {
                EverfoldGroupedItem(
                    index = 0,
                    count = 1,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = DELETE_CONTAINER_ALPHA),
                    onClick = onDelete,
                ) {
                    SheetActionRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.session_action_delete),
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun buildSessionSheetActions(
    summary: AgentSessionSummary,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onToggleArchive: () -> Unit,
): List<SheetActionEntry> =
    buildList {
        if (!summary.isArchived) {
            val pinRes = if (summary.isPinned) R.string.session_action_unpin else R.string.session_action_pin
            add(SheetActionEntry(icon = Icons.Rounded.PushPin, label = stringResource(pinRes), onClick = onTogglePin))
        }
        add(SheetActionEntry(icon = Icons.Rounded.Edit, label = stringResource(R.string.session_action_rename), onClick = onRename))
        val archiveIcon = if (summary.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive
        val archiveRes = if (summary.isArchived) R.string.session_action_unarchive else R.string.session_action_archive
        add(SheetActionEntry(icon = archiveIcon, label = stringResource(archiveRes), onClick = onToggleArchive))
    }

@Composable
private fun SheetSessionHeader(summary: AgentSessionSummary) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = summary.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${summary.workspaceName} · ${summary.loopHostNodeName}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RenameSessionDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    val focusRequester = remember { FocusRequester() }
    val canConfirm = title.trim().isNotEmpty()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_rename_dialog_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.session_rename_dialog_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm(title.trim()) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                shapes = ButtonDefaults.shapes(),
                onClick = { onConfirm(title.trim()) },
            ) {
                Text(stringResource(R.string.session_rename_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.session_dialog_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeleteSessionDialog(
    summary: AgentSessionSummary,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning =
        summary.loopState == LoopState.PLANNING ||
            summary.loopState == LoopState.EXECUTING_ACTION
    val message =
        if (isRunning) {
            stringResource(R.string.session_delete_dialog_running_body, summary.title)
        } else {
            stringResource(R.string.session_delete_dialog_body, summary.title)
        }
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_delete_dialog_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.session_delete_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.session_dialog_cancel))
            }
        },
    )
}
