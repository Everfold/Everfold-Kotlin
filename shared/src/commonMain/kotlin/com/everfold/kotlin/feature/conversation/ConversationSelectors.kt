package com.everfold.kotlin.feature.conversation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.everfold.kotlin.ui.components.SelectionButton
import com.everfold.kotlin.ui.components.SelectionChoice
import com.everfold.kotlin.ui.components.SelectionMenu
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.localization.resolve
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.choose_model
import everfold.shared.generated.resources.model_unavailable

@Composable
internal fun ModelSelector(
    state: ConversationUiState,
    options: ConversationOptions,
    modifier: Modifier = Modifier,
) {
    val models = options.models(state.connectionId)
    val title = localizedString(Res.string.choose_model)
    val enabled = !state.isResponding && models.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        SelectionButton(
            title = title,
            label = state.model?.name?.resolve() ?: localizedString(Res.string.model_unavailable),
            onClick = { expanded = true },
            enabled = enabled,
        )
        SelectionMenu(
            title,
            models.map { SelectionChoice(it.id, it.name.resolve(), selected = it.id == state.model?.id) },
            expanded && enabled,
            { expanded = false },
        ) {
            expanded = false
            options.selectModel(state.id, it)
        }
    }
}
