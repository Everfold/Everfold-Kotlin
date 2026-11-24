package com.everfold.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BASE_TYPOGRAPHY = Typography()

private const val DISPLAY_LETTER_SPACING_SP = -0.25f

val EverfoldTypography =
    Typography(
        displayLarge =
            BASE_TYPOGRAPHY.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = DISPLAY_LETTER_SPACING_SP.sp,
            ),
        displayMedium =
            BASE_TYPOGRAPHY.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = DISPLAY_LETTER_SPACING_SP.sp,
            ),
        displaySmall = BASE_TYPOGRAPHY.displaySmall.copy(fontWeight = FontWeight.SemiBold),
        headlineLarge = BASE_TYPOGRAPHY.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = BASE_TYPOGRAPHY.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = BASE_TYPOGRAPHY.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = BASE_TYPOGRAPHY.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = BASE_TYPOGRAPHY.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = BASE_TYPOGRAPHY.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = BASE_TYPOGRAPHY.bodyLarge,
        bodyMedium = BASE_TYPOGRAPHY.bodyMedium,
        bodySmall = BASE_TYPOGRAPHY.bodySmall,
        labelLarge = BASE_TYPOGRAPHY.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = BASE_TYPOGRAPHY.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = BASE_TYPOGRAPHY.labelSmall.copy(fontWeight = FontWeight.Medium),
    )
