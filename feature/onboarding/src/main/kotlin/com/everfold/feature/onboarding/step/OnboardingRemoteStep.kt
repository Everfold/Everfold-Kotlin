package com.everfold.feature.onboarding.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.model.ConnectionErrorCode
import com.everfold.core.model.RemoteTransportConfig
import com.everfold.core.ui.connection.RemoteConnectionForm
import com.everfold.core.ui.connection.labelRes
import com.everfold.feature.onboarding.R

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OnboardingRemoteStep(
    profileName: String,
    profileNote: String,
    transport: RemoteTransportConfig,
    enabled: Boolean,
    errorCode: ConnectionErrorCode?,
    onProfileNameChange: (String) -> Unit,
    onProfileNoteChange: (String) -> Unit,
    onTransportChange: (RemoteTransportConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val errorText = errorCode?.let { stringResource(it.labelRes()) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_remote_title),
            style = MaterialTheme.typography.headlineSmallEmphasized,
        )
        Text(
            text = stringResource(R.string.onboarding_remote_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RemoteConnectionForm(
            transport = transport,
            onTransportChange = onTransportChange,
            enabled = enabled,
            showProfileFields = true,
            profileName = profileName,
            profileNote = profileNote,
            onProfileNameChange = onProfileNameChange,
            onProfileNoteChange = onProfileNoteChange,
            errorText = errorText,
        )
    }
}
