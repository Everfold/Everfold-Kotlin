package com.everfold.feature.sessions.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.LoopState
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.core.model.SessionAttachment
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableList

private const val COMPOSER_MAX_LINES = 4
private const val COMPOSER_CARD_RADIUS_DP = 24

@Composable
internal fun SessionComposerBar(
    draft: String,
    attachments: ImmutableList<SessionAttachment>,
    loopState: LoopState,
    activeModel: ModelDescriptor?,
    activeEffort: ReasoningEffort?,
    onDraftChange: (String) -> Unit,
    onAddAttachments: (List<SessionAttachment>) -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val launchers = rememberAttachmentLaunchers(onAddAttachments = onAddAttachments)
    val modelLabel = formatModelEffortLabel(model = activeModel, effort = activeEffort)

    Surface(modifier = modifier.fillMaxWidth(), tonalElevation = 1.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SessionAttachmentStrip(attachments = attachments, onRemoveAttachment = onRemoveAttachment)
            ComposerCardSurface(
                draft = draft,
                hasInput = draft.isNotBlank() || attachments.isNotEmpty(),
                loopState = loopState,
                modelLabel = modelLabel,
                launchers = launchers,
                onDraftChange = onDraftChange,
                onOpenModelSheet = onOpenModelSheet,
                onOpenFullscreen = onOpenFullscreen,
                onSend = onSend,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun ComposerCardSurface(
    draft: String,
    hasInput: Boolean,
    loopState: LoopState,
    modelLabel: String,
    launchers: AttachmentLaunchers,
    onDraftChange: (String) -> Unit,
    onOpenModelSheet: () -> Unit,
    onOpenFullscreen: () -> Unit,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val hintRes =
        if (loopState == LoopState.PAUSED) {
            R.string.session_composer_paused_hint
        } else {
            R.string.session_detail_composer_hint
        }
    Surface(
        shape = RoundedCornerShape(COMPOSER_CARD_RADIUS_DP.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ComposerTextInputRow(
                draft = draft,
                placeholder = stringResource(hintRes),
                onDraftChange = onDraftChange,
                onOpenFullscreen = onOpenFullscreen,
            )
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
}

@Composable
private fun ComposerTextInputRow(
    draft: String,
    placeholder: String,
    onDraftChange: (String) -> Unit,
    onOpenFullscreen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SyncedBasicTextField(
            value = draft,
            placeholder = placeholder,
            onValueChange = onDraftChange,
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = COMPOSER_MAX_LINES),
            placeholderMaxLines = 2,
            modifier = Modifier.weight(1f).padding(top = 6.dp, bottom = 2.dp),
        )
        IconButton(onClick = onOpenFullscreen, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.OpenInFull,
                contentDescription = stringResource(R.string.session_composer_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun ComposerToolbarRow(
    loopState: LoopState,
    hasInput: Boolean,
    modelLabel: String,
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
    onOpenModelSheet: () -> Unit,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ComposerAttachMenuButton(onPickFile = onPickFile, onPickImage = onPickImage)
        ComposerModelEffortPill(
            label = modelLabel,
            onClick = onOpenModelSheet,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(modifier = Modifier.weight(1f))
        ComposerActionButtons(
            loopState = loopState,
            hasInput = hasInput,
            onSend = onSend,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
        )
    }
}

@Composable
private fun ComposerAttachMenuButton(
    onPickFile: () -> Unit,
    onPickImage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.session_composer_attach),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.session_composer_pick_file)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Description, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPickFile()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.session_composer_pick_image)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Image, contentDescription = null) },
                onClick = {
                    expanded = false
                    onPickImage()
                },
            )
        }
    }
}

@Composable
private fun ComposerModelEffortPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.session_composer_select_model),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ComposerActionButtons(
    loopState: LoopState,
    hasInput: Boolean,
    onSend: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    val isRunning = loopState == LoopState.PLANNING || loopState == LoopState.EXECUTING_ACTION
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        when {
            isRunning -> {
                RunningLoopButtons(onPause = onPause, onCancel = onCancel)
            }

            loopState == LoopState.PAUSED -> {
                PausedLoopButtons(hasInput = hasInput, onSend = onSend, onResume = onResume, onCancel = onCancel)
            }

            else -> {
                FilledIconButton(onClick = onSend, enabled = hasInput, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.session_detail_send),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.RunningLoopButtons(
    onPause: () -> Unit,
    onCancel: () -> Unit,
) {
    FilledTonalIconButton(onClick = onPause, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Rounded.Pause,
            contentDescription = stringResource(R.string.session_composer_pause),
            modifier = Modifier.size(18.dp),
        )
    }
    FilledIconButton(
        onClick = onCancel,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Stop,
            contentDescription = stringResource(R.string.session_detail_stop),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RowScope.PausedLoopButtons(
    hasInput: Boolean,
    onSend: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    if (hasInput) {
        FilledIconButton(onClick = onSend, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Send,
                contentDescription = stringResource(R.string.session_composer_send_resume),
                modifier = Modifier.size(18.dp),
            )
        }
    } else {
        FilledIconButton(onClick = onResume, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.session_composer_resume),
                modifier = Modifier.size(20.dp),
            )
        }
    }
    FilledTonalIconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Rounded.Stop,
            contentDescription = stringResource(R.string.session_detail_stop),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun formatModelEffortLabel(
    model: ModelDescriptor?,
    effort: ReasoningEffort?,
): String {
    val name = model?.displayName?.ifBlank { null } ?: stringResource(R.string.session_composer_select_model)
    val effortText = if (model?.supportsEffort == true && effort != null) stringResource(effort.labelRes()) else null
    return if (effortText != null) "$name · $effortText" else name
}
