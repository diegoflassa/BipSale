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

    // MigrationTestHelper reads the exported schemas from the test APK's assets at runtime, so the
    // schema directory has to ship inside it.
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }
}

// Exported schemas are what let a migration test catch a forgotten version bump in CI instead of
// at a point of sale (CORE_RULES §13).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Room
    implementation(libs.ax.datastore.preferences)
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

    // Backup archive
    implementation(libs.kotlinx.serialization.json)

    // Common
    implementation(libs.ax.core.ktx)
    implementation(project(":core:domain"))
    implementation(project(":core:utils"))
    implementation(libs.com.jakewharton.timber)

    testImplementation(libs.junit)
    testImplementation(libs.com.google.truth)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.ax.test.ext.junit.ktx)
    androidTestImplementation(libs.ax.test.runner)
    androidTestImplementation(libs.com.google.truth)
    androidTestImplementation(libs.ax.room.testing)
}
