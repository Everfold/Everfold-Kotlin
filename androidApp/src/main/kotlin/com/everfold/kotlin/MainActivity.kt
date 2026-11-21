package com.everfold.kotlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.everfold.kotlin.mock.longtask.AndroidLongTaskRuntime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidLongTaskRuntime.bindHost(applicationContext, EverfoldTaskService::sync)
        handleTaskNotificationIntent(intent)
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
        handleIncomingShareIntent(intent)
    }

    private fun handleTaskNotificationIntent(intent: Intent?) {
        val conversationId = intent?.getStringExtra(EXTRA_CONVERSATION_ID) ?: return
        AndroidLongTaskRuntime.requestOpenConversation(conversationId)
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
