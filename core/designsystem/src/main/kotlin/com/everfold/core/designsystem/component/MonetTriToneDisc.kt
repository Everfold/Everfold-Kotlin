package com.everfold.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.theme.MonetSwatchPalette

private const val TOP_START_ANGLE = 180f
private const val TOP_SWEEP_ANGLE = 180f
private const val BOTTOM_LEFT_START_ANGLE = 90f
private const val BOTTOM_RIGHT_START_ANGLE = 0f
private const val QUARTER_SWEEP_ANGLE = 90f
private const val CHECK_START_X = 0.24f
private const val CHECK_START_Y = 0.53f
private const val CHECK_MID_X = 0.43f
private const val CHECK_MID_Y = 0.71f
private const val CHECK_END_X = 0.76f
private const val CHECK_END_Y = 0.31f
private const val CHECK_STROKE_DP = 1.8f
private const val CHECK_BADGE_PADDING_DP = 3.5f

@Composable
fun MonetTriToneDisc(
    palette: MonetSwatchPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(
                color = palette.top,
                startAngle = TOP_START_ANGLE,
                sweepAngle = TOP_SWEEP_ANGLE,
                useCenter = true,
            )
            drawArc(
                color = palette.bottomLeft,
                startAngle = BOTTOM_LEFT_START_ANGLE,
                sweepAngle = QUARTER_SWEEP_ANGLE,
                useCenter = true,
            )
            drawArc(
                color = palette.bottomRight,
                startAngle = BOTTOM_RIGHT_START_ANGLE,
                sweepAngle = QUARTER_SWEEP_ANGLE,
                useCenter = true,
            )
        }
        if (selected) {
            SelectedCheckBadge(indicatorColor = palette.indicator)
        }
    }
}

@Composable
private fun SelectedCheckBadge(indicatorColor: Color) {
    Surface(
        modifier =
            Modifier
                .size(20.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(CHECK_BADGE_PADDING_DP.dp)) {
            val path =
                Path().apply {
                    moveTo(size.width * CHECK_START_X, size.height * CHECK_START_Y)
                    lineTo(size.width * CHECK_MID_X, size.height * CHECK_MID_Y)
                    lineTo(size.width * CHECK_END_X, size.height * CHECK_END_Y)
                }
            drawPath(
                path = path,
                color = indicatorColor,
                style =
                    Stroke(
                        width = CHECK_STROKE_DP.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
            )
        }
    }
}
