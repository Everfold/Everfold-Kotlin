package com.everfold.kotlin.feature.sessions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.feature.conversation.AgentRunState
import com.everfold.kotlin.feature.conversation.ConversationUiState
import com.everfold.kotlin.feature.conversation.label
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.UiText
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import com.everfold.kotlin.ui.theme.optionalLayerBackdrop
import com.everfold.kotlin.ui.theme.rememberOptionalLayerBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.clear_search
import everfold.shared.generated.resources.connection_removed
import everfold.shared.generated.resources.empty_sessions
import everfold.shared.generated.resources.nav_sessions
import everfold.shared.generated.resources.new_session
import everfold.shared.generated.resources.no_sessions
import everfold.shared.generated.resources.pinned_session
import everfold.shared.generated.resources.search_sessions
import everfold.shared.generated.resources.session_actions
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassTopAppBar
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 会话侧栏：支持宽屏紧凑列表与窄屏 OS4 玻璃大标题页面 */
@Composable
fun SessionsScreen(
    sessions: List<ConversationUiState>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    searchRequest: Int = 0,
    connectionNames: Map<String, UiText> = emptyMap(),
    compactMode: Boolean = false,
    bottomPadding: Dp = 0.dp,
) {
    val searchFocus = remember { FocusRequester() }
    SessionsSearchFocusEffect(searchRequest, searchFocus)
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<String?>(null) }
    var renameInput by rememberSaveable { mutableStateOf("") }
    var menuId by remember { mutableStateOf<String?>(null) }
    val titles = sessions.map { it.title.resolve() }
    val names = connectionNames.mapValues { it.value.resolve() }
    val filtered =
        sessions.filterIndexed { index, session ->
            titles[index].contains(query, ignoreCase = true) ||
                names[session.connectionId]?.contains(query, ignoreCase = true) == true
        }
    SessionsHostAndDialogs(
        compactMode = compactMode,
        bottomPadding = bottomPadding,
        onCreate = onCreate,
        renaming = renaming,
        deleting = deleting,
        renameInput = renameInput,
        onRenameInputChange = { renameInput = it },
        onDismissRename = { renaming = null },
        onConfirmRename = { id, newName -> onRename(id, newName).also { renaming = null } },
        onDismissDelete = { deleting = null },
        onConfirmDelete = { id -> onDelete(id).also { deleting = null } },
        modifier = modifier,
    ) { listModifier, extraPadding ->
        SessionsListContent(
            filtered = filtered,
            names = names,
            selectedId = selectedId,
            menuId = menuId,
            query = query,
            searchFocus = searchFocus,
            onQueryChange = onQueryChange,
            onSelect = onSelect,
            onOpenMenu = { menuId = it },
            onDismissMenu = { menuId = null },
            onStartRename = { session ->
                renameInput = titles[sessions.indexOf(session)]
                renaming = session.id
                menuId = null
            },
            onTogglePin = { id -> onTogglePin(id).also { menuId = null } },
            onStartDelete = { id -> deleting = id.also { menuId = null } },
            onCreate = onCreate,
            modifier = listModifier,
            contentPadding = extraPadding,
        )
    }
}

@Composable
private fun SessionsSearchFocusEffect(
    searchRequest: Int,
    searchFocus: FocusRequester,
) {
    var handledSearchRequest by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(searchRequest) {
        if (searchRequest > handledSearchRequest) {
            withFrameNanos { }
            searchFocus.requestFocus()
            handledSearchRequest = searchRequest
        }
    }
}

@Composable
private fun SessionsHostAndDialogs(
    compactMode: Boolean,
    bottomPadding: Dp,
    onCreate: () -> Unit,
    renaming: String?,
    deleting: String?,
    renameInput: String,
    onRenameInputChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable (Modifier, PaddingValues) -> Unit,
) {
    if (compactMode) {
        CompactSessionsHost(onCreate = onCreate, bottomPadding = bottomPadding, modifier = modifier, body = body)
    } else {
        WideSessionsHost(bottomPadding = bottomPadding, modifier = modifier, body = body)
    }
    SessionDialogs(
        renamingId = renaming,
        deletingId = deleting,
        renameInput = renameInput,
        onRenameInputChange = onRenameInputChange,
        onDismissRename = onDismissRename,
        onConfirmRename = onConfirmRename,
        onDismissDelete = onDismissDelete,
        onConfirmDelete = onConfirmDelete,
    )
}

