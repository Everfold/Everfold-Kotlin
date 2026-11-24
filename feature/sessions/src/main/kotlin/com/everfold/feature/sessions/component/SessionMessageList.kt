package com.everfold.feature.sessions.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpec
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.core.model.AgentSessionDetail
import com.everfold.core.model.AnimationSpeed
import com.everfold.core.model.LoopState
import com.everfold.core.model.ObservedItem
import com.everfold.feature.sessions.R
import com.everfold.feature.sessions.SessionDetailUiState

private const val BOTTOM_ANCHOR_KEY = "__session_bottom_anchor__"
private const val FAB_INITIAL_SCALE = 0.85f
private const val TELEPORT_ITEM_THRESHOLD = 8
private const val MAX_SCROLL_CONVERGE_PASSES = 8
private const val LARGE_SCROLL_OFFSET_PX = 1_000_000

@Composable
internal fun SessionMessageList(
    detail: AgentSessionDetail,
    state: SessionDetailUiState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val animationSpeed = LocalAnimationSpeed.current
    val showScrollToBottom by remember { derivedStateOf { listState.canScrollForward } }
    var stickToBottom by remember(detail.summary.id) { mutableStateOf(true) }
    var scrollToBottomNonce by remember(detail.summary.id) { mutableIntStateOf(0) }
    val currentDensity = LocalDensity.current
    val scaledDensity = Density(currentDensity.density, currentDensity.fontScale * state.chatTextSize.scale)

    SessionMessageScrollEffects(
        detail = detail,
        listState = listState,
        stickToBottom = stickToBottom,
        scrollToBottomNonce = scrollToBottomNonce,
        animationSpeed = animationSpeed,
        onStickToBottomChange = { stickToBottom = it },
        onRequestAnimatedScroll = { scrollToBottomNonce++ },
    )

    Box(modifier = modifier) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(detail.items, key = { it.id }) { item ->
                    ObservedItemRow(
                        item = item,
                        isPaused = detail.summary.loopState == LoopState.PAUSED,
                        autoCollapseLongMessages = state.autoCollapseLongMessages,
                        expandRunningActionTraces = state.expandRunningActionTraces,
                        expandCompletedActionTraces = state.expandCompletedActionTraces,
                    )
                }
                item(key = BOTTOM_ANCHOR_KEY) { Spacer(modifier = Modifier.height(1.dp)) }
            }
        }
        ScrollToLatestButton(
            visible = showScrollToBottom,
            animationSpeed = animationSpeed,
            onClick = { stickToBottom = true.also { scrollToBottomNonce++ } },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun SessionMessageScrollEffects(
    detail: AgentSessionDetail,
    listState: LazyListState,
    stickToBottom: Boolean,
    scrollToBottomNonce: Int,
    animationSpeed: AnimationSpeed,
    onStickToBottomChange: (Boolean) -> Unit,
    onRequestAnimatedScroll: () -> Unit,
) {
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val latestIsDragged by rememberUpdatedState(isDragged)
    val latestOnStickChange by rememberUpdatedState(onStickToBottomChange)
    val latestRequestScroll by rememberUpdatedState(onRequestAnimatedScroll)
    val lastItem = detail.items.lastOrNull()
    val streamingLen = (lastItem as? ObservedItem.AssistantMessage)?.takeIf { it.isStreaming }?.markdown?.length
    val bottomSignature by remember {
        derivedStateOf {
            listState.layoutInfo.totalItemsCount to
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.let { it.index to (it.offset + it.size) }
        }
    }

    LaunchedEffect(isDragged, listState.isScrollInProgress, listState.canScrollForward) {
        if (isDragged) {
            latestOnStickChange(false)
        } else if (!listState.isScrollInProgress && !listState.canScrollForward) {
            latestOnStickChange(true)
        }
    }
    LaunchedEffect(detail.summary.id) {
        if (detail.items.isNotEmpty()) {
            listState.scrollToAbsoluteBottom(animate = false) { latestIsDragged }
        }
    }
    LaunchedEffect(scrollToBottomNonce) {
        if (scrollToBottomNonce > 0 && detail.items.isNotEmpty()) {
            listState.scrollToAbsoluteBottom(animate = animationSpeed != AnimationSpeed.OFF) { latestIsDragged }
        }
    }
    LaunchedEffect(detail.items.size, lastItem?.id) {
        if (detail.items.isNotEmpty() && lastItem is ObservedItem.UserMessage) {
            latestOnStickChange(true)
            latestRequestScroll()
        }
    }
    val needsSettle = stickToBottom && listState.canScrollForward
    val isUserIdle = !isDragged && !listState.isScrollInProgress
    LaunchedEffect(stickToBottom, listState.canScrollForward, bottomSignature, streamingLen) {
        if (needsSettle && isUserIdle) {
            listState.scrollToAbsoluteBottom(animate = false) { latestIsDragged }
        }
    }
}

@Composable
private fun ScrollToLatestButton(
    visible: Boolean,
    animationSpeed: AnimationSpeed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spatialSpec = animationSpeed.spatialSpec<Float>()
    val effectsSpec = animationSpeed.effectsSpec<Float>()
    val enter =
        if (animationSpeed == AnimationSpeed.OFF) {
            EnterTransition.None
        } else {
            fadeIn(effectsSpec) + scaleIn(spatialSpec, initialScale = FAB_INITIAL_SCALE)
        }
    val exit =
        if (animationSpeed == AnimationSpeed.OFF) {
            ExitTransition.None
        } else {
            fadeOut(effectsSpec) + scaleOut(spatialSpec, targetScale = FAB_INITIAL_SCALE)
        }
    AnimatedVisibility(visible = visible, enter = enter, exit = exit, modifier = modifier) {
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.session_detail_scroll_to_latest),
            )
        }
    }
}

private suspend fun LazyListState.scrollToAbsoluteBottom(
    animate: Boolean,
    isUserDragging: () -> Boolean,
) {
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount <= 0 || isUserDragging()) return

    val lastIndex = totalCount - 1
    val lastVisibleIndex =
        layoutInfo.visibleItemsInfo
            .lastOrNull()
            ?.index
            ?: 0
    if (animate && lastIndex - lastVisibleIndex > TELEPORT_ITEM_THRESHOLD) {
        scrollToItem((lastIndex - TELEPORT_ITEM_THRESHOLD).coerceAtLeast(0))
    }

    var pass = 0
    while (canScrollForward && !isUserDragging() && pass < MAX_SCROLL_CONVERGE_PASSES) {
        pass++
        runSingleScrollPass(animate = animate)
        withFrameNanos { }
    }
}

private suspend fun LazyListState.runSingleScrollPass(animate: Boolean) {
    val totalCount = layoutInfo.totalItemsCount
    if (totalCount <= 0) return
    val lastIndex = totalCount - 1
    val remaining = remainingBottomPx(lastIndex)
    if (animate) {
        if (remaining != null && remaining > 0f) {
            animateScrollBy(remaining)
        } else {
            animateScrollToItem(lastIndex, LARGE_SCROLL_OFFSET_PX)
        }
    } else {
        if (remaining != null && remaining > 0f) {
            scrollBy(remaining)
        } else {
            scrollToItem(lastIndex, LARGE_SCROLL_OFFSET_PX)
        }
    }
}

private fun LazyListState.remainingBottomPx(lastIndex: Int): Float? {
    val lastVisible = layoutInfo.visibleItemsInfo.firstOrNull { it.index == lastIndex } ?: return null
    val viewportBottom = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    return (lastVisible.offset + lastVisible.size - viewportBottom).toFloat()
}
