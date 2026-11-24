package com.everfold.feature.onboarding.step

import androidx.annotation.StringRes
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.everfold.core.designsystem.component.EverfoldLogo
import com.everfold.feature.onboarding.R

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
        EverfoldLogo(modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Everfold",
            style = MaterialTheme.typography.headlineLarge,
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(SHOWCASE_PAGE_COUNT) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier =
                        Modifier
                            .size(width = if (selected) 20.dp else 8.dp, height = 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                )
            }
        }
    }
}

@Composable
private fun ShowcaseSlideCard(slide: ShowcaseSlide) {
    Card(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp),
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
                style = MaterialTheme.typography.titleLarge,
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
