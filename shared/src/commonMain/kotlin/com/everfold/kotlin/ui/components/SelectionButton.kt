package com.everfold.kotlin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 选择器共用字号、图标和内边距，宽度不足时仅省略文字。 */
@Composable
internal fun SelectionButton(
    title: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showArrow: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val contentColor =
        if (enabled) {
            MiuixTheme.colorScheme.onSurface
        } else {
            MiuixTheme.colorScheme.disabledOnSurface
        }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .widthIn(max = 280.dp)
                .semantics {
                    contentDescription = title
                    role = Role.Button
                    if (!enabled) disabled()
                },
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showArrow) AppIcon(AppSymbol.ChevronDown, Modifier.size(18.dp), tint = contentColor)
        }
    }
}
