package com.everfold.kotlin.feature.onboarding

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.everfold.kotlin.ui.localization.UiText
import everfold.shared.generated.resources.Res
import everfold.shared.generated.resources.permission_live_updates_action
import everfold.shared.generated.resources.permission_live_updates_desc
import everfold.shared.generated.resources.permission_live_updates_title
import everfold.shared.generated.resources.permission_notifications_action
import everfold.shared.generated.resources.permission_notifications_desc
import everfold.shared.generated.resources.permission_notifications_title

@Composable
internal actual fun rememberOnboardingPermissionsController(): OnboardingPermissionsController {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return EmptyPermissionsController
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var notificationsGranted by remember(context) { mutableStateOf(hasNotificationPermission(context)) }
    var liveUpdatesGranted by remember(context) { mutableStateOf(hasLiveUpdatePermission(context)) }
    val refresh = {
        notificationsGranted = hasNotificationPermission(context)
        liveUpdatesGranted = hasLiveUpdatePermission(context)
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refresh()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            notificationsGranted = granted || hasNotificationPermission(context)
            liveUpdatesGranted = hasLiveUpdatePermission(context)
        }
    return remember(context, notificationsGranted, liveUpdatesGranted, notificationLauncher) {
        buildAndroidPermissionsController(
            context = context,
            notificationsGranted = notificationsGranted,
            liveUpdatesGranted = liveUpdatesGranted,
            requestNotification = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
    }
}

private fun buildAndroidPermissionsController(
    context: Context,
    notificationsGranted: Boolean,
    liveUpdatesGranted: Boolean,
    requestNotification: () -> Unit,
): OnboardingPermissionsController {
    val items =
        buildList {
            add(
                OnboardingPermissionItem(
                    id = PERMISSION_ID_NOTIFICATIONS,
                    title = UiText.Resource(Res.string.permission_notifications_title),
                    description = UiText.Resource(Res.string.permission_notifications_desc),
                    granted = notificationsGranted,
                    actionLabel = UiText.Resource(Res.string.permission_notifications_action),
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                add(
                    OnboardingPermissionItem(
                        id = PERMISSION_ID_LIVE_UPDATES,
                        title = UiText.Resource(Res.string.permission_live_updates_title),
                        description = UiText.Resource(Res.string.permission_live_updates_desc),
                        granted = liveUpdatesGranted,
                        actionLabel = UiText.Resource(Res.string.permission_live_updates_action),
                    ),
                )
            }
        }
    return object : OnboardingPermissionsController {
        override val items: List<OnboardingPermissionItem> = items

        override fun request(id: String) {
            if (id == PERMISSION_ID_NOTIFICATIONS) {
                requestNotification()
            } else if (id == PERMISSION_ID_LIVE_UPDATES && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                openPromotedNotificationSettings(context)
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

private fun hasLiveUpdatePermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
        true
    } else {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.canPostPromotedNotifications() == true
    }

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun openPromotedNotificationSettings(context: Context) {
    val promotionIntent =
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val fallbackIntent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(promotionIntent) }.onFailure { error ->
        if (error is ActivityNotFoundException) {
            runCatching { context.startActivity(fallbackIntent) }
        }
    }
}
