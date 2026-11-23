package com.everfold.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.everfold.core.model.AnimationSpeed
import kotlin.math.roundToInt

val LocalAnimationSpeed = staticCompositionLocalOf { AnimationSpeed.DEFAULT }

fun AnimationSpeed.scaleDuration(baseMs: Int): Int =
    if (this == AnimationSpeed.OFF || baseMs <= 0) {
        0
    } else {
        (baseMs * durationScale).roundToInt().coerceAtLeast(1)
    }
