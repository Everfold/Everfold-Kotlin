package com.everfold.kotlin.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.feature.connections.ConnectionNavigationState
import com.everfold.kotlin.feature.connections.ConnectionStatusIndicator
import com.everfold.kotlin.feature.connections.ConnectionsScreen
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentButton
import com.everfold.kotlin.feature.conversation.AttachmentStatus
import com.everfold.kotlin.feature.conversation.AttachmentTarget
import com.everfold.kotlin.feature.conversation.ComposerFeedback
import com.everfold.kotlin.feature.conversation.ConversationMenu
import com.everfold.kotlin.feature.conversation.ConversationScreen
import com.everfold.kotlin.feature.conversation.DraftAttachments
import com.everfold.kotlin.feature.conversation.FilePickerError
import com.everfold.kotlin.feature.conversation.ModelSelector
import com.everfold.kotlin.feature.sessions.SessionsScreen
import com.everfold.kotlin.feature.settings.SettingsScreen
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.horizontalResizePointer
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.files.rememberFilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.resize_sessions
import kotlinx.coroutines.launch

/** 主界面组装入口连接页面与工作区并协调宽屏侧栏窄屏抽屉和快捷键 */
@Composable
internal fun EverfoldApp(
    workspace: Workspace,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    onPreviewState: ((String, AgentRunState) -> Unit)? = null,
    connectionOverviewExtra: @Composable (String) -> Unit = {},
    filePicker: FilePicker = rememberFilePicker(),
    preferences: UserPreferencesState = remember { UserPreferencesState() },
) {
    var fileErrorTarget by remember { mutableStateOf<AttachmentTarget?>(null) }
    val textSize = preferences.value.textSize
    val shortcut = preferences.value.shortcut
    val collapsePrompts = preferences.value.collapsePrompts
    val sideMargins = preferences.value.sideMargins

    var destination by rememberSaveable { mutableStateOf(AppDestination.Sessions) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val conversationStates = rememberSaveableStateHolder()
    val sessionListState = rememberSaveableStateHolder()
    val connectionPageStates = rememberSaveableStateHolder()
    val connectionNavigation = rememberSaveable(saver = ConnectionNavigationState.Saver) { ConnectionNavigationState() }

    var focusComposer by rememberSaveable { mutableStateOf(true) }
    var sessionsVisible by rememberSaveable { mutableStateOf(true) }
    var sessionWidth by rememberSaveable { mutableFloatStateOf(300f) }
    var sessionQuery by rememberSaveable { mutableStateOf("") }
    var searchRequest by rememberSaveable { mutableIntStateOf(0) }
    var composerFocusRequest by rememberSaveable { mutableIntStateOf(0) }
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .focusable()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
        ) {
            // 宽屏使用固定侧栏窄屏复用同一会话列表作为抽屉内容
            val wide = maxWidth >= 900.dp
            val drawerState = remember(wide) { DrawerState(DrawerValue.Closed) }
            val drawerClosed = drawerState.isClosed && drawerState.targetValue == DrawerValue.Closed
            var drawerHasFocus by remember(wide) { mutableStateOf(false) }
            LaunchedEffect(drawerClosed, drawerHasFocus) {
                if (!wide && drawerClosed && drawerHasFocus) {
                    focusManager.clearFocus(force = true)
                }
            }
            val closeDrawer: () -> Unit = {
                focusManager.clearFocus()
                scope.launch { drawerState.close() }
            }
            val openDrawer: () -> Unit = {
                focusManager.clearFocus()
                scope.launch { drawerState.open() }
            }
            // 新建入口先展示独立草稿首次发送时才加入会话列表
            val openNewConversation: () -> Unit = {
                focusComposer = true
                destination = AppDestination.Sessions
                sessionQuery = ""
                selectedId = null
                closeDrawer()
                composerFocusRequest++
            }

            val maxSessionWidth = (maxWidth.value - 520f).coerceIn(260f, 440f)
            val density = LocalDensity.current.density

            // 拖动调整宽度
            val resizeLabel = localizedString(Res.string.resize_sessions)
            val selected = workspace.conversations.firstOrNull { it.id == selectedId }
            val sessions: @Composable (Modifier) -> Unit = { listModifier ->
                sessionListState.SaveableStateProvider("sessions") {
                    SessionsScreen(
                        sessions = workspace.conversations,
                        connectionNames = workspace.connections.connections.associate { it.id to it.profile.name },
                        selectedId = selected?.id,
                        searchRequest = searchRequest,
                        query = sessionQuery,
                        onQueryChange = { sessionQuery = it },
                        onSelect = {
                            focusComposer = true
                            selectedId = it
                            destination = AppDestination.Sessions
                            closeDrawer()
                        },
                        onRename = workspace::renameConversation,
                        onDelete = { id ->
                            workspace.deleteConversation(id)
                            conversationStates.removeState(id)
                            if (selectedId == id) selectedId = null
                        },
                        onTogglePin = workspace::togglePinned,
                        onCreate = openNewConversation,
                        modifier = listModifier,
                    )
                }
            }
            val navigateBack: () -> Unit = {
                if (!wide && drawerState.isOpen) {
                    closeDrawer()
                } else if (destination != AppDestination.Connections || !connectionNavigation.back()) {
                    destination = AppDestination.Sessions
                }
            }
            val sessionConnectionId = (selected ?: workspace.newConversation).connectionId
            val sessionConnection = workspace.connections.find(sessionConnectionId)
            val openSessionConnection: () -> Unit = {
                if (sessionConnection != null) {
                    connectionNavigation.open(sessionConnectionId)
                } else {
                    connectionNavigation.showList()
                }
                destination = AppDestination.Connections
            }
            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = (!wide && drawerState.isOpen) || destination != AppDestination.Sessions,
                onBackCompleted = navigateBack,
            )
            ModalNavigationDrawer(
                modifier =
                    Modifier.fillMaxSize().onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape &&
                            ((!wide && drawerState.isOpen) || destination != AppDestination.Sessions)
                        ) {
                            navigateBack()
                            true
                        } else if (it.type == KeyEventType.KeyDown && (it.isCtrlPressed || it.isMetaPressed)) {
                            when (it.key) {
                                Key.N -> {
                                    openNewConversation()
                                    true
                                }

                                Key.F -> {
                                    focusComposer = false
                                    destination = AppDestination.Sessions
                                    sessionsVisible = true
                                    focusManager.clearFocus()
                                    scope.launch {
                                        if (!wide) drawerState.open()
                                        searchRequest++
                                    }
                                    true
                                }

                                Key.B -> {
                                    focusComposer = true
                                    if (wide) {
                                        sessionsVisible = !sessionsVisible
                                    } else if (drawerState.isOpen) {
                                        closeDrawer()
                                    } else {
                                        openDrawer()
                                    }
                                    true
                                }

                                else -> {
                                    false
                                }
                            }
                        } else {
                            false
                        }
                    },
                drawerState = drawerState,
                // 只在抽屉打开后允许手势避免与消息区域的滑动冲突
                gesturesEnabled = !wide && drawerState.isOpen,
                drawerContent = {
                    if (!wide) {
                        ModalDrawerSheet(
                            Modifier
                                .widthIn(max = 360.dp)
                                .fillMaxHeight()
                                .onFocusChanged { drawerHasFocus = it.hasFocus }
                                .focusable(),
                        ) {
                            sessions(Modifier.weight(1f))
                            HorizontalDivider()
                            listOf(AppDestination.Connections, AppDestination.Settings).forEach { item ->
                                NavigationDrawerItem(
                                    // 标签见 AppDestination
                                    label = { Text(localizedString(item.label)) },
                                    icon = { AppIcon(item.icon) },
                                    selected = destination == item,
                                    onClick = {
                                        destination = item
                                        closeDrawer()
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                },
            ) {
                Row(Modifier.fillMaxSize()) {
                    if (wide) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            AppDestination.entries.forEach { item ->
                                NavigationRailItem(
                                    selected = destination == item,
                                    onClick = { destination = item },
                                    icon = { AppIcon(item.icon) },
                                    // 标签见 AppDestination
                                    label = { Text(localizedString(item.label)) },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                )
                            }
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Box(Modifier.weight(1f)) {
                            when (destination) {
                                AppDestination.Sessions -> {
                                    Row(Modifier.fillMaxSize()) {
                                        if (wide && sessionsVisible) {
                                            Surface(
                                                modifier = Modifier.width(sessionWidth.coerceIn(260f, maxSessionWidth).dp).fillMaxHeight(),
                                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                            ) { sessions(Modifier.fillMaxSize()) }
                                        }
                                        if (wide && sessionsVisible) {
                                            Box(
                                                Modifier
                                                    .width(if (desktopUi) 10.dp else 1.dp)
                                                    .fillMaxHeight()
                                                    .horizontalResizePointer()
                                                    .draggable(
                                                        rememberDraggableState { delta ->
                                                            sessionWidth =
                                                                (
                                                                    sessionWidth.coerceAtMost(
                                                                        maxSessionWidth,
                                                                    ) + delta / density
                                                                ).coerceIn(260f, maxSessionWidth)
                                                        },
                                                        Orientation.Horizontal,
                                                    ).semantics { contentDescription = resizeLabel }
                                                    .onKeyEvent { event ->
                                                        if (event.type == KeyEventType.KeyDown &&
                                                            (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
                                                        ) {
                                                            sessionWidth =
                                                                (
                                                                    sessionWidth +
                                                                        if (event.key ==
                                                                            Key.DirectionLeft
                                                                        ) {
                                                                            -16f
                                                                        } else {
                                                                            16f
                                                                        }
                                                                ).coerceIn(260f, maxSessionWidth)
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    }.focusable(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                            }
                                        }
                                        // 按会话保存输入和滚动状态切换页面时避免相互覆盖
                                        conversationStates.SaveableStateProvider(selected?.id ?: workspace.newConversation.id) {
                                            val session = selected ?: workspace.newConversation
                                            val options = workspace.conversationOptions
                                            ConversationScreen(
                                                state = session,
                                                connectionName =
                                                    sessionConnection?.profile?.name?.resolve()
                                                        ?: localizedString(Res.string.connection_removed),
                                                textSize = textSize,
                                                shortcut = shortcut,
                                                collapsePrompts = collapsePrompts,
                                                sideMargins = sideMargins,
                                                inputFocusRequest = composerFocusRequest,
                                                compactNavigation = !wide,
                                                autoFocusInput = focusComposer,
                                                sessionsVisible = sessionsVisible,
                                                onToggleSessions = {
                                                    focusComposer = true
                                                    if (wide) sessionsVisible = !sessionsVisible else openDrawer()
                                                },
                                                onNewConversation = openNewConversation,
                                                onDraftChange = {
                                                    if (selected ==
                                                        null
                                                    ) {
                                                        workspace.updateNewConversationDraft(it)
                                                    } else {
                                                        workspace.updateDraft(selected.id, it)
                                                    }
                                                },
                                                onSend = {
                                                    if (selected == null) {
                                                        workspace.submitNewConversation()?.let {
                                                            selectedId = it
                                                            sessionQuery = ""
                                                            focusComposer = true
                                                        }
                                                    } else {
                                                        workspace.send(selected.id)
                                                    }
                                                },
                                                onStop = { selected?.let { workspace.stop(it.id) } },
                                                canSend = sessionConnection?.isConnected == true,
                                                connectionIndicator = { ConnectionStatusIndicator(sessionConnection) },
                                                conversationMenu = {
                                                    ConversationMenu(
                                                        session,
                                                        workspace.connections.connections,
                                                        options,
                                                        canSelect = selected == null,
                                                        onSelectConnection = workspace::selectNewConversationConnection,
                                                        onOpenConnection = openSessionConnection,
                                                        onPreviewState =
                                                            if (selected != null && onPreviewState != null) {
                                                                { state ->
                                                                    if (state == AgentRunState.Running) {
                                                                        workspace.retry(selected.id)
                                                                    } else {
                                                                        onPreviewState(selected.id, state)
                                                                    }
                                                                }
                                                            } else {
                                                                null
                                                            },
                                                    )
                                                },
                                                composerTools = {
                                                    AttachmentButton(enabled = !session.isResponding && session.workspace != null) {
                                                        val target = session.attachmentTarget
                                                        fileErrorTarget = null
                                                        filePicker.launch { result ->
                                                            result
                                                                .onSuccess { options.attach(target, it) }
                                                                .onFailure { fileErrorTarget = target }
                                                        }
                                                    }
                                                    ModelSelector(session, options, Modifier.weight(1f))
                                                },
                                                attachmentsContent = {
                                                    DraftAttachments(session, options)
                                                    if (fileErrorTarget ==
                                                        session.attachmentTarget
                                                    ) {
                                                        FilePickerError { fileErrorTarget = null }
                                                    }
                                                },
                                                composerFeedback = {
                                                    ComposerFeedback(
                                                        session,
                                                        sessionConnection,
                                                        onConnect = { workspace.connections.connect(session.connectionId) },
                                                        onOpenConnection = openSessionConnection,
                                                        onRetryUploads = {
                                                            session.attachments.filter { it.status == AttachmentStatus.Failed }.forEach {
                                                                options.retryAttachment(session.id, it.id)
                                                            }
                                                        },
                                                    )
                                                },
                                                onRetry = { selected?.let { workspace.retry(it.id) } },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }

                                AppDestination.Connections -> {
                                    connectionPageStates.SaveableStateProvider("connections") {
                                        ConnectionsScreen(
                                            workspace.connections,
                                            connectionNavigation,
                                            onNewConversation = { id ->
                                                workspace.selectNewConversationConnection(id)
                                                openNewConversation()
                                            },
                                            onBack = navigateBack,
                                            overviewExtra = connectionOverviewExtra,
                                        )
                                    }
                                }

                                AppDestination.Settings -> {
                                    SettingsScreen(
                                        textSize = textSize,
                                        onTextSizeChange = { value -> preferences.update { it.copy(textSize = value) } },
                                        shortcut = shortcut,
                                        onShortcutChange = { value -> preferences.update { it.copy(shortcut = value) } },
                                        collapsePrompts = collapsePrompts,
                                        onCollapsePromptsChange = { value -> preferences.update { it.copy(collapsePrompts = value) } },
                                        sideMargins = sideMargins,
                                        onSideMarginsChange = { value -> preferences.update { it.copy(sideMargins = value) } },
                                        language = language,
                                        onLanguageChange = onLanguageChange,
                                        themeMode = workspace.themeMode,
                                        themeColor = workspace.themeColor,
                                        onThemeChange = workspace::setTheme,
                                        onSystemColorChange = workspace::useSystemThemeColor,
                                        onPresetChange = workspace::selectThemePreset,
                                        onCustomColorChange = workspace::applyCustomThemeColor,
                                        onBack = if (wide) null else ({ destination = AppDestination.Sessions }),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
