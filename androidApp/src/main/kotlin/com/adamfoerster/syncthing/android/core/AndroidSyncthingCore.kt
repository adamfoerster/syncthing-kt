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
import org.json.JSONObject
import java.io.File
import java.io.IOException
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
        context.stopService(Intent(context, SyncthingService::class.java))
        val running = process
        if (running == null) {
            _state.value = CoreState.Stopped
            return
        }
        scope.launch(Dispatchers.IO) {
            // Ask the core to exit so it flushes its database. destroy() is only the fallback: it also
            // closes the core's output pipe, and the core then dies of SIGPIPE while logging its shutdown.
            val asked = try {
                request("POST", "/rest/system/shutdown")
                true
            } catch (e: Exception) {
                false
            }
            if (!asked) running.destroy()
            if (!running.waitFor(STOP_GRACE_MS, TimeUnit.MILLISECONDS)) running.destroyForcibly()
        }
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
            // Run the core directly, without its monitor process: the app supervises it instead.
            // (v2 no longer creates a default folder, so STNODEFAULTFOLDER isn't needed.)
            put("STMONITORED", "1")
            put("SQLITE_TMPDIR", context.cacheDir.absolutePath)
        }

        val multicast = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager)
            .createMulticastLock("syncthing").apply { setReferenceCounted(false) }
        multicast.acquire()
        try {
            // There's no monitor process to restart the core when it asks for it (POST /rest/system/restart
            // exits with EXIT_RESTART), so do that here.
            while (true) {
                val exitCode = runOnce(builder) ?: return
                if (stopping || exitCode != EXIT_RESTART) {
                    _state.value = if (stopping) {
                        CoreState.Stopped
                    } else {
                        val tail = synchronized(recentOutput) { recentOutput.takeLast(3).joinToString("\n") }
                        CoreState.Failed(context.getString(R.string.core_exit, exitCode) + if (tail.isNotEmpty()) "\n$tail" else "")
                    }
                    return
                }
                Log.i(TAG, "core asked for a restart")
                _state.value = CoreState.Starting
            }
        } finally {
            multicast.release()
            context.stopService(Intent(context, SyncthingService::class.java))
        }
    }

    /** Runs the core until it exits; returns its exit code, or null if it could not be started. */
    private suspend fun runOnce(builder: ProcessBuilder): Int? {
        val started = try {
            builder.start()
        } catch (e: Exception) {
            Log.e(TAG, "could not start core", e)
            _state.value = CoreState.Failed(e.message ?: e.toString())
            return null
        }
        process = started

        val output = scope.launch(Dispatchers.IO) {
            try {
                started.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        Log.i(TAG, line)
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
        return exitCode
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
            if (!config.deviceNamed) nameDeviceAfterModel()
            _state.value = CoreState.Running
        } else if (process != null && !stopping) {
            _state.value = CoreState.Failed(context.getString(R.string.core_start_timeout))
        }
    }

    /**
     * The core names a new device after the hostname, which is always "localhost" on Android.
     * Use the phone's model instead, once; a name the user already chose is kept.
     */
    private fun nameDeviceAfterModel() {
        try {
            val myId = JSONObject(request("GET", "/rest/system/status")).getString("myID")
            val path = "/rest/config/devices/$myId"
            val device = JSONObject(request("GET", path))
            if (device.optString("name").let { it.isBlank() || it == ANDROID_HOSTNAME }) {
                device.put("name", Build.MODEL)
                request("PUT", path, device.toString())
            }
            config.deviceNamed = true
        } catch (e: Exception) {
            // Not worth failing the start over; tried again next time.
            Log.w(TAG, "could not set the device name", e)
        }
    }

    private fun request(method: String, path: String, body: String? = null): String {
        val connection = URL(config.baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 2_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("X-API-Key", config.apiKey)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("$method $path: HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
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
        const val ANDROID_HOSTNAME = "localhost"

        /** The core's exit code when it wants to be started again (svcutil.ExitRestart). */
        const val EXIT_RESTART = 3
        val START_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(90)
        val STOP_GRACE_MS = TimeUnit.SECONDS.toMillis(10)
    }
}
