package com.adamfoerster.meshsync.desktop.core

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.model.CoreState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DesktopSyncthingCoreTest {
    private lateinit var dir: File
    private lateinit var binary: File
    private lateinit var config: DesktopCoreConfig
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val processes = mutableListOf<FakeProcess>()
    private val launches = AtomicInteger()

    @Volatile private var healthy = false
    @Volatile private var shutdownRequests = 0

    private val api = object : CoreApi {
        override fun isHealthy() = healthy
        override fun requestShutdown() {
            shutdownRequests++
            processes.last().finish(0)
        }
    }

    private fun core(binary: File = this.binary, startTimeoutMs: Long = 5_000, launcher: ProcessLauncher = launcherOf()) =
        DesktopSyncthingCore(
            binary, config, scope, api, launcher,
            startTimeoutMs = startTimeoutMs, stopGraceMs = 1_000, healthPollMs = 10,
        )

    private fun launcherOf(output: String = "") = ProcessLauncher { _, _ ->
        launches.incrementAndGet()
        FakeProcess(output).also { processes += it }
    }

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("meshsync-test").toFile()
        binary = File(dir, "syncthing.exe").apply { writeText("fake") }
        config = DesktopCoreConfig(File(dir, "data"))
    }

    @AfterTest
    fun tearDown() {
        processes.forEach { it.finish(0) }
        scope.cancel()
        dir.deleteRecursively()
    }

    private fun DesktopSyncthingCore.await(predicate: (CoreState) -> Boolean): CoreState =
        runBlocking { withTimeout(5_000) { state.first(predicate) } }

    @Test
    fun `missing binary reports BinaryMissing`() {
        val core = core(binary = File(dir, "nope.exe"))
        core.start()
        assertThat(core.state.value).isEqualTo(CoreState.BinaryMissing)
        assertThat(launches.get()).isEqualTo(0)
    }

    @Test
    fun `becomes Running once the API is healthy`() {
        val core = core()
        core.start()
        core.await { it == CoreState.Starting }
        healthy = true
        core.await { it == CoreState.Running }
    }

    @Test
    fun `starts the core with the generated endpoint and no console`() {
        var command: List<String> = emptyList()
        var environment: Map<String, String> = emptyMap()
        val core = core(launcher = { c, e ->
            command = c
            environment = e
            FakeProcess("").also { processes += it }
        })
        core.start()
        core.await { it == CoreState.Starting }
        runBlocking { withTimeout(5_000) { while (command.isEmpty()) kotlinx.coroutines.delay(10) } }

        assertThat(command).contains("--gui-address=${config.baseUrl}")
        assertThat(command).contains("--gui-apikey=${config.apiKey}")
        assertThat(command).contains("--no-console")
        assertThat(environment["STHOMEDIR"]).isEqualTo(config.syncthingHome.absolutePath)
        assertThat(environment["STMONITORED"]).isEqualTo("1")
    }

    @Test
    fun `unexpected exit is Failed with the last output lines`() {
        val core = core(launcher = launcherOf(output = "one\ntwo\nboom\n"))
        core.start()
        core.await { it == CoreState.Starting }
        runBlocking { withTimeout(5_000) { while (processes.isEmpty()) kotlinx.coroutines.delay(10) } }
        processes.last().finish(1)

        val failed = core.await { it is CoreState.Failed }
        assertThat((failed as CoreState.Failed).message).contains("exit code 1")
        assertThat(failed.message).contains("boom")
    }

    @Test
    fun `exit code 3 restarts the core`() {
        val core = core()
        core.start()
        runBlocking { withTimeout(5_000) { while (processes.isEmpty()) kotlinx.coroutines.delay(10) } }
        processes.first().finish(3)
        runBlocking { withTimeout(5_000) { while (launches.get() < 2) kotlinx.coroutines.delay(10) } }
        assertThat(launches.get()).isEqualTo(2)
    }

    @Test
    fun `stop asks the core to shut down and remembers the choice`() {
        val core = core()
        core.start()
        healthy = true
        core.await { it == CoreState.Running }

        core.stop()

        core.await { it == CoreState.Stopped }
        assertThat(shutdownRequests).isEqualTo(1)
        assertThat(config.stoppedByUser).isTrue()
    }

    @Test
    fun `start clears the paused choice`() {
        config.stoppedByUser = true
        val core = core()
        core.start()
        assertThat(config.stoppedByUser).isFalse()
    }

    @Test
    fun `startIfWanted respects a pause`() {
        config.stoppedByUser = true
        val core = core()
        core.startIfWanted()
        assertThat(core.state.value).isEqualTo(CoreState.Stopped)
        assertThat(launches.get()).isEqualTo(0)
    }

    @Test
    fun `launch failure is reported`() {
        val core = core(launcher = { _, _ -> throw IOException("access denied") })
        core.start()
        val failed = core.await { it is CoreState.Failed }
        assertThat(failed).isInstanceOf(CoreState.Failed::class)
        assertThat((failed as CoreState.Failed).message).contains("access denied")
    }
}

/** A [Process] that "runs" until [finish] is called. */
class FakeProcess(private val output: String) : Process() {
    private val done = CountDownLatch(1)
    @Volatile private var code = 0

    fun finish(exitCode: Int) {
        code = exitCode
        done.countDown()
    }

    override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()
    override fun getInputStream(): InputStream = ByteArrayInputStream(output.toByteArray())
    override fun getErrorStream(): InputStream = ByteArrayInputStream(ByteArray(0))
    override fun waitFor(): Int {
        done.await()
        return code
    }
    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = done.await(timeout, unit)
    override fun exitValue(): Int = if (done.count == 0L) code else throw IllegalThreadStateException()
    override fun destroy() = finish(143)
    override fun pid(): Long = 4242
}
