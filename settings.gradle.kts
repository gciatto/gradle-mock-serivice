plugins {
    id("com.gradle.enterprise") version "3.19.1"
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.0.20"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

fun tag(format: String, vararg properties: String): String =
    format.format(*properties.map { System.getProperty(it) ?: error("Missing property $it") }.toTypedArray())

val ci = !System.getenv("CI").isNullOrBlank()
val ciTag = if (ci) "CI" else "Local"
val osTag = "OS: " + tag("%s (%s) v. %s", "os.name", "os.arch", "os.version")
val jvmTag = "JVM: " + tag("%s v. %s", "java.vm.name", "java.vm.version")

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        tag(ciTag)
        tag(osTag)
        tag(jvmTag)
        publishOnFailure()
        buildScanPublished {
            if (ci) {
                println("::error title=Gradle scan for $osTag, $jvmTag::$buildScanUri")
            }
        }
    }
}

gitHooks {
    preCommit {
        tasks("ktlintCheck")
    }
    commitMsg { conventionalCommits() }
    createHooks(true)
}

rootProject.name = "gradle-mock-service"
