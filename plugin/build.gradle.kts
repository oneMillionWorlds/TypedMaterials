plugins {
    `java-gradle-plugin`
    `maven-publish`
    signing
    alias(libs.plugins.plugin.publish)
}

group = "com.onemillionworlds"
version = "1.4.1"

repositories {
    mavenCentral()
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    // Gradle 9 requires Java 17+ so there is no point targeting anything older; but pin it so building on a newer
    // JDK doesn't produce class files that consumers on 17 can't load
    options.release = 17
    options.compilerArgs.add("-Xdoclint:-missing")
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit)
        }

        val functionalTest by registering(JvmTestSuite::class) {
            useJUnitJupiter(libs.versions.junit)
            targets.configureEach {
                testTask.configure { shouldRunAfter(test) }
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

gradlePlugin {
    website = "https://github.com/oneMillionWorlds/TypedMaterials/wiki"
    vcsUrl = "https://github.com/oneMillionWorlds/TypedMaterials"
    plugins {
        create("typedMaterialsPlugin") {
            id = "com.onemillionworlds.typed-materials"
            displayName = "Typed Materials Plugin"
            description = "A plugin to synthesize java classes for jMonkeyEngine materials allowing them to be configured using type safe java code"
            tags = listOf("jMonkeyEngine", "oneMillionWorlds", "java", "MatDef", "Material")
            implementationClass = "com.onemillionworlds.TypedMaterialsPlugin"
        }
    }
    testSourceSets(sourceSets["functionalTest"])
}

tasks.named("check") {
    dependsOn(testing.suites.named("functionalTest"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "typed-materials"

            from(components["java"])

            pom {
                name = project.name
                description = "A library to create java classes for jMonkey materials"
                url = "https://github.com/oneMillionWorlds/TypedMaterials"
                licenses {
                    license {
                        name = "New BSD (3-clause) License"
                        url = "http://opensource.org/licenses/BSD-3-Clause"
                    }
                }
                scm {
                    connection = "git@github.com:oneMillionWorlds/TypedMaterials.git"
                    developerConnection = "git@github.com:oneMillionWorlds/TypedMaterials.git"
                    url = "https://github.com/oneMillionWorlds/TypedMaterials"
                }
                developers {
                    developer {
                        id = "RichardTingle"
                        name = "Richard Tingle (aka richtea)"
                        email = "support@oneMillionWorlds.com"
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = providers.gradleProperty("ossrhUsername").getOrElse("")
                password = providers.gradleProperty("ossrhPassword").getOrElse("")
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
