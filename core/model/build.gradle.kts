plugins {
    alias(libs.plugins.everfold.jvm.library)
}

dependencies {
    api(libs.kotlinx.collections.immutable)
    api(libs.kotlinx.serialization.json)
}
