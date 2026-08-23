plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.feature.products"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:qrcode"))
    implementation(project(":core:domain"))

    // Compose
    implementation(platform(libs.ax.compose.bom))
    implementation(libs.ax.compose.ui)
    implementation(libs.ax.compose.ui.tooling.preview)
    implementation(libs.ax.compose.material3)
    implementation(libs.ax.compose.material.icons.core)
    implementation(libs.ax.compose.material.icons.extended)
    implementation(libs.ax.lifecycle.viewmodel.compose)
    implementation(libs.ax.lifecycle.runtime.compose)
    implementation(libs.ax.activity.compose)

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    implementation(libs.ax.hilt.navigation.compose)

    // Image loading
    implementation(libs.io.coil.kt.coil.compose)

    // Timber
    implementation(libs.com.jakewharton.timber)

    // Common
    implementation(libs.ax.core.ktx)
    implementation(libs.ax.compose.ui.graphics)

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
