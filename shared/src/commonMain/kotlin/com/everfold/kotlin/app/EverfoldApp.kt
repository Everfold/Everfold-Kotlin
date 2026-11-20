package com.everfold.kotlin.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.everfold.kotlin.app.navigation.AppDestination
import com.everfold.kotlin.feature.connections.BUILT_IN_CONNECTION_ID
import com.everfold.kotlin.feature.connections.ConnectionsScreen
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.AttachmentSource
import com.everfold.kotlin.feature.sessions.SessionsScreen
import com.everfold.kotlin.feature.settings.SettingsScreen
import com.everfold.kotlin.mock.longtask.PlatformLongTaskPermissionEffect
import com.everfold.kotlin.mock.longtask.platformLongTaskBridge
import com.everfold.kotlin.preferences.UserPreferencesState
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.WindowBreakpoints
import com.everfold.kotlin.ui.components.desktopUi
import com.everfold.kotlin.ui.components.horizontalResizePointer
import com.everfold.kotlin.ui.files.FilePicker
import com.everfold.kotlin.ui.files.rememberFilePicker
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.workspace.Workspace
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.resize_sessions
import kotlinx.coroutines.launch

private val DrawerMaxWidth = 360.dp
private val ResizeHandleWidthDesktop = 10.dp
private val ResizeHandleWidthTouch = 1.dp

@Immutable
private data class SidebarLayout(
    val wide: Boolean,
    val maxWidthDp: Float,
)

/** 主界面组装入口连接页面与工作区并协调宽屏侧栏窄屏抽屉和快捷键 */
@Composable
internal fun EverfoldApp(
    workspace: Workspace,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    onPreviewState: ((String, AgentRunState) -> Unit)? = null,
    connectionOverviewExtra: @Composable (String) -> Unit = {},
    filePickerProvider: @Composable ((Result<List<AttachmentSource>>) -> Unit) -> FilePicker = { rememberFilePicker(it) },
    preferences: UserPreferencesState = remember { UserPreferencesState() },
) {
    val appState = rememberEverfoldAppState()
    val pendingOpenId by platformLongTaskBridge.pendingOpenConversationId.collectAsState()
    LaunchedEffect(pendingOpenId) {
        val targetId = pendingOpenId ?: return@LaunchedEffect
        if (workspace.conversations.any { it.id == targetId }) {
            appState.destination = AppDestination.Sessions
            appState.selectedId = targetId
        }
        platformLongTaskBridge.consumeOpenConversation(targetId)
    }
    val hasActiveLocalTask =
        workspace.conversations.any {
            it.connectionId == BUILT_IN_CONNECTION_ID &&
                (it.runState == AgentRunState.Running || it.runState == AgentRunState.Waiting)
        }
    PlatformLongTaskPermissionEffect(hasActiveLocalTask)
    val filePicker =
        filePickerProvider { result ->
            val target = appState.attachments.pendingTarget ?: return@filePickerProvider
            appState.attachments.pendingTarget = null
            result
                .onSuccess { files ->
                    if (files.isNotEmpty()) workspace.conversationOptions.attach(target, files)
                }.onFailure { appState.attachments.errorTarget = target }
        }
    val bindings = ShellBindings(workspace, preferences, language, onLanguageChange, onPreviewState, connectionOverviewExtra, filePicker)
    Surface(modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().focusable()) {
            val wide = maxWidth >= WindowBreakpoints.Expanded
            val maxSessionWidth = (maxWidth.value - SESSION_WIDTH_RESERVE_DP).coerceIn(MIN_SESSION_WIDTH_DP, MAX_SESSION_WIDTH_DP)
            val layout = SidebarLayout(wide = wide, maxWidthDp = maxSessionWidth)
            AppNavigationChrome(
                appState = appState,
                wide = wide,
                sessionsContent = { listModifier, closeDrawer ->
                    SessionsSidebarContent(appState, workspace, closeDrawer, listModifier)
                },
            ) { drawer ->
                AppDestinationContent(appState, bindings, drawer, layout)
            }
        }
    }
}

