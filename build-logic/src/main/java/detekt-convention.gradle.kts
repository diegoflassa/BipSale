import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Static analysis for every module, not just :app. Lives here rather than in each build file so
// the ruleset cannot drift module by module (GRADLE_RULES section 8).
plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
        md.required.set(true)
    }
}
