package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.feature.settings.ConversationTextScale
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.AppTooltip
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.primaryShortcutModifier
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.hide_sessions
import everfold.shared.generated.resources.logo
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.new_session_shortcut
import everfold.shared.generated.resources.open_workspace
import everfold.shared.generated.resources.show_sessions
import everfold.shared.generated.resources.suggestion_ideas
import everfold.shared.generated.resources.suggestion_problem
import everfold.shared.generated.resources.suggestion_week
import everfold.shared.generated.resources.welcome_title
import org.jetbrains.compose.resources.painterResource

private const val DIVIDER_ALPHA = 0.5f
private const val WELCOME_LOGO_ALPHA = 0.75f

/** 会话页面组合导航执行状态消息与输入框具体动作交由外部回调处理 */
@Composable
fun ConversationScreen(
    state: ConversationUiState,
    connectionName: String,
    compactNavigation: Boolean,
    onNewConversation: () -> Unit,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    connectionIndicator: @Composable () -> Unit,
    conversationMenu: @Composable () -> Unit,
    composerFeedback: @Composable () -> Unit,
    canSend: Boolean,
    onRetry: () -> Unit,
    autoFocusInput: Boolean,
    sessionsVisible: Boolean,
    onToggleSessions: () -> Unit,
    modifier: Modifier = Modifier,
    inputFocusRequest: Int = 0,
    textSize: ConversationTextSize = ConversationTextSize.System,
    shortcut: SendShortcut = SendShortcut.ModifiedEnter,
    collapsePrompts: Boolean = true,
    sideMargins: Boolean = true,
    composerTools: @Composable RowScope.() -> Unit = {},
    attachmentsContent: @Composable () -> Unit = {},
) {
    // 按会话保留编辑状态仅在外部草稿改变时同步避免打断光标和输入法组合
    var expandedEditor by rememberSaveable(state.id) { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }
    ConversationFocusEffect(state.id, autoFocusInput, inputFocusRequest, inputFocus)
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None), expandedEditor) { expandedEditor = false }
    var scrollRequest by remember(state.id) { mutableIntStateOf(0) }
    val prefs = ComposerPrefs(textSize, shortcut, sideMargins, canSend)
    val actions =
        buildComposerActions(
            onDraftChange = onDraftChange,
            onSend = {
                scrollRequest++
                expandedEditor = false
                onSend()
            },
            onStop = onStop,
            onToggleExpanded = {
                expandedEditor = !expandedEditor
                inputFocus.requestFocus()
            },
        )
    Column(modifier.fillMaxSize().collapseEditorOnEscape(expandedEditor) { expandedEditor = false }) {
        ConversationTopBar(
            state.title.resolve(),
            compactNavigation,
            sessionsVisible,
            onToggleSessions,
            onNewConversation,
            connectionIndicator,
            conversationMenu,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
        if (!expandedEditor) {
            ConversationBody(
                state,
                connectionName,
                scrollRequest,
                canSend && state.workspace != null && state.model != null,
                prefs,
                collapsePrompts,
                { onDraftChange(it).also { inputFocus.requestFocus() } },
                { if (desktopUi) inputFocus.requestFocus() },
                onRetry,
                Modifier.weight(1f),
            )
        }
        ConversationComposer(
            state,
            expandedEditor,
            inputFocus,
            prefs,
            actions,
            composerFeedback,
            composerTools,
            attachmentsContent,
            if (expandedEditor) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth(),
        )
    }
}

private fun buildComposerActions(
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onToggleExpanded: () -> Unit,
): ComposerActions = ComposerActions(onDraftChange, onSend, onStop, onToggleExpanded)

@Composable
private fun ConversationFocusEffect(
    conversationId: String,
    autoFocusInput: Boolean,
    inputFocusRequest: Int,
    inputFocus: FocusRequester,
) {
    LaunchedEffect(conversationId, autoFocusInput, inputFocusRequest) {
        if (desktopUi && autoFocusInput) {
            withFrameNanos { }
            inputFocus.requestFocus()
        }
    }
}

private fun Modifier.collapseEditorOnEscape(
    expandedEditor: Boolean,
    onCollapse: () -> Unit,
): Modifier =
    onPreviewKeyEvent { event ->
        val shouldCollapse = expandedEditor && event.type == KeyEventType.KeyDown && event.key == Key.Escape
        if (shouldCollapse) onCollapse()
        shouldCollapse
    }

