plugins {
    id("android-library-convention")
    id("com.android.library")
    // alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
     alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.core.ui"
}

dependencies {
    // Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.ui.graphics)
    implementation(libs.ax.compose.ui.tooling)
    implementation(libs.ax.compose.ui.tooling.preview)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.activity.compose)
    implementation(libs.io.coil.kt.coil.compose)

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)

    // Common
    implementation(libs.ax.core.ktx)
}
