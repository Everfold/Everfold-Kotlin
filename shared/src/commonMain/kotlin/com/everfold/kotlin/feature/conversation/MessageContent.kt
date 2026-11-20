package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.plainTextClipEntry
import com.everfold.kotlin.ui.localization.localizedString
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.rememberMarkdownState
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.code_block
import everfold.shared.generated.resources.collapse_prompt
import everfold.shared.generated.resources.copied
import everfold.shared.generated.resources.copy_code
import everfold.shared.generated.resources.expand_prompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 长用户消息超过八行时可折叠为六行切换前回报高度差以维持列表位置 */
@Composable
internal fun UserPrompt(
    message: ChatMessage,
    onHeightChange: (Int) -> Unit,
    collapsePrompts: Boolean = true,
) {
    var expanded by rememberSaveable(message.id, collapsePrompts) { mutableStateOf(false) }
    var layout by remember(message.id) { mutableStateOf<TextLayoutResult?>(null) }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.bodyLarge
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val collapsible =
            remember(message.text, style, constraints, collapsePrompts, measurer, density) {
                collapsePrompts && measurer.measure(message.text, style = style, maxLines = 9, constraints = constraints).lineCount > 8
            }
        Column(horizontalAlignment = Alignment.End) {
            SelectionContainer {
                Text(
                    message.text,
                    style = style,
                    maxLines = if (collapsible && !expanded) 6 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout = it },
                )
            }
            if (collapsible) {
                TextButton(onClick = {
                    layout?.let { current ->
                        val next =
                            measurer.measure(
                                message.text,
                                style = style,
                                maxLines = if (expanded) 6 else Int.MAX_VALUE,
                                overflow = TextOverflow.Ellipsis,
                                constraints = current.layoutInput.constraints,
                            )
                        onHeightChange(next.size.height - current.size.height)
                    }
                    expanded = !expanded
                }) {
                    // 收起 / 展开全文
                    Text(localizedString(if (expanded) Res.string.collapse_prompt else Res.string.expand_prompt))
                }
            }
        }
    }
}

/** 渲染 Markdown 并定制代码块解析中或失败时退回原文显示 */
@Composable
internal fun MessageMarkdown(content: String) {
    val state = rememberMarkdownState(content, retainState = true)
    val components =
        remember {
            markdownComponents(
                codeFence = { block -> MarkdownCodeFence(block.content, block.node) { code, language, _ -> CodeSnippet(code, language) } },
                codeBlock = { block -> MarkdownCodeBlock(block.content, block.node) { code, language, _ -> CodeSnippet(code, language) } },
            )
        }
    Markdown(
        markdownState = state,
        modifier = Modifier.fillMaxWidth(),
        components = components,
        loading = { Text(content, style = MaterialTheme.typography.bodyLarge) },
        error = { Text(content, style = MaterialTheme.typography.bodyLarge) },
    )
}

@Composable
private fun CodeSnippet(
    code: String,
    language: String?,
) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // 代码
                    language?.takeIf { it.isNotBlank() } ?: localizedString(Res.string.code_block),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                )

                // 复制代码
                CopyTextButton(code, localizedString(Res.string.copy_code))
            }
            Text(
                code,
                Modifier.horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** 复制非空文本到剪贴板并短暂显示已复制反馈 */
@Composable
internal fun CopyTextButton(
    text: String,
    label: String,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    TextButton(onClick = {
        scope.launch {
            clipboard.setClipEntry(plainTextClipEntry(text))
            copied = true
        }
    }, enabled = text.isNotEmpty()) {
        // 已复制
        Text(if (copied) localizedString(Res.string.copied) else label)
    }
}