@Composable
private fun AppNavigationChrome(
    appState: EverfoldAppState,
    wide: Boolean,
    sessionsContent: @Composable (Modifier, () -> Unit) -> Unit,
    content: @Composable (DrawerActions) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val drawerState = remember(wide) { DrawerState(DrawerValue.Closed) }
    val drawerClosed = drawerState.isClosed && drawerState.targetValue == DrawerValue.Closed
    var drawerHasFocus by remember(wide) { mutableStateOf(false) }
    LaunchedEffect(drawerClosed, drawerHasFocus) {
        if (!wide && drawerClosed && drawerHasFocus) focusManager.clearFocus(force = true)
    }
    val closeDrawer: () -> Unit = {
        focusManager.clearFocus()
        scope.launch { drawerState.close() }
    }
    val openDrawer: () -> Unit = {
        focusManager.clearFocus()
        scope.launch { drawerState.open() }
    }
    val drawer = DrawerActions(openDrawer, closeDrawer) { appState.navigateBack(wide, drawerState.isOpen, closeDrawer) }
    val canGoBack = (!wide && drawerState.isOpen) || appState.destination != AppDestination.Sessions
    NavigationBackHandler(
        rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = canGoBack,
        onBackCompleted = drawer.navigateBack,
    )
    val shortcuts =
        ShortcutHandlers(
            onNavigateBack = drawer.navigateBack,
            onNewConversation = { appState.openNewConversation(closeDrawer) },
            onFocusSearch = {
                appState.startSearch()
                focusManager.clearFocus()
                if (!wide) scope.launch { drawerState.open() }
            },
            onToggleSidebar = {
                appState.focusComposer = true
                if (wide) {
                    appState.sessionsVisible = !appState.sessionsVisible
                } else if (drawerState.isOpen) {
                    closeDrawer()
                } else {
                    openDrawer()
                }
            },
        )
    ModalNavigationDrawer(
        modifier = Modifier.fillMaxSize().everfoldShortcuts(canGoBack, wide, drawerState.isOpen, shortcuts),
        drawerState = drawerState,
        gesturesEnabled = !wide && drawerState.isOpen,
        drawerContent = {
            if (!wide) CompactNavigationDrawerSheet(appState, closeDrawer, { drawerHasFocus = it }, sessionsContent)
        },
    ) {
        Row(Modifier.fillMaxSize()) {
            if (wide) AppNavigationRail(appState)
            Box(Modifier.weight(1f)) { content(drawer) }
        }
    }
}

