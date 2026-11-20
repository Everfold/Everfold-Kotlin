rootProject.name = "Everfold"

pluginManagement {
    val useAliyunMirror =
        providers
            .gradleProperty("useAliyunMirror")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("USE_ALIYUN_MIRROR").map(String::toBoolean))
            .getOrElse(false)
    repositories {
        if (useAliyunMirror) {
            maven("https://maven.aliyun.com/repository/google") {
                mavenContent {
                    includeGroupAndSubgroups("androidx")
                    includeGroupAndSubgroups("com.android")
                    includeGroupAndSubgroups("com.google")
                }
            }
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val useAliyunMirror =
        providers
            .gradleProperty("useAliyunMirror")
            .map(String::toBoolean)
            .orElse(providers.environmentVariable("USE_ALIYUN_MIRROR").map(String::toBoolean))
            .getOrElse(false)
    repositories {
        if (useAliyunMirror) {
            // Resolve all Markdown platform artifacts from the publishing repository.
            exclusiveContent {
                forRepository { mavenCentral() }
                filter { includeGroup("com.mikepenz") }
            }
            maven("https://maven.aliyun.com/repository/google") {
                mavenContent {
                    includeGroupAndSubgroups("androidx")
                    includeGroupAndSubgroups("com.android")
                    includeGroupAndSubgroups("com.google")
                }
            }
            maven("https://maven.aliyun.com/repository/central")
        }
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("io.github.ben-manes.versions") version "0.67.0"
}

include(":androidApp")
include(":desktopApp")
include(":shared")
