package com.everfold.feature.settings.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldSectionHeader
import com.everfold.core.model.AnimationSpeed
import com.everfold.feature.settings.R

@Composable
internal fun AnimationSpeedCard(
    speed: AnimationSpeed,
    onSpeedChange: (AnimationSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = AnimationSpeed.entries
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EverfoldSectionHeader(title = stringResource(R.string.appearance_animation_speed_label))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = speed == option,
                    onClick = { onSpeedChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = {
                        Text(
                            text = stringResource(option.labelRes()),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
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

@StringRes
internal fun AnimationSpeed.labelRes(): Int =
    when (this) {
        AnimationSpeed.DEFAULT -> R.string.animation_speed_default
        AnimationSpeed.FAST -> R.string.animation_speed_fast
        AnimationSpeed.SLOW -> R.string.animation_speed_slow
        AnimationSpeed.OFF -> R.string.animation_speed_off
    }