@Composable
private fun CompactNavigationDrawerSheet(
    appState: EverfoldAppState,
    closeDrawer: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    sessionsContent: @Composable (Modifier, () -> Unit) -> Unit,
) {
    ModalDrawerSheet(
        Modifier
            .widthIn(max = DrawerMaxWidth)
            .fillMaxHeight()
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .focusable(),
    ) {
        sessionsContent(Modifier.weight(1f), closeDrawer)
        HorizontalDivider()
        listOf(AppDestination.Connections, AppDestination.Settings).forEach { item ->
            NavigationDrawerItem(
                label = { Text(localizedString(item.label)) },
                icon = { AppIcon(item.icon) },
                selected = appState.destination == item,
                onClick = {
                    appState.destination = item
                    closeDrawer()
                },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AppNavigationRail(appState: EverfoldAppState) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        AppDestination.entries.forEach { item ->
            NavigationRailItem(
                selected = appState.destination == item,
                onClick = { appState.destination = item },
                icon = { AppIcon(item.icon) },
                label = { Text(localizedString(item.label)) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun SessionsSidebarContent(
    appState: EverfoldAppState,
    workspace: Workspace,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val connectionNames =
        remember(workspace.connections.connections) {
            workspace.connections.connections.associate { it.id to it.profile.name }
        }
    appState.sessionListState.SaveableStateProvider("sessions") {
        SessionsScreen(
            sessions = workspace.conversations,
            connectionNames = connectionNames,
            selectedId = appState.selectedId?.takeIf { id -> workspace.conversations.any { it.id == id } },
            searchRequest = appState.searchRequest,
            query = appState.sessionQuery,
            onQueryChange = { appState.sessionQuery = it },
            onSelect = { appState.selectSession(it, closeDrawer) },
            onRename = workspace::renameConversation,
            onDelete = { id -> appState.deleteSession(workspace, id) },
            onTogglePin = workspace::togglePinned,
            onCreate = { appState.openNewConversation(closeDrawer) },
            modifier = modifier,
        )
    }
}

@Composable
private fun AppDestinationContent(
    appState: EverfoldAppState,
    bindings: ShellBindings,
    drawer: DrawerActions,
    layout: SidebarLayout,
) {
    val workspace = bindings.workspace
    val preferences = bindings.preferences
    when (appState.destination) {
        AppDestination.Sessions -> {
            SessionsWorkspacePane(appState, bindings, drawer, layout)
        }

        AppDestination.Connections -> {
            appState.connectionPageStates.SaveableStateProvider("connections") {
                ConnectionsScreen(
                    workspace.connections,
                    appState.connectionNavigation,
                    onNewConversation = { id ->
                        workspace.selectNewConversationConnection(id)
                        appState.openNewConversation(drawer.close)
                    },
                    onBack = drawer.navigateBack,
                    overviewExtra = bindings.connectionOverviewExtra,
                )
            }
        }

        AppDestination.Settings -> {
            SettingsScreen(
                textSize = preferences.value.textSize,
                onTextSizeChange = { value -> preferences.update { it.copy(textSize = value) } },
                shortcut = preferences.value.shortcut,
                onShortcutChange = { value -> preferences.update { it.copy(shortcut = value) } },
                collapsePrompts = preferences.value.collapsePrompts,
                onCollapsePromptsChange = { value -> preferences.update { it.copy(collapsePrompts = value) } },
                sideMargins = preferences.value.sideMargins,
                onSideMarginsChange = { value -> preferences.update { it.copy(sideMargins = value) } },
                language = bindings.language,
                onLanguageChange = bindings.onLanguageChange,
                themeMode = preferences.themeMode,
                themeColor = preferences.themeColor,
                onThemeChange = preferences::setTheme,
                onSystemColorChange = preferences::useSystemThemeColor,
                onPresetChange = preferences::selectThemePreset,
                onCustomColorChange = preferences::applyCustomThemeColor,
                onBack = if (layout.wide) null else ({ appState.destination = AppDestination.Sessions }),
            )
        }
    }
}

@Composable
private fun SessionsWorkspacePane(
    appState: EverfoldAppState,
    bindings: ShellBindings,
    drawer: DrawerActions,
    layout: SidebarLayout,
) {
    val workspace = bindings.workspace
    val selected = workspace.conversations.firstOrNull { it.id == appState.selectedId }
    Row(Modifier.fillMaxSize()) {
        if (layout.wide && appState.sessionsVisible) {
            Surface(
                modifier = Modifier.width(appState.sessionWidth.coerceIn(MIN_SESSION_WIDTH_DP, layout.maxWidthDp).dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                SessionsSidebarContent(
                    appState = appState,
                    workspace = workspace,
                    closeDrawer = drawer.close,
                    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)),
                )
            }
            SidebarResizeHandle(appState, layout.maxWidthDp)
        }
        appState.conversationStates.SaveableStateProvider(selected?.id ?: workspace.newConversation.id) {
            ConversationSessionPane(appState, bindings, selected, layout.wide, drawer, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SidebarResizeHandle(
    appState: EverfoldAppState,
    maxSessionWidth: Float,
) {
    val density = LocalDensity.current.density
    val resizeLabel = localizedString(Res.string.resize_sessions)
    Box(
        Modifier
            .width(if (desktopUi) ResizeHandleWidthDesktop else ResizeHandleWidthTouch)
            .fillMaxHeight()
            .horizontalResizePointer()
            .draggable(
                rememberDraggableState { delta -> appState.resizeSidebar(delta / density, maxSessionWidth) },
                Orientation.Horizontal,
            ).semantics { contentDescription = resizeLabel }
            .onKeyEvent { event -> handleSidebarResizeKey(event, appState, maxSessionWidth) }
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
