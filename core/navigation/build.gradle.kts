plugins {
    id("android-library-convention")
    alias(libs.plugins.kotlin.serialization)
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
