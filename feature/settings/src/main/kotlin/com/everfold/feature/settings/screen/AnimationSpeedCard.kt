package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldConnectedButtonGroup
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.settings.R

@Composable
internal fun AnimationSpeedCard(
    speed: AnimationSpeed,
    onSpeedChange: (AnimationSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_animation_speed_label))
        EverfoldConnectedButtonGroup(
            options = AnimationSpeed.entries,
            selected = speed,
            onSelect = onSpeedChange,
            label = { option -> stringResource(option.labelRes()) },
        )
        Text(
            text = stringResource(R.string.appearance_animation_speed_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
