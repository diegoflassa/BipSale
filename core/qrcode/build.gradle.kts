plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.core.qrcode"
}

dependencies {
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    // ZXing
    implementation(libs.zxing.core)
    implementation(libs.guava)

    // Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.compose.ui.tooling.preview)
    implementation(libs.ax.lifecycle.viewmodel.compose)
    implementation(libs.ax.lifecycle.runtime.compose)
    implementation(project(":core:ui"))

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)

    // Common
    implementation(libs.ax.core.ktx)
    implementation(libs.com.jakewharton.timber)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.com.google.truth)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.ax.test.runner)
    androidTestImplementation(libs.com.google.truth)
    androidTestImplementation(libs.zxing.core)
}
