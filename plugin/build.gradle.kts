/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Gradle plugin project to get you started.
 * For more details on writing Custom Plugins, please refer to https://docs.gradle.org/8.6/userguide/custom_plugins.html in the Gradle documentation.
 */

plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("signing")
    id("com.gradle.plugin-publish") version "1.2.1"
}

group = "com.onemillionworlds"
version = "1.2.2"

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
}

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation(libs.junit.jupiter)

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        website = "https://github.com/oneMillionWorlds/TypedMaterials/wiki"
        vcsUrl = "https://github.com/oneMillionWorlds/TypedMaterials"
        create("typedMaterialsPlugin") {
            id = "com.onemillionworlds.typed-materials"
            displayName = "Typed Materials Plugin"
            description = "A plugin to synthesize java classes for jMonkeyEngine materials allowing them to be configured using type safe java code"
            tags = listOf("jMonkeyEngine", "oneMillionWorlds", "java", "MatDef", "Material")
            implementationClass = "com.onemillionworlds.TypedMaterialsPlugin"
        }
    }

}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xdoclint:-missing")
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks.named<Task>("check") {
    dependsOn(functionalTest)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

java{
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group.toString()
            artifactId = "typed-materials"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set(project.name)
                description.set("A library to create java classes for jMonkey materials")
                url.set("https://github.com/oneMillionWorlds/TypedMaterials")
                licenses {
                    license {
                        name.set("New BSD (3-clause) License")
                        url.set("http://opensource.org/licenses/BSD-3-Clause")
                    }
                }
                scm {
                    connection.set("git@github.com:oneMillionWorlds/TypedMaterials.git")
                    developerConnection.set("git@github.com:oneMillionWorlds/TypedMaterials.git")
                    url.set("https://github.com/oneMillionWorlds/TypedMaterials")
                }
                developers {
                    developer {
                        id.set("RichardTingle")
                        name.set("Richard Tingle (aka richtea)")
                        email.set("support@oneMillionWorlds.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            // url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            credentials {
                username = findProperty("ossrhUsername")?.toString() ?: ""
                password = findProperty("ossrhPassword")?.toString() ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}