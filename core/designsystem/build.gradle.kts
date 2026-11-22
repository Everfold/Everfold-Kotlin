plugins {
    alias(libs.plugins.everfold.android.library)
    alias(libs.plugins.everfold.android.compose)
}

android {
    namespace = "com.everfold.core.designsystem"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    implementation(libs.material.kolor)
}
