package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.model.rememberMarkdownState
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.code_block
import everfold.shared.generated.resources.collapse_prompt
import everfold.shared.generated.resources.copied
import everfold.shared.generated.resources.copy_code
import everfold.shared.generated.resources.expand_prompt
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
    val style = MiuixTheme.textStyles.body1
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val measured = measurer.measure(message.text, style = style, maxLines = 9, constraints = constraints)
        val collapsible = collapsePrompts && measured.lineCount > 8
        Column(horizontalAlignment = Alignment.End) {
            SelectionContainer {
                Text(
                    text = message.text,
                    style = style,
                    maxLines = if (collapsible && !expanded) 6 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layout = it },
                )
            }
            if (collapsible) {
                TextButton(
                    text = localizedString(if (expanded) Res.string.collapse_prompt else Res.string.expand_prompt),
                    onClick = {
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
                    },
                    colors =
                        ButtonDefaults.textButtonColors(
                            color = Color.Transparent,
                            textColor = MiuixTheme.colorScheme.primary,
                        ),
                    insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 渲染 Markdown 并定制代码块解析中或失败时退回原文显示 */
@Composable
internal fun MessageMarkdown(content: String) {
    val state = rememberMarkdownState(content, retainState = true)
    Markdown(
        markdownState = state,
        colors = miuixMarkdownColors(),
        typography = miuixMarkdownTypography(),
        modifier = Modifier.fillMaxWidth(),
        components =
            markdownComponents(
                codeFence = { block -> MarkdownCodeFence(block.content, block.node) { code, language, _ -> CodeSnippet(code, language) } },
                codeBlock = { block -> MarkdownCodeBlock(block.content, block.node) { code, language, _ -> CodeSnippet(code, language) } },
            ),
        loading = { Text(text = content, style = MiuixTheme.textStyles.body1) },
        error = { Text(text = content, style = MiuixTheme.textStyles.body1) },
    )
}

@Composable
private fun miuixMarkdownColors(): MarkdownColors {
    val colors = MiuixTheme.colorScheme
    return remember(colors) {
        DefaultMarkdownColors(
            text = colors.onSurface,
            codeBackground = colors.surfaceContainer,
            inlineCodeBackground = colors.surfaceContainerHigh,
            dividerColor = colors.dividerLine,
            tableBackground = colors.surfaceContainer,
            highlightText = colors.onPrimaryContainer,
            highlightBackground = colors.primaryContainer,
        )
    }
}

@Composable
private fun miuixMarkdownTypography(): MarkdownTypography {
    val styles = MiuixTheme.textStyles
    val linkColor = MiuixTheme.colorScheme.primary
    return remember(styles, linkColor) {
        DefaultMarkdownTypography(
            h1 = styles.title1,
            h2 = styles.title2,
            h3 = styles.title3,
            h4 = styles.title4,
            h5 = styles.headline1,
            h6 = styles.headline2,
            text = styles.body1,
            code = styles.body2.copy(fontFamily = FontFamily.Monospace),
            inlineCode = styles.body2.copy(fontFamily = FontFamily.Monospace),
            quote = styles.body1,
            paragraph = styles.body1,
            ordered = styles.body1,
            bullet = styles.body1,
            list = styles.body1,
            textLink = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
            table = styles.body2,
        )
    }
}

@Composable
private fun CodeSnippet(
    code: String,
    language: String?,
) {
    Surface(
        modifier = Modifier.squircleSurface(color = MiuixTheme.colorScheme.surfaceContainer, cornerRadius = 14.dp),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // 代码
                    text = language?.takeIf { it.isNotBlank() } ?: localizedString(Res.string.code_block),
                    modifier = Modifier.weight(1f),
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )

                // 复制代码
                CopyTextButton(code, localizedString(Res.string.copy_code))
            }
            Text(
                text = code,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                fontFamily = FontFamily.Monospace,
                style = MiuixTheme.textStyles.body2,
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
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    TextButton(
        text = if (copied) localizedString(Res.string.copied) else label,
        onClick = {
            clipboard.setText(AnnotatedString(text))
            copied = true
        },
        enabled = text.isNotEmpty(),
        colors =
            ButtonDefaults.textButtonColors(
                color = Color.Transparent,
                textColor = MiuixTheme.colorScheme.primary,
            ),
        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    )
}
