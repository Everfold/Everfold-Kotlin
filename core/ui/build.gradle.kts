plugins {
    alias(libs.plugins.everfold.android.library)
    alias(libs.plugins.everfold.android.compose)
}

android {
    namespace = "com.everfold.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.markdown.renderer.m3)
}
