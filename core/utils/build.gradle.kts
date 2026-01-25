plugins {
    id("android-library-convention")
    id("com.android.library")
    alias(libs.plugins.com.google.devtools.ksp)
    alias(libs.plugins.hilt.android.gradle.plugin)
}

android {
    namespace = "dev.diegoflassa.bipsale.core.utils"
}

dependencies {
    // Excel Export
    implementation(libs.apache.poi.ooxml)
    
    // Internal
    implementation(project(":core:domain"))

    // Hilt
    implementation(libs.com.google.dagger.hilt.android)
    ksp(libs.com.google.dagger.hilt.android.compiler)
    
    // Common
    implementation(libs.ax.core.ktx)
}
