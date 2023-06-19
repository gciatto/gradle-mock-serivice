@file:Suppress("OPT_IN_USAGE")

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION as KOTLIN_VERSION

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.dokka)
    alias(libs.plugins.gitSemVer)
    alias(libs.plugins.gradlePluginPublish)
    alias(libs.plugins.jacoco.testkit)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.qa)
    alias(libs.plugins.publishOnCentral)
    alias(libs.plugins.multiJvmTesting)
    alias(libs.plugins.taskTree)
}

/*
 * Project information
 */
group = "io.github.gciatto"
description = "A Gradle plugin for starting mock services as Gradle tasks"
inner class ProjectInfo {
    val longName = "Gradle Plugin for Mock Service"
    val website = "https://github.com/gciatto/$name"
    val vcsUrl = "$website.git"
    val scm = "scm:git:$website.git"
    val pluginImplementationClass = "$group.gradle.mock.MockServicePlugin" // io.github.gciatto.gradle.mock
    val tags = listOf("mock", "service", "web")
}
val info = ProjectInfo()

gitSemVer {
    buildMetadataSeparator.set("-")
}

repositories {
    mavenCentral()
}

multiJvm {
    maximumSupportedJvmVersion.set(latestJavaSupportedByGradle)
    jvmVersionForCompilation.set(11)
}

dependencies {
    api(gradleApi())
    api(gradleKotlinDsl())
    api(kotlin("stdlib-jdk8"))
    testImplementation(gradleTestKit())
    testImplementation(libs.konf.yaml)
    testImplementation(libs.classgraph)
    testImplementation(libs.bundles.kotlin.testing)
    api(libs.javalin)
}

// Enforce Kotlin version coherence
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin")) {
            useVersion(KOTLIN_VERSION)
            because("All Kotlin modules should use the same version, and compiler uses $KOTLIN_VERSION")
        }
    }
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                allWarningsAsErrors = true
                freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
            }
        }
    }
}

inline fun <reified T : Task> Project.disableTrackStateOnWindows() {
    tasks.withType<T>().configureEach {
        doNotTrackState("Windows is a mess and JaCoCo does not work correctly")
    }
}

if (Os.isFamily(Os.FAMILY_WINDOWS)) {
    disableTrackStateOnWindows<Test>()
    disableTrackStateOnWindows<JacocoReport>()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(tasks.generateJacocoTestKitProperties)
    testLogging {
        showStandardStreams = true
        showCauses = true
        showStackTraces = true
        events(*org.gradle.api.tasks.testing.logging.TestLogEvent.values())
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

signing {
    if (System.getenv()["CI"].equals("true", ignoreCase = true)) {
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

/*
 * Publication on Maven Central and the Plugin portal
 */
publishOnCentral {
    projectLongName.set(info.longName)
    projectDescription.set(description ?: TODO("Missing description"))
    projectUrl.set(info.website)
    scmConnection.set(info.scm)
    repository("https://maven.pkg.github.com/gciatto/${rootProject.name}".lowercase(), name = "github") {
        user.set("gciatto")
        password.set(System.getenv("GITHUB_TOKEN"))
    }
    publishing {
        publications {
            withType<MavenPublication> {
                pom {
                    developers {
                        developer {
                            name.set("Giovanni Ciatto")
                            email.set("danilo.pianini@gmail.com")
                            url.set("https://about.me/gciatto")
                        }
                    }
                }
            }
        }
    }
}

gradlePlugin {
    plugins {
        website.set(info.website)
        vcsUrl.set(info.vcsUrl)
        create("") {
            id = "$group.${project.name}"
            displayName = info.longName
            description = project.description
            implementationClass = info.pluginImplementationClass
            tags.set(info.tags)
        }
    }
}
