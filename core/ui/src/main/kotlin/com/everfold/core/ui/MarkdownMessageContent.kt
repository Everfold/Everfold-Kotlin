package com.everfold.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor

@Composable
fun MarkdownMessageContent(
    content: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = content,
        colors =
            markdownColor(
                text = MaterialTheme.colorScheme.onSurface,
                codeBackground = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        modifier = modifier,
    )
}
