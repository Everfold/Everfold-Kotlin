plugins {
    alias(libs.plugins.everfold.android.feature)
}

android {
    namespace = "com.everfold.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.activity.compose)
}
