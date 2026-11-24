package com.everfold.core.designsystem.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.MotionDurationScale
import com.everfold.core.model.AnimationSpeed
import kotlin.math.roundToInt

val LocalAnimationSpeed = staticCompositionLocalOf { AnimationSpeed.DEFAULT }

fun AnimationSpeed.scaleDuration(baseMs: Int): Int =
    if (this == AnimationSpeed.OFF || baseMs <= 0) {
        0
    } else {
        (baseMs * durationScale).roundToInt().coerceAtLeast(1)
    }

fun AnimationSpeed.toMotionDurationScale(): MotionDurationScale =
    object : MotionDurationScale {
        override val scaleFactor: Float = durationScale
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> AnimationSpeed.spatialSpec(): FiniteAnimationSpec<T> = spatialSpecFor(MaterialTheme.motionScheme)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun <T> AnimationSpeed.effectsSpec(): FiniteAnimationSpec<T> = effectsSpecFor(MaterialTheme.motionScheme)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun <T> AnimationSpeed.spatialSpecFor(motionScheme: MotionScheme = MotionScheme.expressive()): FiniteAnimationSpec<T> =
    when (this) {
        AnimationSpeed.OFF -> snap()
        AnimationSpeed.FAST -> motionScheme.fastSpatialSpec()
        AnimationSpeed.DEFAULT -> motionScheme.defaultSpatialSpec()
        AnimationSpeed.SLOW -> motionScheme.slowSpatialSpec()
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun <T> AnimationSpeed.effectsSpecFor(motionScheme: MotionScheme = MotionScheme.expressive()): FiniteAnimationSpec<T> =
    when (this) {
        AnimationSpeed.OFF -> snap()
        AnimationSpeed.FAST -> motionScheme.fastEffectsSpec()
        AnimationSpeed.DEFAULT -> motionScheme.defaultEffectsSpec()
        AnimationSpeed.SLOW -> motionScheme.slowEffectsSpec()
    }
