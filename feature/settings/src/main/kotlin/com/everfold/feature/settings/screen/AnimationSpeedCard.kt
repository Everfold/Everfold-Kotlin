package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.settings.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AnimationSpeedCard(
    speed: AnimationSpeed,
    onSpeedChange: (AnimationSpeed) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_animation_speed_label),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimationSpeed.entries.forEach { option ->
                    FilterChip(
                        selected = speed == option,
                        onClick = { onSpeedChange(option) },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.appearance_animation_speed_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
internal fun AnimationSpeed.labelRes(): Int =
    when (this) {
        AnimationSpeed.DEFAULT -> R.string.animation_speed_default
        AnimationSpeed.FAST -> R.string.animation_speed_fast
        AnimationSpeed.SLOW -> R.string.animation_speed_slow
        AnimationSpeed.OFF -> R.string.animation_speed_off
    }
