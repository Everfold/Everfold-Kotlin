package com.everfold.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class ThemeColorMode {
    SYSTEM_DYNAMIC,
    PRESET,
    CUSTOM,
}

enum class SeedColorPreset {
    BASELINE,
    BLUE,
    TEAL,
    GREEN,
    AMBER,
    ORANGE,
    ROSE,
    VIOLET,
}

enum class ThemePaletteStyle(
    val supportsSpec2025: Boolean,
) {
    TONAL_SPOT(supportsSpec2025 = true),
    NEUTRAL(supportsSpec2025 = true),
    VIBRANT(supportsSpec2025 = true),
    EXPRESSIVE(supportsSpec2025 = true),
    RAINBOW(supportsSpec2025 = false),
    FRUIT_SALAD(supportsSpec2025 = false),
    MONOCHROME(supportsSpec2025 = false),
    FIDELITY(supportsSpec2025 = false),
    CONTENT(supportsSpec2025 = false),
}

enum class ThemeColorSpec {
    SPEC_2021,
    SPEC_2025,
}

enum class AppLanguage(
    val languageTag: String,
) {
    SYSTEM(""),
    SIMPLIFIED_CHINESE("zh-Hans"),
    TRADITIONAL_CHINESE("zh-Hant"),
    ENGLISH("en"),
}

enum class ChatTextSize(
    val scale: Float,
) {
    SMALL(SMALL_SCALE),
    MEDIUM(MEDIUM_SCALE),
    LARGE(LARGE_SCALE),
    EXTRA_LARGE(EXTRA_LARGE_SCALE),
}

enum class AnimationSpeed(
    val durationScale: Float,
) {
    DEFAULT(DEFAULT_ANIMATION_SCALE),
    FAST(FAST_ANIMATION_SCALE),
    SLOW(SLOW_ANIMATION_SCALE),
    OFF(OFF_ANIMATION_SCALE),
}

private const val SMALL_SCALE = 0.88f
private const val MEDIUM_SCALE = 1.0f
private const val LARGE_SCALE = 1.14f
private const val EXTRA_LARGE_SCALE = 1.28f
private const val DEFAULT_ANIMATION_SCALE = 1.0f
private const val FAST_ANIMATION_SCALE = 0.55f
private const val SLOW_ANIMATION_SCALE = 1.75f
private const val OFF_ANIMATION_SCALE = 0f
private const val DEFAULT_SEED_HEX = "#6750A4"

data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorMode: ThemeColorMode = ThemeColorMode.SYSTEM_DYNAMIC,
    val seedColor: SeedColorPreset = SeedColorPreset.BASELINE,
    val customColorHex: String = DEFAULT_SEED_HEX,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TONAL_SPOT,
    val colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
)

data class NotificationPreferences(
    val liveActivityEnabled: Boolean = true,
    val alertOnNeedsAttention: Boolean = true,
    val alertOnCompleted: Boolean = true,
)

data class AppPreferences(
    val onboardingCompleted: Boolean = false,
    val theme: ThemePreferences = ThemePreferences(),
    val language: AppLanguage = AppLanguage.SYSTEM,
    val animationSpeed: AnimationSpeed = AnimationSpeed.DEFAULT,
    val chatTextSize: ChatTextSize = ChatTextSize.MEDIUM,
    val autoCollapseLongMessages: Boolean = true,
    val expandRunningActionTraces: Boolean = true,
    val expandCompletedActionTraces: Boolean = false,
    val notifications: NotificationPreferences = NotificationPreferences(),
)

data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val repositoryUrl: String = "https://github.com/Everfold/Everfold-Kotlin",
    val licenseName: String = "Apache 2.0",
)
