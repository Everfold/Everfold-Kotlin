package com.everfold.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.StickyNote2
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.designsystem.component.EverfoldGroupedList
import com.everfold.core.model.ConnectionProfile
import com.everfold.core.ui.connection.summaryLine

private const val DELETE_CONTAINER_ALPHA = 0.45f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodeProfileActionSheet(
    profile: ConnectionProfile,
    onDismiss: () -> Unit,
    onEditConfig: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NodeSheetHeader(profile = profile)
            EverfoldGroupedList {
                EverfoldGroupedItem(index = 0, count = 2, onClick = onEditConfig) {
                    NodeSheetRow(
                        icon = Icons.Rounded.Edit,
                        label = stringResource(R.string.nodes_action_edit),
                    )
                }
                EverfoldGroupedItem(index = 1, count = 2, onClick = onEditNote) {
                    NodeSheetRow(
                        icon = Icons.Rounded.StickyNote2,
                        label = stringResource(R.string.nodes_action_edit_note),
                    )
                }
            }
            EverfoldGroupedList {
                EverfoldGroupedItem(
                    index = 0,
                    count = 1,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = DELETE_CONTAINER_ALPHA),
                    onClick = onDelete,
                ) {
                    NodeSheetRow(
                        icon = Icons.Rounded.DeleteOutline,
                        label = stringResource(R.string.nodes_action_delete),
                        contentColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeSheetHeader(profile: ConnectionProfile) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = profile.transport.summaryLine(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NodeSheetRow(
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
internal fun EditNodeNoteDialog(
    profile: ConnectionProfile,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember(profile.id, profile.note) { mutableStateOf(profile.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nodes_note_dialog_title, profile.name)) },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.nodes_note_dialog_hint)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note) }, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.nodes_dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.nodes_dialog_cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DeleteNodeProfileDialog(
    profile: ConnectionProfile,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val body =
        stringResource(
            if (isActive) R.string.nodes_delete_active_body else R.string.nodes_delete_body,
            profile.name,
        )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nodes_delete_title)) },
        text = { Text(body) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.nodes_action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
                Text(stringResource(R.string.nodes_dialog_cancel))
            }
        },
    )
}
