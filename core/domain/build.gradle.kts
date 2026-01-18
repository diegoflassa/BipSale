plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation("javax.inject:javax.inject:1") // For @Inject if needed, though usually standard in domain for usecase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
