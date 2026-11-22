plugins {
    alias(libs.plugins.everfold.jvm.library)
}

dependencies {
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
