package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_model
import everfold.shared.generated.resources.model_unavailable
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.glass.GlassBottomSheet
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.squircle.squircleClip
import top.yukonga.miuix.kmp.squircle.squircleSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ModelSelector(
    state: ConversationUiState,
    options: ConversationOptions,
    modifier: Modifier = Modifier,
) {
    val models = options.models(state.connectionId)
    val title = localizedString(Res.string.choose_model)
    val enabled = !state.isResponding && models.isNotEmpty()
    var expanded by remember(state.id) { mutableStateOf(false) }
    val label = state.model?.name?.resolve() ?: localizedString(Res.string.model_unavailable)
    Box(modifier, contentAlignment = Alignment.CenterStart) {
        ModelSelectorTrigger(
            title = title,
            label = label,
            enabled = enabled,
            onClick = { expanded = true },
        )
    }
    GlassBottomSheet(
        show = expanded && enabled,
        onDismissRequest = { expanded = false },
        backdrop = LocalAppBackdrop.current,
        title = title,
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 16.dp).verticalScroll(rememberScrollState())) {
            Card(modifier = Modifier.fillMaxWidth()) {
                models.forEachIndexed { index, model ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    RadioButtonPreference(
                        title = model.name.resolve(),
                        selected = model.id == state.model?.id,
                        onClick = {
                            expanded = false
                            options.selectModel(state.id, model.id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelSelectorTrigger(
    title: String,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.disabledOnSurface
    Surface(
        modifier =
            Modifier
                .widthIn(max = 260.dp)
                .squircleSurface(color = MiuixTheme.colorScheme.surfaceContainerHigh, cornerRadius = 14.dp)
                .squircleClip(14.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = title
                    role = Role.Button
                    if (!enabled) disabled()
                },
        color = Color.Transparent,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AppIcon(AppSymbol.ChevronDown, modifier = Modifier.size(16.dp), tint = contentColor)
        }
    }
}
