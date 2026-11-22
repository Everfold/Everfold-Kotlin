plugins {
    alias(libs.plugins.everfold.jvm.library)
}

dependencies {
    implementation(projects.core.gateway)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)

    testImplementation(projects.core.testing)
    testImplementation(libs.turbine)
}
