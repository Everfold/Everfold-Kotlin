import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.miuix.ui)
    implementation(libs.miuix.window)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.everfold.kotlin.MainKt"
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        if (System.getProperty("os.name").startsWith("Linux")) {
            jvmArgs +=
                listOf(
                    "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
                    "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
                    "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
                )
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.everfold.kotlin"
            packageVersion = "1.0.0"
        }
    }
}

val generateAppVersion =
    tasks.register<WriteProperties>("generateAppVersion") {
        destinationFile.set(layout.buildDirectory.file("generated/appVersion/everfold-app.properties"))
        property("version", requireNotNull(compose.desktop.application.nativeDistributions.packageVersion))
    }

tasks.processResources {
    from(generateAppVersion)
}
