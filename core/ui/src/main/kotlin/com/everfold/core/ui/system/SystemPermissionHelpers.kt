package com.everfold.core.ui.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

fun readSystemPermissionStatus(context: Context): SystemPermissionStatus =
    SystemPermissionStatus(
        notificationsGranted = hasNotificationPermission(context),
        backgroundUnrestricted = isIgnoringBatteryOptimizations(context),
    )

fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent) { openAppDetailsSettings(context) }
}

@SuppressLint("BatteryLife")
fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent) { openBatteryManagementSettings(context) }
}

fun openBatteryManagementSettings(context: Context) {
    openAppDetailsSettings(context)
}

fun openAppDetailsSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:${context.packageName}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent)
}

private fun launchSafely(
    context: Context,
    intent: Intent,
    fallback: (() -> Unit)? = null,
) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        fallback?.invoke()
    }
}
