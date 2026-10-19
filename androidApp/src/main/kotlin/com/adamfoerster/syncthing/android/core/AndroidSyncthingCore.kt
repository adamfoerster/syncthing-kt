package com.adamfoerster.syncthing.android.core

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.adamfoerster.syncthing.android.R
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Runs the Syncthing Go binary as a child process, the same way syncthing-android does.
 * [start] brings up [SyncthingService] (a foreground service keeps the process alive), which in
 * turn calls [launch]; the state turns [CoreState.Running] once the REST API answers.
 */
class AndroidSyncthingCore(
    private val context: Context,
    private val config: CoreConfig,
    private val scope: CoroutineScope,
) : SyncthingCore {

    private val _state = MutableStateFlow<CoreState>(CoreState.Stopped)
    override val state: StateFlow<CoreState> = _state.asStateFlow()

    @Volatile private var process: Process? = null
    @Volatile private var stopping = false
    private var job: Job? = null
    private val recentOutput = ArrayDeque<String>()

    private val binary: File get() = File(context.applicationInfo.nativeLibraryDir, BINARY_NAME)

    fun hasStorageAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    override fun start() {
        config.stoppedByUser = false
        if (job?.isActive == true) return
        _state.value = when {
            !binary.exists() -> CoreState.BinaryMissing
            !hasStorageAccess() -> CoreState.StoragePermissionMissing
            else -> {
                ContextCompat.startForegroundService(context, Intent(context, SyncthingService::class.java))
                CoreState.Starting
            }
        }
    }

    /** Starts automatically unless the user stopped it; called whenever the UI comes to the front. */
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

    private fun shutdown() {
        stopping = true
        // SIGTERM: the core flushes its database and exits cleanly.
        process?.destroy()
        scope.launch {
            delay(STOP_GRACE_MS)
            process?.destroyForcibly()
        }
        context.stopService(Intent(context, SyncthingService::class.java))
        if (process == null) _state.value = CoreState.Stopped
    }

    /** Called by [SyncthingService] once it is in the foreground. */
    internal fun launch() {
        if (job?.isActive == true) return
        stopping = false
        job = scope.launch(Dispatchers.IO) { runProcess() }
    }

    private suspend fun runProcess() {
        val home = File(context.filesDir, "syncthing").apply { mkdirs() }
        val command = listOf(
            binary.path, "serve",
            "--no-browser",
            "--gui-address=${config.baseUrl}",
            "--gui-apikey=${config.apiKey}",
        )
        val builder = ProcessBuilder(command).redirectErrorStream(true)
        builder.environment().apply {
            put("HOME", Environment.getExternalStorageDirectory().absolutePath)
            put("STHOMEDIR", home.absolutePath)
            put("STNOUPGRADE", "1")
            put("STMONITORED", "1")
            put("STNODEFAULTFOLDER", "1")
            put("SQLITE_TMPDIR", context.cacheDir.absolutePath)
        }

        val multicast = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("syncthing").apply { setReferenceCounted(false) }
        val started = try {
            builder.start()
        } catch (e: Exception) {
            Log.e(TAG, "could not start core", e)
            _state.value = CoreState.Failed(e.message ?: e.toString())
            context.stopService(Intent(context, SyncthingService::class.java))
            return
        }
        process = started
        multicast.acquire()

        val output = scope.launch(Dispatchers.IO) {
            started.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    Log.i(TAG, line)
                    synchronized(recentOutput) {
                        recentOutput.addLast(line)
                        if (recentOutput.size > OUTPUT_LINES) recentOutput.removeFirst()
                    }
                }
            }
        }
        val health = scope.launch(Dispatchers.IO) { awaitHealthy() }

        val exitCode = runInterruptible { started.waitFor() }
        health.cancel()
        output.join()
        multicast.release()
        process = null

        _state.value = if (stopping) {
            CoreState.Stopped
        } else {
            val tail = synchronized(recentOutput) { recentOutput.takeLast(3).joinToString("\n") }
            CoreState.Failed(context.getString(R.string.core_exit, exitCode) + if (tail.isNotEmpty()) "\n$tail" else "")
        }
        context.stopService(Intent(context, SyncthingService::class.java))
    }

    private suspend fun awaitHealthy() {
        val healthy = withTimeoutOrNull(START_TIMEOUT_MS) {
            while (isActive) {
                if (isHealthy()) return@withTimeoutOrNull true
                delay(500)
            }
            false
        } == true
        if (healthy) {
            _state.value = CoreState.Running
        } else if (process != null && !stopping) {
            _state.value = CoreState.Failed(context.getString(R.string.core_start_timeout))
        }
    }

    private fun isHealthy(): Boolean = try {
        val connection = URL("${config.baseUrl}/rest/noauth/health").openConnection() as HttpURLConnection
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        try {
            connection.responseCode == 200
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        false
    }

    private companion object {
        const val TAG = "SyncthingCore"
        const val BINARY_NAME = "libsyncthing.so"
        const val OUTPUT_LINES = 50
        val START_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(90)
        val STOP_GRACE_MS = TimeUnit.SECONDS.toMillis(10)
    }
}
