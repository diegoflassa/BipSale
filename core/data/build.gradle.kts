plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.core.data"
}

dependencies {
    // Room
    implementation(libs.ax.room.runtime)
    implementation(libs.ax.room.ktx)
    ksp(libs.ax.room.compiler)

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)

    // Coroutines
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)

    // Image
    implementation(libs.androidx.exifinterface)

    // Common
    implementation(libs.ax.core.ktx)
    implementation(project(":core:domain"))
    implementation(libs.com.jakewharton.timber)
}
