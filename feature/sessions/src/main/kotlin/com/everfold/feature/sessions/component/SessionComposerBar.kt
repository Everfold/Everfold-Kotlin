package com.everfold.feature.sessions.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.feature.sessions.R

@Composable
internal fun SessionComposerBar(
    draft: String,
    isRunning: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(stringResource(R.string.session_detail_composer_hint)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                modifier = Modifier.weight(1f),
            )
            if (isRunning) {
                FilledIconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Rounded.StopCircle,
                        contentDescription = stringResource(R.string.session_detail_stop),
                    )
                }
            } else {
                FilledIconButton(onClick = onSend, enabled = draft.isNotBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.session_detail_send),
                    )
                }
            }
        }
    }
}
