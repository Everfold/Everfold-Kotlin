package com.everfold.kotlin.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.everfold.kotlin.ui.localization.UiText

internal const val PERMISSION_ID_NOTIFICATIONS = "notifications"
internal const val PERMISSION_ID_LIVE_UPDATES = "live_updates"
internal const val PERMISSION_ID_BATTERY_OPTIMIZATION = "battery_optimization"

/** 引导页展示的单条平台系统权限项。 */
@Immutable
internal data class OnboardingPermissionItem(
    val id: String,
    val title: UiText,
    val description: UiText,
    val granted: Boolean,
    val actionLabel: UiText,
)

internal interface OnboardingPermissionsController {
    val items: List<OnboardingPermissionItem>

    fun request(id: String)
}

internal object EmptyPermissionsController : OnboardingPermissionsController {
    override val items: List<OnboardingPermissionItem> = emptyList()

    override fun request(id: String) = Unit
}

@Composable
internal expect fun rememberOnboardingPermissionsController(): OnboardingPermissionsController
