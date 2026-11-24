package com.everfold.core.ui.connection

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.everfold.core.model.CertificatePayload
import com.everfold.core.model.CertificateSource
import com.everfold.core.ui.R

private const val MAX_PREVIEW_CHARS = 120

@Composable
fun CertificateSlotCard(
    title: String,
    payload: CertificatePayload?,
    enabled: Boolean,
    onSelectPayload: (CertificatePayload?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val documentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                onSelectPayload(readCertificateFromUri(context = context, uri = uri))
            }
        }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (payload != null) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SlotHeaderRow(
                title = title,
                payload = payload,
                enabled = enabled,
                onClear = { onSelectPayload(null) },
            )
            SlotActionButtonsRow(
                enabled = enabled,
                onChooseFile = { documentLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SlotActionButtonsRow(
    enabled: Boolean,
    onChooseFile: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onChooseFile,
            shapes = ButtonDefaults.shapes(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Rounded.UploadFile,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.conn_cert_choose_file),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        OutlinedButton(
            onClick = {},
            shapes = ButtonDefaults.shapes(),
            enabled = enabled,
            modifier = Modifier.weight(1f),
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.conn_cert_scan_qr),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SlotHeaderRow(
    title: String,
    payload: CertificatePayload?,
    enabled: Boolean,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (payload != null) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = payload?.displayName ?: stringResource(R.string.conn_cert_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (payload != null && payload.contentPreview.isNotBlank()) {
                Text(
                    text = payload.contentPreview,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (payload != null) {
            IconButton(onClick = onClear, shapes = IconButtonDefaults.shapes(), enabled = enabled) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.conn_cert_clear),
                )
            }
        }
    }
}

private fun readCertificateFromUri(
    context: Context,
    uri: Uri,
): CertificatePayload {
    val fileName =
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        }.getOrNull() ?: uri.lastPathSegment.orEmpty().ifBlank { "certificate.pem" }

    val preview =
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                reader
                    .readText()
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(MAX_PREVIEW_CHARS)
            }
        }.getOrNull().orEmpty()

    return CertificatePayload(
        displayName = fileName,
        contentPreview = preview,
        source = CertificateSource.FILE,
    )
}
