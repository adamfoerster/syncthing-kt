package com.adamfoerster.meshsync.desktop.core

import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Starts the core as a child process. A seam so tests can hand back a fake [Process]. */
fun interface ProcessLauncher {
    fun launch(command: List<String>, environment: Map<String, String>): Process

    companion object {
        val System = ProcessLauncher { command, environment ->
            ProcessBuilder(command).redirectErrorStream(true).apply { environment().putAll(environment) }.start()
        }
    }
}

/** User-visible failure texts; the host replaces the English defaults with translated ones. */
class CoreMessages(
    val exited: (exitCode: Int) -> String = { "Syncthing stopped unexpectedly (exit code $it)" },
    val startTimeout: String = "Syncthing did not start in time",
)

/**
 * Runs `syncthing.exe` as a child process of the tray app, the desktop counterpart of
 * `AndroidSyncthingCore`: no service is installed, the app supervises the core. The state turns
 * [CoreState.Running] once the REST API answers, and the core is restarted when it asks for it
 * (exit code 3), since there is no monitor process (`STMONITORED=1`).
 */
class DesktopSyncthingCore(
    private val binary: File,
    private val config: DesktopCoreConfig,
    private val scope: CoroutineScope,
    private val api: CoreApi = HttpCoreApi(config),
    private val launcher: ProcessLauncher = ProcessLauncher.System,
    private val messages: CoreMessages = CoreMessages(),
    private val startTimeoutMs: Long = TimeUnit.SECONDS.toMillis(90),
    private val stopGraceMs: Long = TimeUnit.SECONDS.toMillis(10),
    private val healthPollMs: Long = 500,
) : SyncthingCore {

    private val _state = MutableStateFlow<CoreState>(CoreState.Stopped)
    override val state: StateFlow<CoreState> = _state.asStateFlow()

    @Volatile private var process: Process? = null
    @Volatile private var stopping = false
    private var job: Job? = null
    private val recentOutput = ArrayDeque<String>()

    override fun start() {
        config.stoppedByUser = false
        if (job?.isActive == true) return
        if (!binary.exists()) {
            _state.value = CoreState.BinaryMissing
            return
        }
        stopping = false
        _state.value = CoreState.Starting
        job = scope.launch(Dispatchers.IO) { runProcess() }
    }

    /** Starts unless the user paused it; called when the app launches. */
    fun startIfWanted() {
        if (!config.stoppedByUser && _state.value !is CoreState.Running && _state.value != CoreState.Starting) start()
    }

    override fun stop() {
        config.stoppedByUser = true
        shutdown()
    }

    override fun restart() {
        scope.launch {
            shutdown()
            job?.join()
            start()
        }
    }

    /**
     * Ends the core without touching the "paused" choice: used when the whole app quits. Blocks until
     * the process is gone, so the JVM does not exit with it still running.
     */
    fun shutdownAndWait() {
        stopping = true
        val running = process
        if (running != null) terminate(running)
        _state.value = CoreState.Stopped
    }

    private fun shutdown() {
        stopping = true
        val running = process
        if (running == null) {
            job?.cancel()
            _state.value = CoreState.Stopped
            return
        }
        scope.launch(Dispatchers.IO) { terminate(running) }
    }

    private fun terminate(running: Process) {
        // Ask the core to exit so it flushes its database; destroy() is only the fallback.
        val asked = try {
            api.requestShutdown()
            true
        } catch (e: IOException) {
            false
        }
        if (!asked) running.destroy()
        if (!running.waitFor(stopGraceMs, TimeUnit.MILLISECONDS)) running.destroyForcibly()
    }

    private suspend fun runProcess() {
        config.syncthingHome.mkdirs()
        StaleProcess.terminate(config.pidFile)
        val command = listOf(
            binary.path, "serve",
            "--no-browser",
            "--no-console",
            "--gui-address=${config.baseUrl}",
            "--gui-apikey=${config.apiKey}",
        )
        val environment = mapOf(
            "STHOMEDIR" to config.syncthingHome.absolutePath,
            "STNOUPGRADE" to "1",
            // Run the core directly, without its monitor process: the app supervises it instead.
            "STMONITORED" to "1",
        )
        // There's no monitor process to restart the core when it asks for it (POST /rest/system/restart
        // exits with EXIT_RESTART), so do that here.
        while (true) {
            val exitCode = runOnce(command, environment) ?: return
            if (stopping || exitCode != EXIT_RESTART) {
                _state.value = if (stopping) {
                    CoreState.Stopped
                } else {
                    val tail = synchronized(recentOutput) { recentOutput.takeLast(3).joinToString("\n") }
                    CoreState.Failed(messages.exited(exitCode) + if (tail.isNotEmpty()) "\n$tail" else "")
                }
                return
            }
            _state.value = CoreState.Starting
        }
    }

    /** Runs the core until it exits; returns its exit code, or null if it could not be started. */
    private suspend fun runOnce(command: List<String>, environment: Map<String, String>): Int? {
        if (stopping) {
            _state.value = CoreState.Stopped
            return null
        }
        val started = try {
            launcher.launch(command, environment)
        } catch (e: IOException) {
            _state.value = CoreState.Failed(e.message ?: e.toString())
            return null
        }
        process = started
        StaleProcess.write(config.pidFile, started.pid())

        val output = scope.launch(Dispatchers.IO) {
            try {
                started.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(recentOutput) {
                            recentOutput.addLast(line)
                            if (recentOutput.size > OUTPUT_LINES) recentOutput.removeFirst()
                        }
                    }
                }
            } catch (e: IOException) {
                // destroy() closes the stream under the reader when stopping.
            }
        }
        val health = scope.launch(Dispatchers.IO) { awaitHealthy() }

        val exitCode = runInterruptible { started.waitFor() }
        health.cancel()
        output.join()
        process = null
        StaleProcess.clear(config.pidFile)
        return exitCode
    }

    private suspend fun awaitHealthy() {
        val healthy = withTimeoutOrNull(startTimeoutMs) {
            while (isActive) {
                if (api.isHealthy()) return@withTimeoutOrNull true
                delay(healthPollMs)
            }
            false
        } == true
        if (healthy) {
            _state.value = CoreState.Running
        } else if (process != null && !stopping) {
            _state.value = CoreState.Failed(messages.startTimeout)
        }
    }

    private companion object {
        const val OUTPUT_LINES = 50

        /** The core's exit code when it wants to be started again (svcutil.ExitRestart). */
        const val EXIT_RESTART = 3
    }
}
