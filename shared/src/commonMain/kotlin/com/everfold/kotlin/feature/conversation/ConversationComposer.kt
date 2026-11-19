package com.everfold.kotlin.feature.conversation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.ConversationTextScale
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.collapse_composer
import everfold.shared.generated.resources.expand_composer
import everfold.shared.generated.resources.message_placeholder
import everfold.shared.generated.resources.send_message
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.stop_reply
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.squircle.squircleBorder
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val EXPAND_EDITOR_LENGTH = 240
private const val EXPAND_EDITOR_LINES = 3

@Composable
internal fun ConversationComposer(
    state: ConversationUiState,
    textSize: ConversationTextSize,
    shortcut: SendShortcut,
    sideMargins: Boolean,
    canSend: Boolean,
    expandedEditor: Boolean,
    onExpandedEditorChange: (Boolean) -> Unit,
    inputFocus: FocusRequester,
    bottomPadding: Dp,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    composerFeedback: @Composable () -> Unit,
    composerTools: @Composable RowScope.() -> Unit,
    attachmentsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var inputValue by remember(state.id) { mutableStateOf(TextFieldValue(state.draft, TextRange(state.draft.length))) }
    LaunchedEffect(state.draft) {
        if (inputValue.text != state.draft) inputValue = TextFieldValue(state.draft, TextRange(state.draft.length))
    }
    var inputFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue =
            if (inputFocused) {
                MiuixTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MiuixTheme.colorScheme.outline.copy(alpha = 0.4f)
            },
        animationSpec = tween(150),
        label = "composerBorder",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .conversationContentWidth(sideMargins)
                .then(if (expandedEditor) Modifier.fillMaxSize() else Modifier)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp + bottomPadding),
        ) {
            composerFeedback()
            ComposerCard(
                state = state,
                inputValue = inputValue,
                onValueChange = {
                    inputValue = it
                    onDraftChange(it.text)
                },
                textSize = textSize,
                shortcut = shortcut,
                canSend = canSend,
                expandedEditor = expandedEditor,
                borderColor = borderColor,
                inputFocus = inputFocus,
                onFocusChanged = { inputFocused = it },
                onToggleExpand = {
                    onExpandedEditorChange(!expandedEditor)
                    inputFocus.requestFocus()
                },
                onSend = onSend,
                onStop = onStop,
                composerTools = composerTools,
                attachmentsContent = attachmentsContent,
                modifier = if (expandedEditor) Modifier.weight(1f) else Modifier,
            )
        }
    }
}

@Composable
private fun ComposerCard(
    state: ConversationUiState,
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textSize: ConversationTextSize,
    shortcut: SendShortcut,
    canSend: Boolean,
    expandedEditor: Boolean,
    borderColor: Color,
    inputFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onToggleExpand: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    composerTools: @Composable RowScope.() -> Unit,
    attachmentsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .squircleSurface(color = MiuixTheme.colorScheme.surfaceContainer, cornerRadius = 24.dp)
                .squircleBorder(width = { 1.dp }, color = { borderColor }, cornerRadius = 24.dp),
        color = Color.Transparent,
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            attachmentsContent()
            ComposerInputField(
                inputValue = inputValue,
                onValueChange = onValueChange,
                textSize = textSize,
                expandedEditor = expandedEditor,
                shortcut = shortcut,
                canSend = canSend,
                isResponding = state.isResponding,
                hasSendableContent = state.hasSendableContent,
                inputFocus = inputFocus,
                onFocusChanged = onFocusChanged,
                onSend = onSend,
                modifier = if (expandedEditor) Modifier.weight(1f) else Modifier,
            )
            ComposerToolbar(
                state = state,
                inputText = inputValue.text,
                canSend = canSend,
                shortcut = shortcut,
                expandedEditor = expandedEditor,
                onToggleExpand = onToggleExpand,
                onSend = onSend,
                onStop = onStop,
                composerTools = composerTools,
            )
        }
    }
}

