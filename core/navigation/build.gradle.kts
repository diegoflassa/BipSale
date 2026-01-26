plugins {
    id("android-library-convention")
    id("com.android.library")
    // alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    // alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.core.navigation"
}

dependencies {
    // Navigation Compose 3
    implementation(libs.ax.navigation3.runtime)
    implementation(libs.ax.navigation3.ui)
    implementation(libs.ax.navigation3.viewmodel)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Common
    implementation(libs.ax.core.ktx)
}
