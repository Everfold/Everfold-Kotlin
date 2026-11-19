package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
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
import everfold.shared.generated.resources.connection_unsaved
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            ConnectionEditorForm(profile, draft, initialName, showErrors, update, Modifier.weight(1f))
            ConnectionEditorActions(
                adding = profile == null,
                changed = changed,
                saveRevision = saveRevision,
                onSave = save,
                onReset = {
                    update(ConnectionDraft(null, profile?.setup, profile?.autoConnect ?: false))
                    showErrors = false
                },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun ConnectionEditorForm(
    profile: ConnectionProfile?,
    draft: ConnectionDraft,
    initialName: String,
    showErrors: Boolean,
    onChange: (ConnectionDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (profile == null) {
            Text(
                text = localizedString(Res.string.connection_add),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold,
            )
        }
        ConnectionFields(draft, draft.name ?: initialName, onChange, showErrors)
        if (profile?.canEditEndpoint == true && draft.setup != profile.setup) {
            Text(
                text = localizedString(Res.string.connection_edit_hint),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!adding && changed) {
            Text(
                text = localizedString(Res.string.connection_unsaved),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        ConnectionSaveNotice(saveRevision)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSave(false) },
                enabled = changed,
                colors = ButtonDefaults.buttonColorsPrimary(),
            ) {
                Text(localizedString(Res.string.connection_save), style = MiuixTheme.textStyles.button)
            }
            if (adding) {
                Button(onClick = { onSave(true) }) {
                    Text(localizedString(Res.string.connection_save_connect), style = MiuixTheme.textStyles.button)
                }
            } else {
                TextButton(
                    text = localizedString(Res.string.connection_reset),
                    onClick = onReset,
                    enabled = changed,
                    colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
            }
            if (onCancel != null) {
                TextButton(
                    text = localizedString(Res.string.cancel),
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                    insideMargin = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                )
            }
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
    if (visible) {
        Text(
            text = localizedString(Res.string.connection_saved),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ConnectionFields(
    draft: ConnectionDraft,
    name: String,
    onChange: (ConnectionDraft) -> Unit,
    showErrors: Boolean,
) {
    val nameError = showErrors && name.isBlank()
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TextField(
                value = name,
                onValueChange = { onChange(draft.copy(name = it)) },
                modifier = Modifier.fillMaxWidth(),
                label = localizedString(Res.string.connection_name),
                useLabelAsPlaceholder = false,
                singleLine = true,
                colors =
                    TextFieldDefaults.textFieldColors(
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        borderColor = if (nameError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                    ),
                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            )
            if (nameError) {
                Text(
                    text = localizedString(Res.string.connection_name_required),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.error,
                )
            }
        }
        if (draft.setup != null) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                ClusterConnectionForm(
                    form = draft.setup,
                    wide = maxWidth >= 600.dp,
                    onChange = { onChange(draft.copy(setup = it)) },
                    showErrors = showErrors,
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            SwitchPreference(
                title = localizedString(Res.string.connection_auto_connect),
                checked = draft.autoConnect,
                onCheckedChange = { onChange(draft.copy(autoConnect = it)) },
                modifier = Modifier.semantics { toggleableState = ToggleableState(draft.autoConnect) },
            )
        }
    }
}

private fun ConnectionDraft.hasChanges(
    profile: ConnectionProfile?,
    initialName: String,
): Boolean = profile == null || setup != profile.setup || autoConnect != profile.autoConnect || (name != null && name.trim() != initialName)
