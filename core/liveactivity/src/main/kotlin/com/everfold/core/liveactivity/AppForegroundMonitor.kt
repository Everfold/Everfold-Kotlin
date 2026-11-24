package com.everfold.core.liveactivity

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppForegroundMonitor : Application.ActivityLifecycleCallbacks {
    private val startedActivities = mutableSetOf<Int>()
    private val _isForeground = MutableStateFlow(false)
    val isForeground: StateFlow<Boolean> = _isForeground.asStateFlow()

    override fun onActivityStarted(activity: Activity) {
        startedActivities.add(System.identityHashCode(activity))
        _isForeground.value = true
    }

    override fun onActivityStopped(activity: Activity) {
        startedActivities.remove(System.identityHashCode(activity))
        _isForeground.value = startedActivities.isNotEmpty()
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
