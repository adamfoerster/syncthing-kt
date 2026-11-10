package com.adamfoerster.meshsync.desktop.core

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.security.SecureRandom
import java.util.Properties

/**
 * Where the core's REST API listens and the key to call it. Generated once and kept in
 * `core.properties` under [dataDir], so the app never reads Syncthing's config.xml: both values are
 * passed to the core on every start (same idea as the Android `CoreConfig`).
 */
class DesktopCoreConfig(val dataDir: File) {
    private val file = File(dataDir, "core.properties")
    private val props = Properties()

    init {
        dataDir.mkdirs()
        if (file.exists()) file.inputStream().use(props::load)
        var changed = false
        if (props.getProperty(KEY_API).isNullOrBlank()) {
            props.setProperty(KEY_API, randomKey())
            changed = true
        }
        if (props.getProperty(KEY_PORT)?.toIntOrNull() == null) {
            props.setProperty(KEY_PORT, freePort().toString())
            changed = true
        }
        if (changed) save()
    }

    val apiKey: String get() = props.getProperty(KEY_API)

    val port: Int get() = props.getProperty(KEY_PORT).toInt()

    val baseUrl: String get() = "http://127.0.0.1:$port"

    /** Syncthing's own config, database and certificates. Kept apart from this file. */
    val syncthingHome: File get() = File(dataDir, "syncthing")

    /** PID of the core we started, to clean up after a crash of the app. */
    val pidFile: File get() = File(dataDir, "syncthing.pid")

    /** True after the user chose "pause": the app then doesn't start the core on its own. */
    var stoppedByUser: Boolean
        get() = props.getProperty(KEY_STOPPED) == "true"
        set(value) {
            props.setProperty(KEY_STOPPED, value.toString())
            save()
        }

    @Synchronized
    private fun save() {
        file.outputStream().use { props.store(it, "MeshSync core settings") }
    }

    private fun randomKey(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..32).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun freePort(): Int {
        val random = SecureRandom()
        repeat(20) {
            val candidate = 21_000 + random.nextInt(20_000)
            val free = runCatching { ServerSocket(candidate, 1, InetAddress.getLoopbackAddress()).close() }.isSuccess
            if (free) return candidate
        }
        return 28_384
    }

    private companion object {
        const val KEY_API = "apiKey"
        const val KEY_PORT = "port"
        const val KEY_STOPPED = "stoppedByUser"
    }
}
