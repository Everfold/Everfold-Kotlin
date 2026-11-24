package com.everfold.feature.onboarding.step

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.core.designsystem.theme.LocalAnimationSpeed
import com.everfold.core.designsystem.theme.effectsSpec
import com.everfold.core.designsystem.theme.spatialSpec
import com.everfold.feature.onboarding.R

private const val SELECTED_DOT_WIDTH_DP = 20
private const val UNSELECTED_DOT_WIDTH_DP = 8
private const val DOT_HEIGHT_DP = 8

private data class ShowcaseSlide(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
)

private val showcaseSlides =
    listOf(
        ShowcaseSlide(
            icon = Icons.Rounded.Hub,
            titleRes = R.string.onboarding_showcase_1_title,
            bodyRes = R.string.onboarding_showcase_1_body,
        ),
        ShowcaseSlide(
            icon = Icons.Rounded.Timeline,
            titleRes = R.string.onboarding_showcase_2_title,
            bodyRes = R.string.onboarding_showcase_2_body,
        ),
        ShowcaseSlide(
            icon = Icons.Rounded.Lan,
            titleRes = R.string.onboarding_showcase_3_title,
            bodyRes = R.string.onboarding_showcase_3_body,
        ),
    )

internal const val SHOWCASE_PAGE_COUNT = 3

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun OnboardingShowcaseStep(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = MaterialShapes.Cookie9Sided.toShape(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                EverfoldLogo(modifier = Modifier.size(56.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Everfold",
            style = MaterialTheme.typography.headlineLargeEmphasized,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(28.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(250.dp),
        ) { page ->
            val slide = showcaseSlides[page]
            ShowcaseSlideCard(slide = slide)
        }
        Spacer(modifier = Modifier.height(20.dp))
        ShowcasePageIndicators(currentPage = pagerState.currentPage)
    }
}

@Composable
private fun ShowcasePageIndicators(currentPage: Int) {
    val animationSpeed = LocalAnimationSpeed.current
    val widthSpec = animationSpeed.spatialSpec<Dp>()
    val colorSpec = animationSpeed.effectsSpec<Color>()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(SHOWCASE_PAGE_COUNT) { index ->
            val selected = currentPage == index
            val width by
                animateDpAsState(
                    targetValue = if (selected) SELECTED_DOT_WIDTH_DP.dp else UNSELECTED_DOT_WIDTH_DP.dp,
                    animationSpec = widthSpec,
                    label = "showcase_dot_width",
                )
            val color by
                animateColorAsState(
                    targetValue =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    animationSpec = colorSpec,
                    label = "showcase_dot_color",
                )
            Box(
                modifier =
                    Modifier
                        .size(width = width, height = DOT_HEIGHT_DP.dp)
                        .clip(CircleShape)
                        .background(color),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShowcaseSlideCard(slide: ShowcaseSlide) {
    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = MaterialShapes.SoftBurst.toShape(),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = slide.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(slide.titleRes),
                style = MaterialTheme.typography.titleLargeEmphasized,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(slide.bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
