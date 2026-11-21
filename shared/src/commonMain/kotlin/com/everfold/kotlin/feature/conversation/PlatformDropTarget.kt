package com.everfold.kotlin.feature.conversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal fun appendDroppedDraftText(
    currentDraft: String,
    droppedText: String,
): String {
    val cleanDrop = droppedText.trim()
    if (cleanDrop.isEmpty()) return currentDraft
    val cleanCurrent = currentDraft.trimEnd()
    return if (cleanCurrent.isEmpty()) cleanDrop else "$cleanCurrent\n$cleanDrop"
}

@Composable
internal expect fun rememberPlatformTextDropTargetModifier(onDropText: (String) -> Unit): Modifier
