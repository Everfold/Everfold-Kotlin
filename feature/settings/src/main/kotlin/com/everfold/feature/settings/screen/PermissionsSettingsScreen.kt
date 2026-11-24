package com.everfold.feature.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.everfold.core.ui.system.SystemPermissionsCard
import com.everfold.feature.settings.R

@Composable
fun PermissionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsSubPageScaffold(
        title = stringResource(R.string.settings_entry_permissions_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SystemPermissionsCard() }
        }
    }
}
