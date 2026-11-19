package com.everfold.kotlin.feature.connections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.everfold.kotlin.ui.localization.localizedString
import com.everfold.kotlin.ui.theme.LocalAppBackdrop
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.cancel
import everfold.shared.generated.resources.choose_file
import everfold.shared.generated.resources.error_certificate
import everfold.shared.generated.resources.hide_password
import everfold.shared.generated.resources.import_hint
import everfold.shared.generated.resources.import_preview
import everfold.shared.generated.resources.no_file
import everfold.shared.generated.resources.remove_file
import everfold.shared.generated.resources.replace_file
import everfold.shared.generated.resources.scan_file
import everfold.shared.generated.resources.scan_preview
import everfold.shared.generated.resources.show_password
import everfold.shared.generated.resources.use_sample
import org.jetbrains.compose.resources.StringResource
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.glass.GlassDialog
import top.yukonga.miuix.kmp.glass.GlassDialogButton
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 字段失焦或提交后才显示错误支持密码显隐标签与错误中文文案由调用处传入 */
@Composable
internal fun SetupField(
    label: StringResource,
    value: String,
    onChange: (String) -> Unit,
    invalid: Boolean,
    error: StringResource,
    showErrors: Boolean,
    secret: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    var focusedOnce by remember(label) { mutableStateOf(false) }
    var touched by remember(label) { mutableStateOf(false) }
    var reveal by remember(label) { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val showError = invalid && (showErrors || touched)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TextField(
            value = value,
            onValueChange = onChange,
            modifier =
                Modifier.fillMaxWidth().onFocusChanged {
                    if (it.isFocused) {
                        focusedOnce = true
                    } else if (focusedOnce) {
                        touched = true
                    }
                },
            label = localizedString(label),
            useLabelAsPlaceholder = false,
            singleLine = true,
            colors =
                TextFieldDefaults.textFieldColors(
                    backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    borderColor = if (showError) MiuixTheme.colorScheme.error else MiuixTheme.colorScheme.primary,
                ),
            cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = if (secret) KeyboardType.Password else keyboard,
                    imeAction = if (secret) ImeAction.Done else ImeAction.Next,
                ),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }, onDone = { focus.clearFocus() }),
            visualTransformation = if (secret && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                if (secret) {
                    TextButton(
                        text = localizedString(if (reveal) Res.string.hide_password else Res.string.show_password),
                        onClick = { reveal = !reveal },
                        colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.primary),
                        insideMargin = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
            },
        )
        if (showError) {
            Text(text = localizedString(error), style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.error)
        }
    }
}

/** 模拟证书导入与扫码只回传示例文件名不访问设备文件或相机 */
@Composable
internal fun FileImportField(
    label: StringResource,
    value: String?,
    sampleName: String,
    onImport: (String?) -> Unit,
    showErrors: Boolean,
) {
    var importing by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var touched by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = localizedString(label), style = MiuixTheme.textStyles.headline1)
            Text(
                text = value ?: localizedString(Res.string.no_file),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scanning = false.also { importing = true } }) {
                    Text(localizedString(if (value == null) Res.string.choose_file else Res.string.replace_file))
                }
                Button(onClick = { scanning = true.also { importing = true } }) {
                    Text(localizedString(Res.string.scan_file))
                }
                if (value != null) {
                    TextButton(
                        text = localizedString(Res.string.remove_file),
                        onClick = {
                            touched = true
                            onImport(null)
                        },
                        colors = ButtonDefaults.textButtonColors(color = Color.Transparent, textColor = MiuixTheme.colorScheme.error),
                        insideMargin = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            if ((showErrors || touched) && value.isNullOrBlank()) {
                Text(
                    text = localizedString(Res.string.error_certificate),
                    color = MiuixTheme.colorScheme.error,
                    style = MiuixTheme.textStyles.footnote1,
                )
            }
        }
    }
    FileImportDialog(
        show = importing,
        scanning = scanning,
        onDismiss = { importing = false },
        onConfirm = {
            onImport(sampleName)
            importing = false
        },
    )
}

@Composable
private fun FileImportDialog(
    show: Boolean,
    scanning: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    GlassDialog(
        show = show,
        onDismissRequest = onDismiss,
        backdrop = LocalAppBackdrop.current,
        title = localizedString(if (scanning) Res.string.scan_preview else Res.string.import_preview),
        summary = localizedString(Res.string.import_hint),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassDialogButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text(text = localizedString(Res.string.cancel), style = MiuixTheme.textStyles.button)
            }
            GlassDialogButton(onClick = onConfirm, modifier = Modifier.weight(1f), role = GlassDialogButtonRole.Primary) {
                Text(text = localizedString(Res.string.use_sample), style = MiuixTheme.textStyles.button)
            }
        }
    }
}

@Composable
internal fun SetupColumns(
    wide: Boolean,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        maxItemsInEachRow = if (wide) 2 else 1,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) { first() }
        Column(Modifier.weight(1f)) { second() }
    }
}
