package com.everfold.kotlin.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSUserDefaults

internal actual val preferenceIoDispatcher: CoroutineDispatcher = Dispatchers.IO

@Composable
internal actual fun rememberPreferenceStore(): PreferenceStore =
    remember {
        object : PreferenceStore {
            override fun read(): String? = NSUserDefaults.standardUserDefaults.stringForKey("everfold.ui")

            override fun write(value: String) {
                NSUserDefaults.standardUserDefaults.setObject(value, forKey = "everfold.ui")
            }
        }
    }
