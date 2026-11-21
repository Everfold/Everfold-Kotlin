package com.everfold.kotlin.feature.conversation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.composer_draft_badge
import everfold.shared.generated.resources.composer_open_input
import everfold.shared.generated.resources.composer_status_running_tool
import everfold.shared.generated.resources.composer_status_streaming
import everfold.shared.generated.resources.composer_status_thinking
import everfold.shared.generated.resources.stop_reply

private const val PILL_BORDER_ALPHA = 0.65f
private const val PILL_ANIM_MILLIS = 180
private const val SLIDE_DIVISOR = 4

@Stable
internal class ExecutionComposerState(
    val collapsed: Boolean,
    val openInput: () -> Unit,
    val hideInput: () -> Unit,
)

@Composable
internal fun rememberExecutionComposerState(
    conversationId: String,
    isResponding: Boolean,
    expandedEditor: Boolean,
    showTrace: Boolean,
    inputFocus: FocusRequester,
): ExecutionComposerState {
    var revealedDuringRun by rememberSaveable(conversationId) { mutableStateOf(false) }
    var revealFocusRequest by remember(conversationId) { mutableIntStateOf(0) }
    LaunchedEffect(conversationId, isResponding) {
        if (!isResponding) revealedDuringRun = false
    }
    val collapsed = isResponding && !revealedDuringRun && !expandedEditor
    LaunchedEffect(collapsed, revealFocusRequest) {
        if (!collapsed && revealFocusRequest > 0) {
            withFrameNanos { }
            inputFocus.requestFocus()
        }
    }
    NavigationBackHandler(
        rememberNavigationEventState(NavigationEventInfo.None),
        !showTrace && !expandedEditor && isResponding && revealedDuringRun,
    ) {
        revealedDuringRun = false
    }
    return ExecutionComposerState(
        collapsed = collapsed,
        openInput = {
            revealedDuringRun = true
            revealFocusRequest++
        },
        hideInput = { revealedDuringRun = false },
    )
}

@Composable
internal fun CollapsibleConversationComposer(
    state: ConversationUiState,
    composerCollapsed: Boolean,
    expandedEditor: Boolean,
    inputFocus: FocusRequester,
    prefs: ComposerPrefs,
    actions: ComposerActions,
    onOpenInput: () -> Unit,
    composerFeedback: @Composable () -> Unit,
    composerTools: @Composable RowScope.() -> Unit,
    attachmentsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = composerCollapsed,
        transitionSpec = {
            (fadeIn(tween(PILL_ANIM_MILLIS)) + slideInVertically(tween(PILL_ANIM_MILLIS)) { it / SLIDE_DIVISOR })
                .togetherWith(fadeOut(tween(PILL_ANIM_MILLIS)))
                .using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "composerCollapseTransition",
    ) { collapsed ->
        if (collapsed) {
            ExecutionComposerPill(
                state = state,
                sideMargins = prefs.sideMargins,
                onOpenInput = onOpenInput,
                onStop = actions.onStop,
            )
        } else {
            ConversationComposer(
                state = state,
                expandedEditor = expandedEditor,
                inputFocus = inputFocus,
                prefs = prefs,
                actions = actions,
                composerFeedback = composerFeedback,
                composerTools = composerTools,
                attachmentsContent = attachmentsContent,
                modifier = if (expandedEditor) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 执行期间收起输入框后展示的底部紧凑胶囊条，显示实时步骤并提供一键展开输入与停止操作。 */
@Composable
internal fun ExecutionComposerPill(
    state: ConversationUiState,
    sideMargins: Boolean,
    onOpenInput: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = localizedString(Res.string.composer_open_input)
    val stopLabel = localizedString(Res.string.stop_reply)
    val haptic = LocalHapticFeedback.current
    val performOpen = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onOpenInput()
    }
    val performStop = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        onStop()
    }
    Box(
        modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier =
                Modifier
                    .conversationContentWidth(sideMargins)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = PILL_BORDER_ALPHA)),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ExecutionPillStatusArea(state, performOpen, Modifier.weight(1f))
                AppTooltip(openLabel) {
                    IconButton(onClick = performOpen) {
                        AppIcon(AppSymbol.Edit, modifier = Modifier.size(20.dp), description = openLabel)
                    }
                }
                AppTooltip(stopLabel) {
                    FilledIconButton(
                        onClick = performStop,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                    ) {
                        AppIcon(AppSymbol.Stop, modifier = Modifier.size(18.dp), description = stopLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutionPillStatusArea(
    state: ConversationUiState,
    onOpenInput: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable(role = Role.Button, onClick = onOpenInput)
                .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = resolveExecutionStatusText(state),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (state.draft.isNotBlank() || state.attachments.isNotEmpty()) {
            DraftIndicatorBadge()
        }
    }
}

@Composable
private fun DraftIndicatorBadge() {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = localizedString(Res.string.composer_draft_badge),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

@Composable
private fun resolveExecutionStatusText(state: ConversationUiState): String {
    val activeBlock =
        state.messages
            .lastOrNull()
            ?.streamBlocks
            ?.lastOrNull()
    return when (activeBlock) {
        is AgentStreamBlock.ToolCall -> localizedString(Res.string.composer_status_running_tool, activeBlock.toolName)
        is AgentStreamBlock.Thinking -> localizedString(Res.string.composer_status_thinking)
        is AgentStreamBlock.Narration -> localizedString(Res.string.composer_status_streaming)
        else -> state.preview.resolve().ifBlank { localizedString(state.runState.label) }
    }
}
