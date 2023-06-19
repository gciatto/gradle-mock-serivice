package io.github.gciatto.gradle.mock

import io.javalin.Javalin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Extension for configuring the mock service.
 */
open class MockServiceExtension(
    private val project: Project,
    private val startMockTask: Task,
    private val stopMockTask: Task,
) {
    companion object {
        /**
         * Default port for the mock service.
         */
        const val DEFAULT_PORT = 8080
    }

    private var server: Javalin? = null

    @Suppress("NOTHING_TO_INLINE")
    private inline fun errorIfAlreadyStarted() {
        if (server != null) {
            error("Server already started")
        }
    }

    /**
     * The port the mock service shall listen upon.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var port: Int = DEFAULT_PORT
        set(value) {
            errorIfAlreadyStarted()
            field = value
        }

    private var setup: Javalin.() -> Unit = {}
        set(value) {
            errorIfAlreadyStarted()
            field = value
        }

    /**
     * Starts the mock service.
     * Fails if the service is already running.
     */
    fun start() {
        errorIfAlreadyStarted()
        server = Javalin.create {
            it.requestLogger.http { ctx, executionTimeMs ->
                project.logger.lifecycle("${ctx.method()} ${ctx.url()} ... ${ctx.status()} in $executionTimeMs ms")
            }
        }.also {
            it.setup()
        }.start(port)
    }

    /**
     * Stops the mock service.
     * Fails if the service is not running.
     */
    fun stop() {
        server.let {
            require(it != null) { "Server has not started yet" }
            it.stop()
        }
        server = null
    }

    /**
     * Wraps tasks (selected by name) in such a way that the mock service is started before them,
     * and stopped after them.
     */
    fun wrapTasks(name: String, vararg names: String) {
        val tasks = setOf(name, *names)
        project.tasks.matching { it.name in tasks }.configureEach {
            it.dependsOn(startMockTask)
            it.finalizedBy(stopMockTask)
        }
    }

    /**
     * Lets the user configure routes for the mock service.
     */
    fun routes(action: Javalin.() -> Unit) {
        setup = action
    }
}
