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

    // Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.ui.tooling.preview)
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
    implementation(libs.com.jakewharton.timber)

    testImplementation(libs.junit)
    testImplementation(libs.com.google.truth)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
    testImplementation(libs.app.cash.turbine)

    androidTestImplementation(platform(libs.ax.compose.bom))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.ax.test.runner)
    androidTestImplementation(libs.com.google.truth)
    androidTestImplementation(libs.ax.compose.ui.test)
    androidTestImplementation(libs.ax.compose.ui.test.junit4)
    debugImplementation(libs.ax.compose.ui.test.manifest)
}
