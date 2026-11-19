package com.everfold.kotlin.feature.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.delete_session
import everfold.shared.generated.resources.delete_session_notice
import everfold.shared.generated.resources.pin_session
import everfold.shared.generated.resources.rename_session
import everfold.shared.generated.resources.save_session
import everfold.shared.generated.resources.session_name
import everfold.shared.generated.resources.unpin_session
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SessionActionsPopup(
    expanded: Boolean,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    OverlayListPopup(show = expanded, onDismissRequest = onDismiss) {
        ListPopupColumn {
            SessionPopupActionRow(
                label = localizedString(Res.string.rename_session),
                icon = AppSymbol.Edit,
                onClick = onRename,
            )
            SessionPopupActionRow(
                label = localizedString(if (isPinned) Res.string.unpin_session else Res.string.pin_session),
                icon = if (isPinned) AppSymbol.Unpin else AppSymbol.Pin,
                onClick = onTogglePin,
            )
            SessionPopupActionRow(
                label = localizedString(Res.string.delete_session),
                icon = AppSymbol.Delete,
                color = MiuixTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SessionPopupActionRow(
    label: String,
    icon: AppSymbol,
    color: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(icon, modifier = Modifier.size(18.dp), tint = color)
        Text(
            text = label,
            style = MiuixTheme.textStyles.body1,
            fontWeight = FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
internal fun SessionDialogs(
    renamingId: String?,
    deletingId: String?,
    renameInput: String,
    onRenameInputChange: (String) -> Unit,
    onDismissRename: () -> Unit,
    onConfirmRename: (String, String) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (String) -> Unit,
) {
    val backdrop = LocalAppBackdrop.current
    GlassDialog(
        show = renamingId != null,
        onDismissRequest = onDismissRename,
        backdrop = backdrop,
        title = localizedString(Res.string.rename_session),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextField(
                value = renameInput,
                onValueChange = onRenameInputChange,
                label = localizedString(Res.string.session_name),
                useLabelAsPlaceholder = false,
                singleLine = true,
                colors =
                    TextFieldDefaults.textFieldColors(
                        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                        labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        borderColor = MiuixTheme.colorScheme.primary,
                    ),
                cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassDialogButton(onClick = onDismissRename, modifier = Modifier.weight(1f)) {
                    Text(text = localizedString(Res.string.cancel), style = MiuixTheme.textStyles.button)
                }
                GlassDialogButton(
                    onClick = { renamingId?.let { onConfirmRename(it, renameInput) } },
                    modifier = Modifier.weight(1f),
                    enabled = renameInput.isNotBlank(),
                    role = GlassDialogButtonRole.Primary,
                ) {
                    Text(text = localizedString(Res.string.save_session), style = MiuixTheme.textStyles.button)
                }
            }
        }
    }
    GlassDialog(
        show = deletingId != null,
        onDismissRequest = onDismissDelete,
        backdrop = backdrop,
        title = localizedString(Res.string.delete_session),
        summary = localizedString(Res.string.delete_session_notice),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassDialogButton(onClick = onDismissDelete, modifier = Modifier.weight(1f)) {
                Text(text = localizedString(Res.string.cancel), style = MiuixTheme.textStyles.button)
            }
            GlassDialogButton(
                onClick = { deletingId?.let(onConfirmDelete) },
                modifier = Modifier.weight(1f),
                role = GlassDialogButtonRole.Destructive,
            ) {
                Text(text = localizedString(Res.string.delete_session), style = MiuixTheme.textStyles.button)
            }
        }
    }
}
