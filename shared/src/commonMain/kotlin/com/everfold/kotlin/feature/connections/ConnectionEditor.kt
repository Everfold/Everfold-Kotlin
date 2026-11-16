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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

@Composable
internal fun ConnectionEditor(
    profile: ConnectionProfile?,
    onSave: (ConnectionDraft, Boolean) -> Boolean,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val initialName = profile?.name?.resolve().orEmpty()
    var draft by rememberSaveable(stateSaver = ConnectionDraft.Saver) {
        mutableStateOf(ConnectionDraft(initialName, if (profile == null) ClusterSetup() else profile.setup, profile?.autoConnect ?: false))
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val update: (ConnectionDraft) -> Unit = {
        draft = it
        saved = false
    }
    val save: (Boolean) -> Unit = { connect ->
        showErrors = !draft.isValid
        if (draft.isValid) saved = onSave(draft, connect)
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (profile == null) Text(localizedString(Res.string.connection_add), style = MaterialTheme.typography.headlineSmall)
            ConnectionFields(draft, update, showErrors)
            if (profile?.canEditEndpoint == true) {
                Text(
                    localizedString(Res.string.connection_edit_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (saved) Text(localizedString(Res.string.connection_saved), color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { save(false) }) { Text(localizedString(Res.string.connection_save)) }
                if (profile == null) {
                    OutlinedButton(onClick = { save(true) }) { Text(localizedString(Res.string.connection_save_connect)) }
                } else {
                    TextButton(onClick = {
                        update(ConnectionDraft(initialName, profile.setup, profile.autoConnect))
                        showErrors = false
                    }) { Text(localizedString(Res.string.connection_reset)) }
                }
                if (onCancel != null) TextButton(onClick = onCancel) { Text(localizedString(Res.string.cancel)) }
            }
        }
    }
}

@Composable
private fun ConnectionFields(
    draft: ConnectionDraft,
    onChange: (ConnectionDraft) -> Unit,
    showErrors: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onChange(draft.copy(name = it)) },
            label = { Text(localizedString(Res.string.connection_name)) },
            singleLine = true,
            isError = showErrors && draft.name.isBlank(),
            supportingText = { if (showErrors && draft.name.isBlank()) Text(localizedString(Res.string.connection_name_required)) },
            modifier = Modifier.fillMaxWidth(),
        )
        if (draft.setup != null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                ClusterConnectionForm(
                    draft.setup,
                    wide = maxWidth >= 600.dp,
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
