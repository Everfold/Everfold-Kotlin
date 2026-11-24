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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.markdown.renderer.m3)
}
