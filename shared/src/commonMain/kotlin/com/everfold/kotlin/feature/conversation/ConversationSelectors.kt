package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.components.AppIcon
import com.everfold.kotlin.ui.components.AppSymbol
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
) {
    val choices = if (canSelect) options.workspaces(state.connectionId) else listOfNotNull(state.workspace)
    ConversationSelector(
        title = localizedString(Res.string.choose_workspace),
        label = state.workspace?.name?.resolve() ?: localizedString(Res.string.workspace_unavailable),
        choices = choices.map { SelectionChoice(it.id, it.name.resolve(), it.path, it.id == state.workspace?.id) },
        onSelect = { if (canSelect) options.selectWorkspace(state.id, it) },
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
        TextButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.semantics { contentDescription = title }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (symbol != null) AppIcon(symbol)
                Text(
                    label,
                    Modifier.widthIn(max = 180.dp).weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (showArrow) AppIcon(AppSymbol.ChevronDown)
            }
        }
        SelectionMenu(title, choices, expanded && enabled, { expanded = false }) {
            expanded = false
            onSelect(it)
        }
    }
}
