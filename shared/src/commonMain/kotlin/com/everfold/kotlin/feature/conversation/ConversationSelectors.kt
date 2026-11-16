package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
import com.everfold.kotlin.ui.components.SelectionButton
import com.everfold.kotlin.ui.components.SelectionChoice
import com.everfold.kotlin.ui.components.SelectionMenu
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_model
import everfold.shared.generated.resources.choose_workspace
import everfold.shared.generated.resources.model_unavailable
import everfold.shared.generated.resources.workspace_unavailable

@Composable
internal fun WorkspaceSelector(
    state: ConversationUiState,
    options: ConversationOptions,
    canSelect: Boolean,
    modifier: Modifier = Modifier,
) {
    val choices = if (canSelect) options.workspaces(state.connectionId) else listOfNotNull(state.workspace)
    ConversationSelector(
        title = localizedString(Res.string.choose_workspace),
        label = state.workspace?.name?.resolve() ?: localizedString(Res.string.workspace_unavailable),
        choices = choices.map { SelectionChoice(it.id, it.name.resolve(), it.path, it.id == state.workspace?.id) },
        onSelect = { if (canSelect) options.selectWorkspace(state.id, it) },
        modifier = modifier,
        symbol = AppSymbol.Folder,
        showArrow = canSelect,
        enabled = choices.isNotEmpty(),
    )
}

@Composable
internal fun ModelSelector(
    state: ConversationUiState,
    options: ConversationOptions,
    modifier: Modifier = Modifier,
) {
    val models = options.models(state.connectionId)
    ConversationSelector(
        title = localizedString(Res.string.choose_model),
        label = state.model?.name?.resolve() ?: localizedString(Res.string.model_unavailable),
        choices = models.map { SelectionChoice(it.id, it.name.resolve(), selected = it.id == state.model?.id) },
        onSelect = { options.selectModel(state.id, it) },
        modifier = modifier,
        enabled = !state.isResponding && models.isNotEmpty(),
    )
}

@Composable
private fun ConversationSelector(
    title: String,
    label: String,
    choices: List<SelectionChoice>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    symbol: AppSymbol? = null,
    showArrow: Boolean = true,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectionButton(
            title = title,
            label = label,
            onClick = { expanded = true },
            enabled = enabled,
            showArrow = showArrow,
            leadingIcon = symbol?.let { { AppIcon(it, Modifier.size(18.dp)) } },
        )
        SelectionMenu(title, choices, expanded && enabled, { expanded = false }) {
            expanded = false
            onSelect(it)
        }
    }
}
