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

enum class SeedColorPreset(
    val displayName: String,
) {
    INDIGO("靛蓝"),
    TEAL("青黛"),
    TERRACOTTA("陶土"),
    FOREST("森绿"),
    PLUM("梅紫"),
}

enum class AppLanguage(
    val languageTag: String,
) {
    SYSTEM(""),
    CHINESE("zh-CN"),
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

private const val SMALL_SCALE = 0.88f
private const val MEDIUM_SCALE = 1.0f
private const val LARGE_SCALE = 1.14f
private const val EXTRA_LARGE_SCALE = 1.28f

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeColorMode: ThemeColorMode = ThemeColorMode.SYSTEM_DYNAMIC,
    val seedColor: SeedColorPreset = SeedColorPreset.INDIGO,
    val customColorHex: String = "#3F51B5",
    val language: AppLanguage = AppLanguage.SYSTEM,
    val chatTextSize: ChatTextSize = ChatTextSize.MEDIUM,
    val autoCollapseLongMessages: Boolean = true,
    val showNodeBadgesInline: Boolean = false,
)

data class AppInfo(
    val versionName: String,
    val versionCode: Long,
    val repositoryUrl: String = "https://github.com/Everfold/Everfold-Kotlin",
    val licenseName: String = "Apache 2.0",
)
