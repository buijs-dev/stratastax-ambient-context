plugins {
    id("stratastax.library")
}

description = "Generic CoroutineContext utilities, used throughout Stratastax"

publishing {
    publications.named<MavenPublication>("mavenJava") { artifactId = "ambient-context" }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.smallrye.mutiny.kotlin)
    testImplementation(libs.bundles.test)
}
