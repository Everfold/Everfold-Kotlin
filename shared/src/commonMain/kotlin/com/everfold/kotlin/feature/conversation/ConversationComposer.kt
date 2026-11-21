package com.everfold.kotlin.feature.conversation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.settings.ConversationTextScale
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.ProcessVisibility
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.primaryShortcutModifier
import com.everfold.kotlin.ui.localization.localizedString
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.collapse_composer
import everfold.shared.generated.resources.composer_hide_input
import everfold.shared.generated.resources.expand_composer
import everfold.shared.generated.resources.message_placeholder
import everfold.shared.generated.resources.send_message
import everfold.shared.generated.resources.send_shortcut
import everfold.shared.generated.resources.send_shortcut_enter
import everfold.shared.generated.resources.stop_reply

private const val EXPAND_EDITOR_LENGTH = 240
private const val EXPAND_EDITOR_LINES = 3
private const val INPUT_BORDER_ANIM_MILLIS = 150
private const val COMPACT_COMPOSER_MAX_LINES = 5
private const val FOCUS_BORDER_ALPHA = 0.5f

@Immutable
internal data class ComposerPrefs(
    val textSize: ConversationTextSize,
    val shortcut: SendShortcut,
    val sideMargins: Boolean,
    val canSend: Boolean,
    val collapsePrompts: Boolean = true,
    val processVisibility: ProcessVisibility = ProcessVisibility(),
)

internal class ComposerActions(
    val onDraftChange: (String) -> Unit,
    val onSend: () -> Unit,
    val onStop: () -> Unit,
    val onToggleExpanded: () -> Unit,
    val onCollapseInput: (() -> Unit)? = null,
)

@Composable
internal fun ConversationComposer(
    state: ConversationUiState,
    expandedEditor: Boolean,
    inputFocus: FocusRequester,
    prefs: ComposerPrefs,
    actions: ComposerActions,
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
    val targetBorder =
        if (inputFocused) MaterialTheme.colorScheme.primary.copy(alpha = FOCUS_BORDER_ALPHA) else MaterialTheme.colorScheme.outlineVariant
    val inputBorderColor by animateColorAsState(targetBorder, tween(INPUT_BORDER_ANIM_MILLIS), label = "inputBorderColor")
    Box(
        modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .conversationContentWidth(prefs.sideMargins)
                .then(if (expandedEditor) Modifier.fillMaxSize() else Modifier)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            composerFeedback()
            val dropTargetModifier =
                rememberPlatformTextDropTargetModifier { dropped ->
                    actions.onDraftChange(appendDroppedDraftText(state.draft, dropped))
                }
            Surface(
                modifier = (if (expandedEditor) Modifier.weight(1f) else Modifier).then(dropTargetModifier),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, inputBorderColor),
            ) {
                Column(Modifier.fillMaxWidth().padding(8.dp)) {
                    attachmentsContent()
                    ComposerInputField(
                        inputValue = inputValue,
                        onValueChange = {
                            inputValue = it
                            actions.onDraftChange(it.text)
                        },
                        expandedEditor = expandedEditor,
                        textSize = prefs.textSize,
                        inputFocus = inputFocus,
                        onFocusChange = { inputFocused = it },
                        onShortcutSend = { event ->
                            handleSendShortcutKey(event, prefs.shortcut, inputValue.composition != null) {
                                if (prefs.canSend && !state.isResponding && state.hasSendableContent) actions.onSend()
                            }
                        },
                        modifier = if (expandedEditor) Modifier.weight(1f) else Modifier,
                    )
                    ComposerActionRow(state, inputValue.text, expandedEditor, prefs, actions, composerTools)
                }
            }
        }
    }
}

@Composable
private fun ComposerInputField(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    expandedEditor: Boolean,
    textSize: ConversationTextSize,
    inputFocus: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    onShortcutSend: (KeyEvent) -> Boolean,
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
                    .onFocusChanged { onFocusChange(it.isFocused) }
                    .onPreviewKeyEvent(onShortcutSend),
            placeholder = { Text(localizedString(Res.string.message_placeholder)) },
            maxLines = if (expandedEditor) Int.MAX_VALUE else COMPACT_COMPOSER_MAX_LINES,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }
}

@Composable
private fun ComposerActionRow(
    state: ConversationUiState,
    draftText: String,
    expandedEditor: Boolean,
    prefs: ComposerPrefs,
    actions: ComposerActions,
    composerTools: @Composable RowScope.() -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        composerTools()
        if (shouldShowExpandToggle(draftText, expandedEditor)) {
            ComposerExpandToggleButton(expandedEditor, actions.onToggleExpanded)
        }
        actions.onCollapseInput?.let { onHide ->
            ComposerHideInputButton(onHide)
        }
        ComposerSubmitButton(state, prefs.shortcut, prefs.canSend, actions)
    }
}

@Composable
private fun ComposerHideInputButton(onHide: () -> Unit) {
    val label = localizedString(Res.string.composer_hide_input)
    AppTooltip(label) {
        IconButton(onClick = onHide) {
            AppIcon(AppSymbol.ChevronDown, description = label)
        }
    }
}

private fun shouldShowExpandToggle(
    draftText: String,
    expandedEditor: Boolean,
): Boolean = expandedEditor || draftText.length > EXPAND_EDITOR_LENGTH || draftText.count { it == '\n' } >= EXPAND_EDITOR_LINES

@Composable
private fun ComposerExpandToggleButton(
    expandedEditor: Boolean,
    onToggle: () -> Unit,
) {
    val editorLabel = localizedString(if (expandedEditor) Res.string.collapse_composer else Res.string.expand_composer)
    AppTooltip(editorLabel) {
        IconButton(onClick = onToggle) {
            AppIcon(if (expandedEditor) AppSymbol.Collapse else AppSymbol.Expand, description = editorLabel)
        }
    }
}

@Composable
private fun ComposerSubmitButton(
    state: ConversationUiState,
    shortcut: SendShortcut,
    canSend: Boolean,
    actions: ComposerActions,
) {
    val tooltip =
        when {
            state.isResponding -> localizedString(Res.string.stop_reply)
            desktopUi && shortcut == SendShortcut.Enter -> localizedString(Res.string.send_shortcut_enter)
            desktopUi -> localizedString(Res.string.send_shortcut, primaryShortcutModifier)
            else -> localizedString(Res.string.send_message)
        }
    val actionDescription = localizedString(if (state.isResponding) Res.string.stop_reply else Res.string.send_message)
    val haptic = LocalHapticFeedback.current
    AppTooltip(tooltip) {
        FilledIconButton(
            onClick = {
                haptic.performHapticFeedback(
                    if (state.isResponding) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove,
                )
                if (state.isResponding) actions.onStop() else actions.onSend()
            },
            enabled = state.isResponding || (canSend && state.hasSendableContent),
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            AppIcon(if (state.isResponding) AppSymbol.Stop else AppSymbol.Send, description = actionDescription)
        }
    }
}

private fun handleSendShortcutKey(
    event: KeyEvent,
    shortcut: SendShortcut,
    composing: Boolean,
    onSend: () -> Unit,
): Boolean {
    if (event.type != KeyEventType.KeyDown || event.key != Key.Enter) return false
    val matched =
        shortcut.matches(
            ctrl = event.isCtrlPressed,
            meta = event.isMetaPressed,
            shift = event.isShiftPressed,
            alt = event.isAltPressed,
            desktop = desktopUi,
            composing = composing,
        )
    if (matched) onSend()
    return matched
}
