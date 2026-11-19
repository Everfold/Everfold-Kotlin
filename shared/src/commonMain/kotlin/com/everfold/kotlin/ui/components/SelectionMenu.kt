package com.everfold.kotlin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.selection_empty
import everfold.shared.generated.resources.selection_search
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val SEARCH_THRESHOLD = 8

/** 窄窗口使用玻璃底部面板，宽窗口在触发按钮旁展开；选项内容完全共用。 */
@Composable
internal fun SelectionMenu(
    title: String,
    choices: List<SelectionChoice>,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val compact =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.width
                .toDp() < 600.dp
        }
    var query by remember(title, expanded) { mutableStateOf("") }
    val searchable = choices.count { it.searchable && !it.readOnly } >= SEARCH_THRESHOLD
    val filtered = choices.filter { !it.searchable || it.label.contains(query, true) || it.detail.contains(query, true) }
    val body: @Composable () -> Unit = {
        if (searchable) {
            SelectionSearchField(query) { query = it }
        }
        if (filtered.none { it.searchable } && query.isNotEmpty()) {
            Text(
                text = localizedString(Res.string.selection_empty),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        SelectionItems(filtered, onSelect)
    }
    if (compact) {
        GlassBottomSheet(
            show = expanded,
            onDismissRequest = onDismiss,
            backdrop = LocalAppBackdrop.current,
            title = title,
            insideMargin = DpSize(12.dp, 12.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            ) {
                body()
            }
        }
    } else {
        OverlayListPopup(
            show = expanded,
            onDismissRequest = onDismiss,
        ) {
            ListPopupColumn {
                body()
            }
        }
    }
}

@Composable
private fun SelectionSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        insideMargin = DpSize(12.dp, 10.dp),
        label = localizedString(Res.string.selection_search),
        useLabelAsPlaceholder = true,
        singleLine = true,
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
private fun SelectionItems(
    choices: List<SelectionChoice>,
    onSelect: (String) -> Unit,
) {
    choices.forEach { choice ->
        if (choice.readOnly) {
            SelectionInformation(choice)
        } else {
            val containerColor =
                if (choice.selected) {
                    MiuixTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                }
            val titleColor =
                when {
                    !choice.enabled -> MiuixTheme.colorScheme.disabledOnSecondaryVariant
                    choice.selected -> MiuixTheme.colorScheme.primary
                    else -> MiuixTheme.colorScheme.onSurfaceContainer
                }
            val summaryColor =
                if (choice.enabled) {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                } else {
                    MiuixTheme.colorScheme.disabledOnSecondaryVariant
                }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(containerColor)
                        .clickable(enabled = choice.enabled) { onSelect(choice.id) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = choice.label,
                        style = MiuixTheme.textStyles.body1,
                        fontWeight = if (choice.selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = titleColor,
                    )
                    if (choice.detail.isNotEmpty()) {
                        Text(
                            text = choice.detail,
                            style = MiuixTheme.textStyles.body2,
                            color = summaryColor,
                        )
                    }
                }
                if (choice.selected) {
                    AppIcon(
                        AppSymbol.Check,
                        modifier = Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionInformation(choice: SelectionChoice) {
    SelectionContainer(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = choice.label,
                style = MiuixTheme.textStyles.body1,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurfaceContainer,
            )
            if (choice.detail.isNotEmpty()) {
                Text(
                    text = choice.detail,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
