plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.feature.sales"
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":core:qrcode"))
    implementation(project(":core:navigation"))
    implementation(project(":core:utils"))
    implementation(project(":core:qrcode"))

    // Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.lifecycle.viewmodel.compose)

    // CameraX
    implementation(libs.androidx.camera.core)

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    implementation(libs.androidx.camera.core)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    implementation(libs.ax.hilt.navigation.compose)

    // Common
    implementation(libs.ax.core.ktx)
}
