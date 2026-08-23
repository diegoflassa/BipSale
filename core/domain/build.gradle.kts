plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("detekt-convention")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.com.google.truth)
    testImplementation(libs.org.jetbrains.kotlinx.coroutines.test)
}
