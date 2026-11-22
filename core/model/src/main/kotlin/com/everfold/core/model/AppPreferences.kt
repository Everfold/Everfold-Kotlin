package com.everfold.core.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
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

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val seedColor: SeedColorPreset = SeedColorPreset.INDIGO,
    val showNodeBadgesInline: Boolean = false,
)
