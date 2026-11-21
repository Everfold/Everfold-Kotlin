package com.everfold.kotlin.feature.onboarding

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
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
import everfold.shared.generated.resources.permission_battery_action
import everfold.shared.generated.resources.permission_battery_desc
import everfold.shared.generated.resources.permission_battery_title
import everfold.shared.generated.resources.permission_live_updates_action
import everfold.shared.generated.resources.permission_live_updates_desc
import everfold.shared.generated.resources.permission_live_updates_title
import everfold.shared.generated.resources.permission_notifications_action
import everfold.shared.generated.resources.permission_notifications_desc
import everfold.shared.generated.resources.permission_notifications_title

private data class AndroidPermissionSnapshot(
    val notificationsGranted: Boolean,
    val liveUpdatesGranted: Boolean,
    val batteryGranted: Boolean,
)

@Composable
internal actual fun rememberOnboardingPermissionsController(): OnboardingPermissionsController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var snapshot by remember(context) { mutableStateOf(readPermissionSnapshot(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) snapshot = readPermissionSnapshot(context)
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val updated = readPermissionSnapshot(context).copy(notificationsGranted = granted || hasNotificationPermission(context))
            snapshot = updated
            if (!updated.notificationsGranted && isPermanentlyDeniedNotification(context)) {
                openAppNotificationSettings(context)
            }
        }
    return remember(context, snapshot, notificationLauncher) {
        buildAndroidPermissionsController(
            context = context,
            snapshot = snapshot,
            requestNotification = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
        )
    }
}

private fun readPermissionSnapshot(context: Context): AndroidPermissionSnapshot =
    AndroidPermissionSnapshot(
        notificationsGranted = hasNotificationPermission(context),
        liveUpdatesGranted = hasLiveUpdatePermission(context),
        batteryGranted = hasBatteryOptimizationExemption(context),
    )

private fun buildAndroidPermissionsController(
    context: Context,
    snapshot: AndroidPermissionSnapshot,
    requestNotification: () -> Unit,
): OnboardingPermissionsController {
    val items =
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    OnboardingPermissionItem(
                        id = PERMISSION_ID_NOTIFICATIONS,
                        title = UiText.Resource(Res.string.permission_notifications_title),
                        description = UiText.Resource(Res.string.permission_notifications_desc),
                        granted = snapshot.notificationsGranted,
                        actionLabel = UiText.Resource(Res.string.permission_notifications_action),
                    ),
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                add(
                    OnboardingPermissionItem(
                        id = PERMISSION_ID_LIVE_UPDATES,
                        title = UiText.Resource(Res.string.permission_live_updates_title),
                        description = UiText.Resource(Res.string.permission_live_updates_desc),
                        granted = snapshot.liveUpdatesGranted,
                        actionLabel = UiText.Resource(Res.string.permission_live_updates_action),
                    ),
                )
            }
            add(
                OnboardingPermissionItem(
                    id = PERMISSION_ID_BATTERY_OPTIMIZATION,
                    title = UiText.Resource(Res.string.permission_battery_title),
                    description = UiText.Resource(Res.string.permission_battery_desc),
                    granted = snapshot.batteryGranted,
                    actionLabel = UiText.Resource(Res.string.permission_battery_action),
                ),
            )
        }
    return object : OnboardingPermissionsController {
        override val items: List<OnboardingPermissionItem> = items

        override fun request(id: String) {
            when (id) {
                PERMISSION_ID_NOTIFICATIONS -> {
                    requestNotification()
                }

                PERMISSION_ID_LIVE_UPDATES -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) openPromotedNotificationSettings(context)
                }

                PERMISSION_ID_BATTERY_OPTIMIZATION -> {
                    openBatteryOptimizationSettings(context)
                }
            }
        }

        override fun openSettings(id: String) {
            when (id) {
                PERMISSION_ID_NOTIFICATIONS -> {
                    openAppNotificationSettings(context)
                }

                PERMISSION_ID_LIVE_UPDATES -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                        openPromotedNotificationSettings(context)
                    } else {
                        openAppNotificationSettings(context)
                    }
                }

                PERMISSION_ID_BATTERY_OPTIMIZATION -> {
                    openAppDetailsSettings(context)
                }
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

private fun isPermanentlyDeniedNotification(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        context is Activity &&
        !context.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)

private fun hasLiveUpdatePermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
        true
    } else {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.canPostPromotedNotifications() == true
    }

private fun hasBatteryOptimizationExemption(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
private fun openPromotedNotificationSettings(context: Context) {
    val promotionIntent =
        Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(promotionIntent) }.onFailure { error ->
        if (error is ActivityNotFoundException) openAppNotificationSettings(context)
    }
}

private fun openAppNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
}

@SuppressLint("BatteryLife")
private fun openBatteryOptimizationSettings(context: Context) {
    val directIntent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    val listIntent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(directIntent) }
        .recoverCatching { context.startActivity(listIntent) }
        .onFailure { openAppDetailsSettings(context) }
}

private fun openAppDetailsSettings(context: Context) {
    val appDetailsIntent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(appDetailsIntent) }
}
