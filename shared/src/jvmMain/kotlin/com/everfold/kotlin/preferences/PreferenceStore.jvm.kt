package com.everfold.kotlin.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.util.prefs.Preferences

internal actual val preferenceIoDispatcher: CoroutineDispatcher = Dispatchers.IO

@Composable
internal actual fun rememberPreferenceStore(): PreferenceStore =
    remember {
        JvmPreferenceStore(Preferences.userRoot().node("com/everfold/kotlin"))
    }

internal class JvmPreferenceStore(
    private val preferences: Preferences,
) : PreferenceStore {
    override fun read(): String? = preferences.get("ui", null)

    override fun write(value: String) {
        preferences.put("ui", value)
        preferences.flush()
    }
}