@Composable
private fun CompactSessionsHost(
    onCreate: () -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    body: @Composable (Modifier, PaddingValues) -> Unit,
) {
    val backdrop = rememberOptionalLayerBackdrop()
    val scrollBehavior = MiuixScrollBehavior()
    val newSessionLabel = localizedString(Res.string.new_session)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            GlassTopAppBar(
                title = localizedString(Res.string.nav_sessions),
                expandable = true,
                progressiveBlur = true,
                backdrop = backdrop,
                scrollBehavior = scrollBehavior,
                actions = {
                    GlassIconButton(onClick = onCreate) {
                        AppIcon(
                            AppSymbol.Plus,
                            modifier = Modifier.size(28.dp),
                            description = newSessionLabel,
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
            Box(Modifier.fillMaxSize().optionalLayerBackdrop(backdrop)) {
                body(
                    Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .padding(top = innerPadding.calculateTopPadding()),
                    PaddingValues(bottom = 16.dp + bottomPadding),
                )
            }
        }
    }
}

@Composable
private fun WideSessionsHost(
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
    body: @Composable (Modifier, PaddingValues) -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = localizedString(Res.string.nav_sessions),
                modifier = Modifier.weight(1f),
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.SemiBold,
            )
        }
        body(Modifier.weight(1f), PaddingValues(bottom = 16.dp + bottomPadding))
    }
}

@Composable
private fun SessionsListContent(
    filtered: List<ConversationUiState>,
    names: Map<String, String>,
    selectedId: String?,
    menuId: String?,
    query: String,
    searchFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSelect: (String) -> Unit,
    onOpenMenu: (String) -> Unit,
    onDismissMenu: () -> Unit,
    onStartRename: (ConversationUiState) -> Unit,
    onTogglePin: (String) -> Unit,
    onStartDelete: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 16.dp),
) {
    Column(modifier.padding(horizontal = 16.dp)) {
        SessionSearchField(query, searchFocus, onQueryChange)
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = contentPadding,
        ) {
            items(filtered, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    connectionName = names[session.connectionId] ?: localizedString(Res.string.connection_removed),
                    selected = session.id == selectedId,
                    menuExpanded = menuId == session.id,
                    onSelect = { onSelect(session.id) },
                    onOpenMenu = { onOpenMenu(session.id) },
                    onDismissMenu = onDismissMenu,
                    onRename = { onStartRename(session) },
                    onTogglePin = { onTogglePin(session.id) },
                    onDelete = { onStartDelete(session.id) },
                )
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    SessionsEmptyState(query = query, onCreate = onCreate, onClearQuery = { onQueryChange("") })
                }
            }
        }
    }
}

@Composable
private fun SessionSearchField(
    query: String,
    searchFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).focusRequester(searchFocus),
        insideMargin = DpSize(12.dp, 10.dp),
        cornerRadius = 16.dp,
        label = localizedString(Res.string.search_sessions),
        useLabelAsPlaceholder = true,
        singleLine = true,
        leadingIcon = {
            AppIcon(
                AppSymbol.Search,
                modifier = Modifier.padding(start = 12.dp).size(18.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                TextButton(
                    text = localizedString(Res.string.clear_search),
                    onClick = { onQueryChange("") },
                    colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                    insideMargin = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        },
        colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                borderColor = MiuixTheme.colorScheme.primary,
            ),
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
    )
}

@Composable
private fun SessionsEmptyState(
    query: String,
    onCreate: () -> Unit,
    onClearQuery: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = localizedString(if (query.isNotBlank()) Res.string.no_sessions else Res.string.empty_sessions),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        TextButton(
            text = localizedString(if (query.isBlank()) Res.string.new_session else Res.string.clear_search),
            onClick = { if (query.isBlank()) onCreate() else onClearQuery() },
            colors = ButtonDefaults.textButtonColorsPrimary(),
        )
    }
}
