package com.everfold.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private const val SHAPE_EXTRA_SMALL_DP = 6
private const val SHAPE_SMALL_DP = 10
private const val SHAPE_MEDIUM_DP = 16
private const val SHAPE_LARGE_DP = 20
private const val SHAPE_EXTRA_LARGE_DP = 28

val EverfoldShapes =
    Shapes(
        extraSmall = RoundedCornerShape(SHAPE_EXTRA_SMALL_DP.dp),
        small = RoundedCornerShape(SHAPE_SMALL_DP.dp),
        medium = RoundedCornerShape(SHAPE_MEDIUM_DP.dp),
        large = RoundedCornerShape(SHAPE_LARGE_DP.dp),
        extraLarge = RoundedCornerShape(SHAPE_EXTRA_LARGE_DP.dp),
    )
