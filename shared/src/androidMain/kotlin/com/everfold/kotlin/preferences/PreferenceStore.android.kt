package com.everfold.kotlin.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val preferenceIoDispatcher: CoroutineDispatcher = Dispatchers.IO

@Composable
internal actual fun rememberPreferenceStore(): PreferenceStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val preferences = context.getSharedPreferences("everfold_preferences", Context.MODE_PRIVATE)
        object : PreferenceStore {
            override fun read(): String? = preferences.getString("ui", null)

            override fun write(value: String) {
                preferences.edit().putString("ui", value).apply()
            }
        }
    }
}
