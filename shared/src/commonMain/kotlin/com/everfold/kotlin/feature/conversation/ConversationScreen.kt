package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.hide_sessions
import everfold.shared.generated.resources.logo
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.open_workspace
import everfold.shared.generated.resources.show_sessions
import everfold.shared.generated.resources.suggestion_ideas
import everfold.shared.generated.resources.suggestion_problem
import everfold.shared.generated.resources.suggestion_week
import everfold.shared.generated.resources.welcome_title
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 会话页面：OS4 玻璃顶栏、倒序消息流与悬浮超椭圆输入区 */
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
    var expandedEditor by rememberSaveable(state.id) { mutableStateOf(false) }
    val inputFocus = remember { FocusRequester() }
    LaunchedEffect(state.id, autoFocusInput, inputFocusRequest) {
        if (desktopUi && autoFocusInput) {
            withFrameNanos { }
            inputFocus.requestFocus()
        }
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = expandedEditor,
        onBackCompleted = { expandedEditor = false },
    )
    var scrollRequest by remember(state.id) { mutableIntStateOf(0) }
    val backdrop = rememberLayerBackdrop()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(
        modifier =
            modifier.fillMaxSize().onPreviewKeyEvent {
                if (expandedEditor && it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    expandedEditor = false
                    true
                } else {
                    false
                }
            },
        contentWindowInsets = WindowInsets(0),
        topBar = {
            ConversationTopBar(
                title = state.title.resolve(),
                subtitle = connectionName,
                backdrop = backdrop,
                compactNavigation = compactNavigation,
                sessionsVisible = sessionsVisible,
                onToggleSessions = onToggleSessions,
                onNewConversation = onNewConversation,
                connectionIndicator = connectionIndicator,
                conversationMenu = conversationMenu,
            )
        },
    ) { innerPadding ->
        ConversationScaffoldBody(
            state = state,
            scrollRequest = scrollRequest,
            connectionName = connectionName,
            textSize = textSize,
            shortcut = shortcut,
            collapsePrompts = collapsePrompts,
            sideMargins = sideMargins,
            canSend = canSend,
            expandedEditor = expandedEditor,
            onExpandedEditorChange = { expandedEditor = it },
            inputFocus = inputFocus,
            topPadding = innerPadding.calculateTopPadding(),
            bottomPadding = if (imeOpen) 0.dp else innerPadding.calculateBottomPadding(),
            backdrop = backdrop,
            onDraftChange = onDraftChange,
            onSend = {
                scrollRequest++
                expandedEditor = false
                onSend()
            },
            onStop = onStop,
            onRetry = onRetry,
            composerFeedback = composerFeedback,
            composerTools = composerTools,
            attachmentsContent = attachmentsContent,
        )
    }
}

