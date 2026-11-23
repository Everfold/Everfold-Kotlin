package com.everfold.feature.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.LocaleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.everfold.core.model.AppLanguage

internal fun readSystemPermissionStatus(context: Context): SystemPermissionStatus =
    SystemPermissionStatus(
        notificationsGranted = hasNotificationPermission(context),
        backgroundUnrestricted = isIgnoringBatteryOptimizations(context),
    )

internal fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

internal fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
    return powerManager.isIgnoringBatteryOptimizations(context.packageName)
}

internal fun openNotificationSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent) { openAppDetailsSettings(context) }
}

@SuppressLint("BatteryLife")
internal fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent) { openBatteryManagementSettings(context) }
}

internal fun openBatteryManagementSettings(context: Context) {
    val batteryIntent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, batteryIntent) { openAppDetailsSettings(context) }
}

internal fun openAppDetailsSettings(context: Context) {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    launchSafely(context, intent)
}

internal fun applyAppLanguage(
    context: Context,
    language: AppLanguage,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val localeManager = context.getSystemService(LocaleManager::class.java) ?: return
        localeManager.applicationLocales =
            if (language.languageTag.isEmpty()) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language.languageTag)
            }
    }
}

internal fun openUrlInCustomTab(
    context: Context,
    url: String,
) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        launchSafely(context, Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
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