@Composable
private fun ConversationTopBar(
    title: String,
    compactNavigation: Boolean,
    sessionsVisible: Boolean,
    onToggleSessions: () -> Unit,
    onNewConversation: () -> Unit,
    connectionIndicator: @Composable () -> Unit,
    conversationMenu: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val toggleLabel =
            when {
                compactNavigation -> localizedString(Res.string.open_workspace)
                sessionsVisible -> localizedString(Res.string.hide_sessions, primaryShortcutModifier)
                else -> localizedString(Res.string.show_sessions, primaryShortcutModifier)
            }
        AppTooltip(toggleLabel) {
            IconButton(onClick = onToggleSessions) {
                AppIcon(if (compactNavigation) AppSymbol.Menu else AppSymbol.Sidebar, description = toggleLabel)
            }
        }
        Row(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            connectionIndicator()
            Text(
                title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val newConversationLabel = localizedString(Res.string.new_session)
        val newConversationTooltip =
            if (desktopUi) {
                localizedString(Res.string.new_session_shortcut, primaryShortcutModifier)
            } else {
                newConversationLabel
            }
        AppTooltip(newConversationTooltip) {
            IconButton(onClick = onNewConversation) { AppIcon(AppSymbol.Plus, description = newConversationLabel) }
        }
        conversationMenu()
    }
}

@Composable
private fun ConversationBody(
    state: ConversationUiState,
    connectionName: String,
    scrollRequest: Int,
    canRetry: Boolean,
    prefs: ComposerPrefs,
    collapsePrompts: Boolean,
    onSuggestion: (String) -> Unit,
    onJumpToLatest: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.messages.isEmpty()) {
        WelcomeContent(onSuggestion, modifier)
    } else {
        key(state.id) {
            ConversationMessages(
                state,
                scrollRequest,
                connectionName = connectionName,
                onRetry = onRetry,
                canRetry = canRetry,
                textSize = prefs.textSize,
                collapsePrompts = collapsePrompts,
                sideMargins = prefs.sideMargins,
                onJumpToLatest = onJumpToLatest,
                modifier = modifier,
            )
        }
    }
}

/** 按角色展示消息用户提示可折叠助手回复支持 Markdown选择与复制 */
@Composable
internal fun MessageItem(
    message: ChatMessage,
    authorName: String,
    modifier: Modifier = Modifier,
    onPromptHeightChange: (Int) -> Unit = {},
    textSize: ConversationTextSize = ConversationTextSize.System,
    collapsePrompts: Boolean = true,
    runState: AgentRunState = AgentRunState.Idle,
    onRetry: () -> Unit = {},
    canRetry: Boolean = true,
) {
    val user = message.role == MessageRole.User
    val status = if (user) AgentRunState.Idle else runState
    val modelName =
        message.model
            ?.takeIf { !user }
            ?.name
            ?.resolve()
    Column(modifier, horizontalAlignment = if (user) Alignment.End else Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!user) AppIcon(AppSymbol.Spark, modifier = Modifier.size(18.dp))
            Text(
                if (modelName == null) authorName else "$authorName · $modelName",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (status == AgentRunState.Running) AgentStatusBar(status, onRetry, canRetry)
        Spacer(Modifier.height(10.dp))
        MessageAttachments(message.attachments)
        ConversationTextScale(textSize) {
            if (user) {
                if (message.text.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        UserPrompt(message, onPromptHeightChange, collapsePrompts)
                    }
                }
            } else if (message.text.isNotEmpty()) {
                SelectionContainer { MessageMarkdown(message.text) }
            }
        }
        if (status != AgentRunState.Running) AgentStatusBar(status, onRetry, canRetry)
    }
}

/** 空会话欢迎页点击建议只填入草稿不自动发送 */
@Composable
private fun WelcomeContent(
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestions =
        listOf(
            localizedString(Res.string.suggestion_ideas),
            localizedString(Res.string.suggestion_week),
            localizedString(Res.string.suggestion_problem),
        )
    LazyColumn(
        modifier.fillMaxWidth(),
        contentPadding = PaddingValues(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        item {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(75.dp),
                alpha = WELCOME_LOGO_ALPHA,
            )
        }
        item {
            Text(
                localizedString(Res.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
        }
        items(suggestions) { suggestion ->
            OutlinedButton(onClick = { onSuggestion(suggestion) }, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                Text(suggestion)
            }
        }
    }
}
