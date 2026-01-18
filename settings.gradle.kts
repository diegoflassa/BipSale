@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        //maven {
        //    url = uri("https://androidx.dev/snapshots/builds/13508953/artifacts/repository")
        //}
    }
}
rootProject.name = "BipSale"
includeBuild("build-logic")

include(":app")
include(":feature:sales")
include(":feature:products")
include(":feature:history")
include(":feature:qrcode")
include(":core:data")
include(":core:ui")
include(":core:utils")
include(":core:navigation")
include(":core:domain")
