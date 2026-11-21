package com.everfold.kotlin.feature.conversation

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberPlatformTextDropTargetModifier(onDropText: (String) -> Unit): Modifier {
    val context = LocalContext.current
    val latestOnDrop by rememberUpdatedState(onDropText)
    val callback =
        remember(context) {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val clip = event.toAndroidDragEvent().clipData ?: return false
                    val extracted = extractClipText(context, clip)
                    if (extracted.isNotEmpty()) {
                        latestOnDrop(extracted)
                    }
                    return extracted.isNotEmpty()
                }
            }
        }
    return Modifier.dragAndDropTarget(
        shouldStartDragAndDrop = { event ->
            val types = event.mimeTypes()
            ClipDescription.MIMETYPE_TEXT_PLAIN in types || types.any { it.startsWith("text/") }
        },
        target = callback,
    )
}

private fun extractClipText(
    context: Context,
    clip: ClipData,
): String =
    buildList {
        for (index in 0 until clip.itemCount) {
            val text =
                clip
                    .getItemAt(index)
                    .coerceToText(context)
                    ?.toString()
                    ?.trim()
            if (!text.isNullOrEmpty()) add(text)
        }
    }.joinToString("\n")
