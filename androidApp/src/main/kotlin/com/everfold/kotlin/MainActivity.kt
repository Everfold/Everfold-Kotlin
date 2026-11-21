package com.everfold.kotlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.everfold.kotlin.mock.longtask.AndroidLongTaskRuntime

private const val ACTION_SHORTCUT_NEW_CHAT = "com.everfold.kotlin.action.SHORTCUT_NEW_CHAT"
private const val ACTION_SHORTCUT_ACTIVE_TASK = "com.everfold.kotlin.action.SHORTCUT_ACTIVE_TASK"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidLongTaskRuntime.bindHost(applicationContext, EverfoldTaskService::sync)
        handleTaskNotificationIntent(intent)
        handleShortcutIntent(intent)
        if (savedInstanceState == null) {
            handleIncomingShareIntent(intent)
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleTaskNotificationIntent(intent)
        handleShortcutIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleTaskNotificationIntent(intent: Intent?) {
        val conversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        AndroidLongTaskRuntime.requestOpenConversation(conversationId)
    }

    private fun handleShortcutIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SHORTCUT_NEW_CHAT -> {
                AndroidLongTaskRuntime.requestNewConversation()
            }

            ACTION_SHORTCUT_ACTIVE_TASK -> {
                val targetId = AndroidLongTaskRuntime.currentNotification.value?.conversationId
                if (targetId != null) {
                    AndroidLongTaskRuntime.requestOpenConversation(targetId)
                } else {
                    AndroidLongTaskRuntime.requestNewConversation()
                }
            }
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        val parsed = parseIncomingShareIntent(this, intent) ?: return
        AndroidLongTaskRuntime.deliverSharedContent(text = parsed.text, attachments = parsed.attachments)
    }
}

@Preview
@Composable
private fun AppAndroidPreview() {
    App()
}
