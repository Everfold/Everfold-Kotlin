package com.everfold.feature.sessions.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddComment
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.core.model.AgentSessionSummary
import com.everfold.core.model.LoopState
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.SessionDetailUiState
import com.everfold.feature.sessions.labelRes
import com.everfold.core.ui.R as CoreUiR

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SessionDetailTopBar(
    state: SessionDetailUiState,
    onOpenDrawer: () -> Unit,
    onContextClick: () -> Unit,
    onNewSession: () -> Unit,
    onOpenMenu: (AgentSessionSummary) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val detail = state.detail
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer, shapes = IconButtonDefaults.shapes()) {
                Icon(
                    imageVector = Icons.Rounded.Menu,
                    contentDescription = stringResource(CoreUiR.string.common_open_menu),
                )
            }
        },
        title = {
            SessionTopBarTitleBlock(state = state, onContextClick = onContextClick)
        },
        actions = {
            if (detail != null) {
                SessionTopBarActions(
                    summary = detail.summary,
                    onNewSession = onNewSession,
                    onOpenMenu = onOpenMenu,
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionTopBarTitleBlock(
    state: SessionDetailUiState,
    onContextClick: () -> Unit,
) {
    val detail = state.detail
    val isDisconnectedNew = detail == null && !state.hasConnectedRelay
    val subtitleText =
        if (isDisconnectedNew) {
            stringResource(R.string.session_context_disconnected_action)
        } else {
            state.activeContext?.compactSubtitle().orEmpty()
        }
    Column {
        Text(
            text = detail?.summary?.title ?: stringResource(R.string.session_detail_new_title),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMediumEmphasized,
        )
        if (subtitleText.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clip(MaterialTheme.shapes.extraSmall).clickable(onClick = onContextClick),
            ) {
                Text(
                    text = subtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (isDisconnectedNew) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                if (!isDisconnectedNew) {
                    Icon(
                        imageVector =
                            if (detail == null) {
                                Icons.Rounded.KeyboardArrowDown
                            } else {
                                Icons.AutoMirrored.Rounded.KeyboardArrowRight
                            },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.SessionTopBarActions(
    summary: AgentSessionSummary,
    onNewSession: () -> Unit,
    onOpenMenu: (AgentSessionSummary) -> Unit,
) {
    if (summary.isArchived) {
        EverfoldStatusPill(
            text = stringResource(R.string.session_archived_badge),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        SessionLoopStatusPill(loopState = summary.loopState)
        IconButton(onClick = onNewSession, shapes = IconButtonDefaults.shapes()) {
            Icon(
                imageVector = Icons.Rounded.AddComment,
                contentDescription = stringResource(CoreUiR.string.common_new_session),
            )
        }
    }
    IconButton(onClick = { onOpenMenu(summary) }, shapes = IconButtonDefaults.shapes()) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.session_menu_more_actions),
        )
    }
}

@Composable
private fun SessionLoopStatusPill(loopState: LoopState) {
    val showPill = loopState != LoopState.IDLE && loopState != LoopState.COMPLETED
    if (!showPill) return
    val (bg, fg) =
        when (loopState) {
            LoopState.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            LoopState.FAILED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        }
    EverfoldStatusPill(
        text = stringResource(loopState.labelRes()),
        containerColor = bg,
        contentColor = fg,
    )
}
