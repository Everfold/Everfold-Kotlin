package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.feature.settings.R

@Composable
internal fun ThemePreviewCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_preview_title),
                style = MaterialTheme.typography.titleSmall,
            )
            PreviewCardSample(scheme = scheme)
            PreviewToneStrip(scheme = scheme)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreviewCardSample(scheme: ColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_preview_sample_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Text(
                text = stringResource(R.string.appearance_preview_sample_body),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PreviewRoleBadge(
                    label = stringResource(R.string.appearance_preview_primary),
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary,
                )
                PreviewRoleBadge(
                    label = stringResource(R.string.appearance_preview_secondary),
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                )
                PreviewRoleBadge(
                    label = stringResource(R.string.appearance_preview_tertiary),
                    containerColor = scheme.tertiaryContainer,
                    contentColor = scheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun PreviewToneStrip(scheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PreviewToneSwatch(
            label = stringResource(R.string.appearance_preview_primary),
            containerColor = scheme.primary,
            contentColor = scheme.onPrimary,
            modifier = Modifier.weight(1f),
        )
        PreviewToneSwatch(
            label = stringResource(R.string.appearance_preview_secondary),
            containerColor = scheme.secondary,
            contentColor = scheme.onSecondary,
            modifier = Modifier.weight(1f),
        )
        PreviewToneSwatch(
            label = stringResource(R.string.appearance_preview_tertiary),
            containerColor = scheme.tertiary,
            contentColor = scheme.onTertiary,
            modifier = Modifier.weight(1f),
        )
        PreviewToneSwatch(
            label = stringResource(R.string.appearance_preview_container),
            containerColor = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PreviewRoleBadge(
    label: String,
    containerColor: Color,
    contentColor: Color,
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PreviewToneSwatch(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
