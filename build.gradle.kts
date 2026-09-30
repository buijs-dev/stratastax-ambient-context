plugins {
    id("stratastax.kotlin")
    id("stratastax.publishing")
    id("stratastax.repositories")
    id("stratastax.spotless")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "ambient-context"
            pom {
                name = "Stratastax Ambient Context"
                description = "Generic CoroutineContext utilities, used throughout Stratastax"
                url = "https://github.com/buijs-dev/stratastax-ambient-core"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://github.com/buijs-dev/stratastax-ambient-core/blob/main/LICENSE"
                    }
                }

                developers {
                    developer {
                        id = "buijs-dev"
                        name = "Gillian Buijs"
                        email = "info@buijs.dev"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/buijs-dev/stratastax-ambient-context.git"
                    developerConnection = "scm:git:ssh://git@github.com/buijs-dev/stratastax-ambient-context.git"
                    url = "https://github.com/buijs-dev/stratastax-ambient-context"
                }
            }
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.smallrye.mutiny.kotlin)
    testImplementation(libs.bundles.test)
}
