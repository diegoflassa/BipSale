plugins {
    id("android-library-convention")
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.org.jetbrains.kotlinx.coroutines.test)

    // Common
    implementation(libs.ax.core.ktx)
    implementation(project(":core:domain"))
    implementation(libs.com.jakewharton.timber)
}
