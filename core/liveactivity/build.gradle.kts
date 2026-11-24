plugins {
    alias(libs.plugins.everfold.android.library)
}

android {
    namespace = "com.everfold.core.liveactivity"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.gateway)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