@Composable
private fun ComposerInputField(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    textSize: ConversationTextSize,
    expandedEditor: Boolean,
    shortcut: SendShortcut,
    canSend: Boolean,
    isResponding: Boolean,
    hasSendableContent: Boolean,
    inputFocus: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ConversationTextScale(textSize) {
        TextField(
            value = inputValue,
            onValueChange = onValueChange,
            modifier =
                modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocus)
                    .onFocusChanged { onFocusChanged(it.isFocused) }
                    .onPreviewKeyEvent {
                        val shouldSend =
                            it.type == KeyEventType.KeyDown &&
                                it.key == Key.Enter &&
                                shortcut.matches(
                                    ctrl = it.isCtrlPressed,
                                    meta = it.isMetaPressed,
                                    shift = it.isShiftPressed,
                                    alt = it.isAltPressed,
                                    desktop = desktopUi,
                                    composing = inputValue.composition != null,
                                )
                        if (shouldSend) {
                            if (canSend && !isResponding && hasSendableContent) onSend()
                            true
                        } else {
                            false
                        }
                    },
            insideMargin = DpSize(10.dp, 8.dp),
            label = localizedString(Res.string.message_placeholder),
            useLabelAsPlaceholder = true,
            maxLines = if (expandedEditor) Int.MAX_VALUE else 5,
            colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    borderColor = Color.Transparent,
                ),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun ComposerToolbar(
    state: ConversationUiState,
    inputText: String,
    canSend: Boolean,
    shortcut: SendShortcut,
    expandedEditor: Boolean,
    onToggleExpand: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    composerTools: @Composable RowScope.() -> Unit,
) {
    val canExpand = inputText.length > EXPAND_EDITOR_LENGTH || inputText.count { it == '\n' } >= EXPAND_EDITOR_LINES || expandedEditor
    val sendEnabled = state.isResponding || (canSend && state.hasSendableContent)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        composerTools()
        if (canExpand) {
            ComposerExpandButton(expandedEditor, onToggleExpand)
        }
        ComposerSendStopButton(
            responding = state.isResponding,
            enabled = sendEnabled,
            shortcut = shortcut,
            onClick = if (state.isResponding) onStop else onSend,
        )
    }
}

@Composable
private fun ComposerExpandButton(
    expandedEditor: Boolean,
    onToggleExpand: () -> Unit,
) {
    val editorLabel = localizedString(if (expandedEditor) Res.string.collapse_composer else Res.string.expand_composer)
    AppTooltip(editorLabel) {
        IconButton(onClick = onToggleExpand) {
            AppIcon(if (expandedEditor) AppSymbol.Collapse else AppSymbol.Expand, description = editorLabel)
        }
    }
}

@Composable
private fun ComposerSendStopButton(
    responding: Boolean,
    enabled: Boolean,
    shortcut: SendShortcut,
    onClick: () -> Unit,
) {
    val actionLabel = localizedString(if (responding) Res.string.stop_reply else Res.string.send_message)
    val tooltip =
        localizedString(
            when {
                responding -> Res.string.stop_reply
                desktopUi && shortcut == SendShortcut.Enter -> Res.string.send_shortcut_enter
                desktopUi -> Res.string.send_shortcut
                else -> Res.string.send_message
            },
        )
    val container = if (enabled) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.disabledPrimaryButton
    val contentTint = if (enabled) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.disabledOnPrimaryButton
    AppTooltip(tooltip) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            backgroundColor = container,
            cornerRadius = 18.dp,
            modifier = Modifier.padding(end = 2.dp, bottom = 2.dp),
        ) {
            AppIcon(
                symbol = if (responding) AppSymbol.Stop else AppSymbol.Send,
                modifier = Modifier.size(20.dp),
                description = actionLabel,
                tint = contentTint,
            )
        }
    }
}
