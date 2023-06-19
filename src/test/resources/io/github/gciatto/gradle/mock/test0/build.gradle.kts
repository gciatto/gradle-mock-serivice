plugins {
    id("io.github.gciatto.gradle-mock-service")
    java
}

mockService {
    port = 8080
    wrapTasks("test")
    routes {
        get("/hello") { it.result("hello") }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

repositories {
    mavenCentral()
}