@Composable
private fun ConversationScaffoldBody(
    state: ConversationUiState,
    scrollRequest: Int,
    connectionName: String,
    textSize: ConversationTextSize,
    shortcut: SendShortcut,
    collapsePrompts: Boolean,
    sideMargins: Boolean,
    canSend: Boolean,
    expandedEditor: Boolean,
    onExpandedEditorChange: (Boolean) -> Unit,
    inputFocus: FocusRequester,
    topPadding: Dp,
    bottomPadding: Dp,
    backdrop: LayerBackdrop,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    composerFeedback: @Composable () -> Unit,
    composerTools: @Composable RowScope.() -> Unit,
    attachmentsContent: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
        Column(Modifier.fillMaxSize()) {
            if (!expandedEditor) {
                Box(Modifier.weight(1f).fillMaxWidth().layerBackdrop(backdrop)) {
                    ConversationBody(
                        state = state,
                        scrollRequest = scrollRequest,
                        connectionName = connectionName,
                        textSize = textSize,
                        collapsePrompts = collapsePrompts,
                        sideMargins = sideMargins,
                        canSend = canSend,
                        topPadding = topPadding,
                        onDraftChange = onDraftChange,
                        onFocusInput = { inputFocus.requestFocus() },
                        onRetry = onRetry,
                    )
                }
            } else {
                Spacer(Modifier.height(topPadding))
            }
            ConversationComposer(
                state = state,
                textSize = textSize,
                shortcut = shortcut,
                sideMargins = sideMargins,
                canSend = canSend,
                expandedEditor = expandedEditor,
                onExpandedEditorChange = onExpandedEditorChange,
                inputFocus = inputFocus,
                bottomPadding = bottomPadding,
                onDraftChange = onDraftChange,
                onSend = onSend,
                onStop = onStop,
                composerFeedback = composerFeedback,
                composerTools = composerTools,
                attachmentsContent = attachmentsContent,
                modifier = if (expandedEditor) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ConversationTopBar(
    title: String,
    subtitle: String,
    backdrop: LayerBackdrop,
    compactNavigation: Boolean,
    sessionsVisible: Boolean,
    onToggleSessions: () -> Unit,
    onNewConversation: () -> Unit,
    connectionIndicator: @Composable () -> Unit,
    conversationMenu: @Composable () -> Unit,
) {
    val toggleLabel =
        localizedString(
            when {
                compactNavigation -> Res.string.open_workspace
                sessionsVisible -> Res.string.hide_sessions
                else -> Res.string.show_sessions
            },
        )
    val newLabel = localizedString(Res.string.new_session)
    GlassTopAppBar(
        title = title,
        subtitle = subtitle,
        expandable = false,
        progressiveBlur = true,
        backdrop = backdrop,
        navigationIcon = {
            AppTooltip(toggleLabel) {
                GlassIconButton(onClick = onToggleSessions) {
                    AppIcon(
                        if (compactNavigation) AppSymbol.Back else AppSymbol.Sidebar,
                        modifier = Modifier.size(28.dp),
                        description = toggleLabel,
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                connectionIndicator()
                AppTooltip(newLabel) {
                    GlassIconButton(onClick = onNewConversation) {
                        AppIcon(
                            AppSymbol.Plus,
                            modifier = Modifier.size(28.dp),
                            description = newLabel,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                conversationMenu()
            }
        },
    )
}

@Composable
private fun ConversationBody(
    state: ConversationUiState,
    scrollRequest: Int,
    connectionName: String,
    textSize: ConversationTextSize,
    collapsePrompts: Boolean,
    sideMargins: Boolean,
    canSend: Boolean,
    topPadding: Dp,
    onDraftChange: (String) -> Unit,
    onFocusInput: () -> Unit,
    onRetry: () -> Unit,
) {
    if (state.messages.isEmpty()) {
        WelcomeContent(
            onSuggestion = {
                onDraftChange(it)
                onFocusInput()
            },
            topPadding = topPadding,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        key(state.id) {
            ConversationMessages(
                state = state,
                scrollRequest = scrollRequest,
                connectionName = connectionName,
                onJumpToLatest = { if (desktopUi) onFocusInput() },
                onRetry = onRetry,
                canRetry = canSend && state.workspace != null && state.model != null,
                textSize = textSize,
                collapsePrompts = collapsePrompts,
                sideMargins = sideMargins,
                topPadding = topPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 按角色展示消息：用户提示支持超椭圆气泡与折叠，助手回复渲染 Markdown */
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
            if (!user) AppIcon(AppSymbol.Spark, modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.primary)
            Text(
                text = if (modelName == null) authorName else "$authorName · $modelName",
                style = MiuixTheme.textStyles.footnote1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
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
                    Surface(
                        modifier = Modifier.squircleSurface(color = MiuixTheme.colorScheme.secondaryContainer, cornerRadius = 20.dp),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = MiuixTheme.colorScheme.onSecondaryContainer,
                    ) {
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

/** 空会话欢迎页：点击建议词条填入草稿并聚焦输入框 */
@Composable
private fun WelcomeContent(
    onSuggestion: (String) -> Unit,
    topPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val suggestions =
        listOf(
            localizedString(Res.string.suggestion_ideas),
            localizedString(Res.string.suggestion_week),
            localizedString(Res.string.suggestion_problem),
        )
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = topPadding + 28.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        item(key = "logo") {
            Image(
                painter = painterResource(Res.drawable.logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                alpha = 0.85f,
            )
        }
        item(key = "title") {
            Text(
                text = localizedString(Res.string.welcome_title),
                style = MiuixTheme.textStyles.title2,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
            )
        }
        items(suggestions, key = { it }) { suggestion ->
            Card(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                cornerRadius = 16.dp,
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                onClick = { onSuggestion(suggestion) },
            ) {
                Text(
                    text = suggestion,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
