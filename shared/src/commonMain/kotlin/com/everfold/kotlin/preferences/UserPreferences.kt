package com.everfold.kotlin.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.everfold.kotlin.feature.settings.ConversationTextSize
import com.everfold.kotlin.feature.settings.SendShortcut
import com.everfold.kotlin.ui.localization.AppLanguage
import com.everfold.kotlin.ui.theme.AppearancePreferences
import com.everfold.kotlin.ui.theme.ThemeColorMode
import com.everfold.kotlin.ui.theme.ThemeColorSelection
import com.everfold.kotlin.ui.theme.ThemeMode
import com.everfold.kotlin.ui.theme.ThemePreset

/** 只保存应用偏好，连接凭据与会话内容由各自的数据层负责。 */
internal interface PreferenceStore {
    fun read(): String?

    fun write(value: String)
}

@Composable
internal expect fun rememberPreferenceStore(): PreferenceStore

internal class MemoryPreferenceStore : PreferenceStore {
    private var saved: String? = null

    override fun read(): String? = saved

    override fun write(value: String) {
        saved = value
    }
}

internal class UserPreferencesState(
    private val store: PreferenceStore = MemoryPreferenceStore(),
) {
    var value by mutableStateOf(UserPreferences.decode(store.read()))
        private set

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(value)
        if (next == value) return
        store.write(next.encode())
        value = next
    }
}

internal data class UserPreferences(
    val language: AppLanguage = AppLanguage.System,
    val textSize: ConversationTextSize = ConversationTextSize.System,
    val shortcut: SendShortcut = SendShortcut.ModifiedEnter,
    val collapsePrompts: Boolean = true,
    val sideMargins: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val themeColor: ThemeColorSelection = ThemeColorSelection(),
) {
    fun restoreAppearance(target: AppearancePreferences) {
        target.setTheme(themeMode)
        target.selectThemePreset(themeColor.preset)
        target.applyCustomThemeColor(themeColor.customSeed)
        when (themeColor.mode) {
            ThemeColorMode.System -> target.useSystemThemeColor()
            ThemeColorMode.Preset -> target.selectThemePreset(themeColor.preset)
            ThemeColorMode.Custom -> Unit
        }
    }

    fun encode(): String =
        mapOf(
            "version" to "1",
            "language" to language.name,
            "textSize" to textSize.name,
            "shortcut" to shortcut.name,
            "collapsePrompts" to collapsePrompts.toString(),
            "sideMargins" to sideMargins.toString(),
            "themeMode" to themeMode.name,
            "colorMode" to themeColor.mode.name,
            "preset" to themeColor.preset.name,
            "customColor" to themeColor.customSeed.toArgb().toString(),
        ).entries.joinToString("\n") { "${it.key}=${it.value}" }

    companion object {
        fun decode(encoded: String?): UserPreferences {
            val fields =
                encoded.orEmpty().lineSequence().filter { '=' in it }.associate {
                    it.substringBefore(
                        '=',
                    ) to it.substringAfter('=')
                }
            return UserPreferences(
                language = enumValue(fields["language"], AppLanguage.System),
                textSize = enumValue(fields["textSize"], ConversationTextSize.System),
                shortcut = enumValue(fields["shortcut"], SendShortcut.ModifiedEnter),
                collapsePrompts = fields["collapsePrompts"]?.toBooleanStrictOrNull() ?: true,
                sideMargins = fields["sideMargins"]?.toBooleanStrictOrNull() ?: true,
                themeMode = enumValue(fields["themeMode"], ThemeMode.System),
                themeColor =
                    ThemeColorSelection(
                        mode = enumValue(fields["colorMode"], ThemeColorMode.System),
                        preset = enumValue(fields["preset"], ThemePreset.Purple),
                        customSeed = fields["customColor"]?.toIntOrNull()?.let { Color(it) } ?: ThemePreset.Purple.seed,
                    ),
            )
        }
    }
}

private inline fun <reified T : Enum<T>> enumValue(
    value: String?,
    fallback: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: fallback
