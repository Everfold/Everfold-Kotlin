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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private const val PREFERENCES_VERSION = 1

/** 只保存应用偏好，连接凭据与会话内容由各自的数据层负责。 */
internal interface PreferenceStore {
    fun read(): String?

    fun write(value: String)
}

internal expect val preferenceIoDispatcher: CoroutineDispatcher

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
    persistScope: CoroutineScope? = null,
    ioDispatcher: CoroutineDispatcher = preferenceIoDispatcher,
) : AppearancePreferences {
    var value by mutableStateOf(UserPreferences.decode(store.read()))
        private set

    private val pendingWrites =
        persistScope?.let { scope ->
            Channel<String>(Channel.CONFLATED).also { channel ->
                scope.launch(ioDispatcher) {
                    for (encoded in channel) {
                        store.write(encoded)
                    }
                }
            }
        }

    override val themeMode: ThemeMode get() = value.themeMode
    override val themeColor: ThemeColorSelection get() = value.themeColor

    override fun setTheme(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    override fun useSystemThemeColor() = update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.System)) }

    override fun selectThemePreset(preset: ThemePreset) =
        update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.Preset, preset = preset)) }

    override fun applyCustomThemeColor(color: Color) =
        update { it.copy(themeColor = it.themeColor.copy(mode = ThemeColorMode.Custom, customSeed = color)) }

    fun markOnboardingCompleted() = update { it.copy(onboardingCompleted = true) }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(value)
        if (next == value) return
        val encoded = next.encode()
        value = next
        if (pendingWrites != null) {
            pendingWrites.trySend(encoded)
        } else {
            store.write(encoded)
        }
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
    val onboardingCompleted: Boolean = false,
) {
    fun encode(): String =
        mapOf(
            "version" to PREFERENCES_VERSION.toString(),
            "language" to language.name,
            "textSize" to textSize.name,
            "shortcut" to shortcut.name,
            "collapsePrompts" to collapsePrompts.toString(),
            "sideMargins" to sideMargins.toString(),
            "themeMode" to themeMode.name,
            "colorMode" to themeColor.mode.name,
            "preset" to themeColor.preset.name,
            "customColor" to themeColor.customSeed.toArgb().toString(),
            "onboardingCompleted" to onboardingCompleted.toString(),
        ).entries.joinToString("\n") { "${it.key}=${it.value}" }

    companion object {
        fun decode(encoded: String?): UserPreferences {
            val fields =
                encoded.orEmpty().lineSequence().filter { '=' in it }.associate {
                    it.substringBefore('=') to it.substringAfter('=')
                }
            val version = fields["version"]?.toIntOrNull()
            if (fields.containsKey("version") && (version == null || version > PREFERENCES_VERSION)) {
                return UserPreferences()
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
                onboardingCompleted = fields["onboardingCompleted"]?.toBooleanStrictOrNull() ?: false,
            )
        }
    }
}

private inline fun <reified T : Enum<T>> enumValue(
    value: String?,
    fallback: T,
): T = enumValues<T>().firstOrNull { it.name == value } ?: fallback
