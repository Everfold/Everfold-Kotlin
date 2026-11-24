package com.everfold.feature.sessions.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.LoopState
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun SessionFullscreenComposerDialog(
    draft: String,
    attachments: ImmutableList<SessionAttachment>,
    loopState: LoopState,
    activeModel: ModelDescriptor?,
    activeEffort: ReasoningEffort?,
    onDraftChange: (String) -> Unit,
    onAddAttachments: (List<SessionAttachment>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberAttachmentLaunchers(onAddAttachments = onAddAttachments)
    val hintRes =
        if (loopState == LoopState.PAUSED) {
            R.string.session_composer_paused_hint
        } else {
            R.string.session_detail_composer_hint
        }

    BackHandler(onBack = onDismiss)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
        ) {
            FullscreenHeaderRow(charCount = draft.length, onDismiss = onDismiss)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FullscreenEditorBody(
                draft = draft,
                placeholder = stringResource(hintRes),
                onDraftChange = onDraftChange,
                modifier = Modifier.weight(1f),
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            FullscreenFooter(
                attachments = attachments,
                hasInput = draft.isNotBlank() || attachments.isNotEmpty(),
                loopState = loopState,
                modelLabel = formatModelEffortLabel(model = activeModel, effort = activeEffort),
                launchers = launchers,
                onRemoveAttachment = onRemoveAttachment,
                onOpenModelSheet = onOpenModelSheet,
                onSend = {
                    onSend()
                    onDismiss()
                },
                onPause = onPause,
                onResume = {
                    onResume()
                    onDismiss()
                },
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun FullscreenFooter(
    attachments: ImmutableList<SessionAttachment>,
    hasInput: Boolean,
    loopState: LoopState,
    modelLabel: String,
    launchers: AttachmentLaunchers,
    onRemoveAttachment: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionAttachmentStrip(attachments = attachments, onRemoveAttachment = onRemoveAttachment)
        ComposerToolbarRow(
            loopState = loopState,
            hasInput = hasInput,
            modelLabel = modelLabel,
            onPickFile = launchers.launchFiles,
            onPickImage = launchers.launchImages,
            onOpenModelSheet = onOpenModelSheet,
            onSend = onSend,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun FullscreenHeaderRow(
    charCount: Int,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.session_composer_fullscreen_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.session_composer_char_count, charCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.CloseFullscreen,
                contentDescription = stringResource(R.string.session_composer_collapse),
            )
        }
    }
}

@Composable
private fun FullscreenEditorBody(
    draft: String,
    placeholder: String,
    onDraftChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SyncedBasicTextField(
            value = draft,
            placeholder = placeholder,
            onValueChange = onDraftChange,
            lineLimits = TextFieldLineLimits.MultiLine(),
        )
    }
}

@Composable
internal fun SyncedBasicTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    lineLimits: TextFieldLineLimits,
    modifier: Modifier = Modifier,
    placeholderMaxLines: Int = Int.MAX_VALUE,
) {
    val textFieldState = rememberTextFieldState(initialText = value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(value) {
        if (textFieldState.text.toString() != value) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .distinctUntilChanged()
            .collect { latestOnValueChange(it) }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (textFieldState.text.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = placeholderMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            state = textFieldState,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            lineLimits = lineLimits,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
