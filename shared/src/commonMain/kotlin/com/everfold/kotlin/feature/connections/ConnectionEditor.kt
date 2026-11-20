package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.WindowBreakpoints
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.connection_add
import everfold.shared.generated.resources.connection_auto_connect
import everfold.shared.generated.resources.connection_edit_hint
import everfold.shared.generated.resources.connection_name
import everfold.shared.generated.resources.connection_name_required
import everfold.shared.generated.resources.connection_reset
import everfold.shared.generated.resources.connection_save
import everfold.shared.generated.resources.connection_save_connect
import everfold.shared.generated.resources.connection_saved
import everfold.shared.generated.resources.connection_unsaved
import kotlinx.coroutines.delay

private const val SAVE_NOTICE_DURATION_MILLIS = 2_000L

@Composable
internal fun ConnectionEditor(
    profile: ConnectionProfile?,
    onSave: (ConnectionDraft, Boolean) -> Boolean,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val initialName = profile?.name?.resolve().orEmpty()
    var draft by rememberSaveable(stateSaver = ConnectionDraft.Saver) {
        mutableStateOf(
            ConnectionDraft(
                name = if (profile == null) "" else null,
                setup = if (profile == null) ClusterSetup() else profile.setup,
                autoConnect = profile?.autoConnect ?: false,
            ),
        )
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var saveRevision by remember { mutableIntStateOf(0) }
    val changed = draft.hasChanges(profile, initialName)
    val update: (ConnectionDraft) -> Unit = {
        draft = it
        saveRevision = 0
    }
    val save: (Boolean) -> Unit = { connect ->
        showErrors = !draft.isValid
        if (draft.isValid) saveRevision = if (onSave(draft, connect)) saveRevision + 1 else 0
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 760.dp).fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (profile == null) Text(localizedString(Res.string.connection_add), style = MaterialTheme.typography.headlineSmall)
                ConnectionFields(draft, draft.name ?: initialName, update, showErrors)
                if (profile?.canEditEndpoint == true && draft.setup != profile.setup) {
                    Text(
                        localizedString(Res.string.connection_edit_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ConnectionEditorActions(profile == null, changed, saveRevision, save, onReset = {
                update(ConnectionDraft(null, profile?.setup, profile?.autoConnect ?: false))
                showErrors = false
            }, onCancel = onCancel)
        }
    }
}

@Composable
private fun ConnectionEditorActions(
    adding: Boolean,
    changed: Boolean,
    saveRevision: Int,
    onSave: (Boolean) -> Unit,
    onReset: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    HorizontalDivider()
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!adding && changed) Text(localizedString(Res.string.connection_unsaved), style = MaterialTheme.typography.labelMedium)
        ConnectionSaveNotice(saveRevision)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onSave(false) }, enabled = changed) { Text(localizedString(Res.string.connection_save)) }
            if (adding) {
                OutlinedButton(onClick = { onSave(true) }) { Text(localizedString(Res.string.connection_save_connect)) }
            } else {
                TextButton(onClick = onReset, enabled = changed) { Text(localizedString(Res.string.connection_reset)) }
            }
            if (onCancel != null) TextButton(onClick = onCancel) { Text(localizedString(Res.string.cancel)) }
        }
    }
}

@Composable
private fun ConnectionSaveNotice(revision: Int) {
    if (revision == 0) return
    var visible by remember(revision) { mutableStateOf(true) }
    LaunchedEffect(revision) {
        delay(SAVE_NOTICE_DURATION_MILLIS)
        visible = false
    }
    if (visible) Text(localizedString(Res.string.connection_saved), color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ConnectionFields(
    draft: ConnectionDraft,
    name: String,
    onChange: (ConnectionDraft) -> Unit,
    showErrors: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text(localizedString(Res.string.connection_name)) },
            singleLine = true,
            isError = showErrors && name.isBlank(),
            supportingText = { if (showErrors && name.isBlank()) Text(localizedString(Res.string.connection_name_required)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.setup != null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                ClusterConnectionForm(
                    draft.setup,
                    wide = maxWidth >= WindowBreakpoints.Compact,
                    onChange = { onChange(draft.copy(setup = it)) },
                    showErrors = showErrors,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(localizedString(Res.string.connection_auto_connect), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(checked = draft.autoConnect, onCheckedChange = { onChange(draft.copy(autoConnect = it)) })
        }
    }
}

private fun ConnectionDraft.hasChanges(
    profile: ConnectionProfile?,
    initialName: String,
): Boolean = profile == null || setup != profile.setup || autoConnect != profile.autoConnect || (name != null && name.trim() != initialName)
