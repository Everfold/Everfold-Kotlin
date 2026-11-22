import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.everfold.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "everfold.jvm.library"
            implementationClass = "com.everfold.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "everfold.android.library"
            implementationClass = "com.everfold.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "everfold.android.compose"
            implementationClass = "com.everfold.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "everfold.android.feature"
            implementationClass = "com.everfold.buildlogic.AndroidFeatureConventionPlugin"
        }
    }
}
