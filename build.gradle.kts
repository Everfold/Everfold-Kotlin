import com.diffplug.spotless.LineEnding
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import dev.detekt.gradle.Detekt

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.versions)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

detekt {
    source.setFrom("app/src")
    config.setFrom(layout.projectDirectory.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig.set(true)
    basePath.set(layout.projectDirectory)
}

tasks.withType<Detekt>().configureEach {
    include("**/*.kt")
    exclude("**/build/**")
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    checkConstraints = true
    rejectPreReleases = true
    outputFormatter = "plain,json"
}

spotless {
    lineEndings = LineEnding.UNIX

    kotlin {
        target("**/src/**/*.kt")
        targetExclude("**/build/**", ".agents/**")
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
        target("**/*.gradle.kts")
        targetExclude("**/build/**", ".gradle/**", ".agents/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("xml") {
        target("**/src/**/*.xml")
        targetExclude("**/build/**", ".agents/**")
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("markdown") {
        target("**/*.md")
        targetExclude("**/build/**", ".agents/**", ".gradle/**", ".direnv/**")
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
            "config/**/*.yml",
            "config/**/*.yaml",
            ".github/**/*.yml",
            ".github/**/*.yaml",
            "**/keepRules/**/*.keep",
            "**/proguard-rules.pro",
        )
        targetExclude("local.properties", "**/build/**", ".agents/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
