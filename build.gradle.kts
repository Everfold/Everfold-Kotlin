import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.spotless)
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    checkConstraints = true
    rejectPreReleases = true
    outputFormatter = "plain,json"
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
            .customRuleSets(
                listOf(
                    libs.compose.rules.ktlint
                        .get()
                        .toString(),
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "app/build.gradle.kts")
        targetExclude("**/build/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("config") {
        target(
            ".editorconfig",
            ".gitignore",
            "*.properties",
            "*.toml",
            "*.yml",
            "*.yaml",
            "gradle/**/*.properties",
            "gradle/**/*.toml",
            ".github/**/*.yml",
            ".github/**/*.yaml",
        )
        targetExclude("local.properties", "**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
