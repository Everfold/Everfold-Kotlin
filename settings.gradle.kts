rootProject.name = "Everfold"

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/google") {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
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
    repositories {
        // Resolve Markdown and Miuix platform artifacts directly from Maven Central.
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("com.mikepenz")
                includeGroup("top.yukonga.miuix.kmp")
            }
        }
        maven("https://maven.aliyun.com/repository/google") {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        maven("https://maven.aliyun.com/repository/central")
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
