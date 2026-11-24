package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.designsystem.component.EverfoldGroupedItem
import com.everfold.core.model.ModelCapability
import com.everfold.core.model.ModelDescriptor
import com.everfold.core.model.ReasoningEffort
import com.everfold.feature.sessions.R
import kotlinx.collections.immutable.ImmutableList

private const val SELECTED_CONTAINER_ALPHA = 0.45f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SessionModelEffortSheet(
    models: ImmutableList<ModelDescriptor>,
    activeModel: ModelDescriptor?,
    activeEffort: ReasoningEffort?,
    onSelectModelAndEffort: (String, ReasoningEffort?) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.session_model_sheet_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
            )
            EffortTierSection(
                activeModel = activeModel,
                activeEffort = activeEffort,
                onSelectEffort = { effort ->
                    activeModel?.let { onSelectModelAndEffort(it.id, effort) }
                },
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(models, key = { _, item -> item.id }) { index, model ->
                    ModelOptionCard(
                        index = index,
                        count = models.size,
                        model = model,
                        selected = model.id == activeModel?.id,
                        onClick = { onSelectModelAndEffort(model.id, activeEffort) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.session_model_apply_next_turn),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EffortTierSection(
    activeModel: ModelDescriptor?,
    activeEffort: ReasoningEffort?,
    onSelectEffort: (ReasoningEffort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.session_model_effort_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (activeModel != null && activeModel.supportsEffort) {
            EverfoldConnectedButtonGroup(
                options = ReasoningEffort.entries,
                selected = activeEffort ?: ReasoningEffort.MEDIUM,
                onSelect = { effort -> if (effort in activeModel.supportedEfforts) onSelectEffort(effort) },
                isItemEnabled = { effort -> effort in activeModel.supportedEfforts },
                label = { effort -> stringResource(effort.labelRes()) },
            )
        } else {
            Text(
                text = stringResource(R.string.session_model_effort_unsupported),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModelOptionCard(
    index: Int,
    count: Int,
    model: ModelDescriptor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    EverfoldGroupedItem(
        index = index,
        count = count,
        containerColor =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = SELECTED_CONTAINER_ALPHA)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        selected = selected,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${model.provider} · ${model.contextWindowLabel} · ${model.hostNodeName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                ModelCapabilitiesRow(capabilities = model.capabilities)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelCapabilitiesRow(capabilities: Set<ModelCapability>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        capabilities.forEach { capability ->
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
                Text(
                    text = capability.shortTag(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
