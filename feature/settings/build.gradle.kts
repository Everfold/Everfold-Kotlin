plugins {
    alias(libs.plugins.everfold.android.feature)
}

android {
    namespace = "com.everfold.feature.settings"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core.ktx)
}
