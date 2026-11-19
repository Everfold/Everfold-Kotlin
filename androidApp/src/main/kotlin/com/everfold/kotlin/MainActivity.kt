package com.everfold.kotlin

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.everfold.kotlin.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.System) }
            DisposableEffect(themeMode) {
                val barStyle =
                    when (themeMode) {
                        ThemeMode.System -> SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
                        ThemeMode.Light -> SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        ThemeMode.Dark -> SystemBarStyle.dark(Color.TRANSPARENT)
                    }
                enableEdgeToEdge(
                    statusBarStyle = barStyle,
                    navigationBarStyle = barStyle,
                )
                onDispose {}
            }
            App(onAppearanceChange = { mode, _ -> themeMode = mode })
        }
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    App()
}
