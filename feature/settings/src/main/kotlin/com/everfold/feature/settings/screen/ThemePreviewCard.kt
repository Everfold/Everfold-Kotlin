package com.everfold.feature.settings.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldStatusPill
import com.everfold.feature.settings.R

private const val CARD_CORNER_DP = 24
private const val BUBBLE_CORNER_DP = 14
private const val BUBBLE_TAIL_DP = 4
private const val OUTLINE_ALPHA = 0.5f

@Composable
internal fun ThemePreviewCard(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_DP.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = OUTLINE_ALPHA)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PreviewHeaderRow(scheme = scheme)
            PreviewUserBubble(scheme = scheme)
            PreviewAgentReply(scheme = scheme)
            PreviewComposerBar(scheme = scheme)
        }
    }
}

@Composable
private fun PreviewHeaderRow(scheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
        )
        Text(
            text = stringResource(R.string.appearance_preview_app_name),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurface,
        )
        Spacer(modifier = Modifier.weight(1f))
        EverfoldStatusPill(
            text = stringResource(R.string.appearance_preview_status_ready),
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun PreviewUserBubble(scheme: ColorScheme) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 260.dp),
            shape =
                RoundedCornerShape(
                    topStart = BUBBLE_CORNER_DP.dp,
                    topEnd = BUBBLE_CORNER_DP.dp,
                    bottomStart = BUBBLE_CORNER_DP.dp,
                    bottomEnd = BUBBLE_TAIL_DP.dp,
                ),
            color = scheme.primaryContainer,
            contentColor = scheme.onPrimaryContainer,
        ) {
            Text(
                text = stringResource(R.string.appearance_preview_user_prompt),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun PreviewAgentReply(scheme: ColorScheme) {
    Surface(
        modifier = Modifier.widthIn(max = 280.dp),
        shape =
            RoundedCornerShape(
                topStart = BUBBLE_TAIL_DP.dp,
                topEnd = BUBBLE_CORNER_DP.dp,
                bottomStart = BUBBLE_CORNER_DP.dp,
                bottomEnd = BUBBLE_CORNER_DP.dp,
            ),
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_preview_agent_reply),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = CircleShape,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = stringResource(R.string.appearance_preview_tool_badge),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewComposerBar(scheme: ColorScheme) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        color = scheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_preview_input_placeholder),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = scheme.primary,
                contentColor = scheme.onPrimary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}
