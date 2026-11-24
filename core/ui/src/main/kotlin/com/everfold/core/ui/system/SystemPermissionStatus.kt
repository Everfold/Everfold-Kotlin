package com.everfold.core.ui.system

data class SystemPermissionStatus(
    val notificationsGranted: Boolean,
    val backgroundUnrestricted: Boolean,
    val promotedNotificationsSupported: Boolean = false,
    val promotedNotificationsGranted: Boolean = false,
)
