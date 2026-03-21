plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.feature.qrcode"
}

dependencies {
    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
}
