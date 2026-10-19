package com.adamfoerster.syncthing.android.core

import android.content.Context
import androidx.core.content.edit
import java.net.InetAddress
import java.net.ServerSocket
import java.security.SecureRandom

/**
 * Where the core's REST API listens and the key to call it. Generated once and kept, so the
 * app never has to read Syncthing's config.xml: both are passed to the core on every start.
 */
class CoreConfig(context: Context) {
    private val prefs = context.getSharedPreferences("core", Context.MODE_PRIVATE)

    val apiKey: String = prefs.getString(KEY_API, null) ?: randomKey().also { key -> prefs.edit { putString(KEY_API, key) } }

    /** A port above the usual 8384, so another Syncthing app on the phone doesn't clash with us. */
    val port: Int = prefs.getInt(KEY_PORT, 0).takeIf { it > 0 } ?: freePort().also { port -> prefs.edit { putInt(KEY_PORT, port) } }

    val baseUrl: String get() = "http://127.0.0.1:$port"

    /** True after the user pressed "stop": the app then doesn't start the core on its own. */
    var stoppedByUser: Boolean
        get() = prefs.getBoolean(KEY_STOPPED, false)
        set(value) = prefs.edit { putBoolean(KEY_STOPPED, value) }

    /** True once this device got the phone's model as its name (see AndroidSyncthingCore). */
    var deviceNamed: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_NAMED, false)
        set(value) = prefs.edit { putBoolean(KEY_DEVICE_NAMED, value) }

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
        const val KEY_DEVICE_NAMED = "deviceNamed"
    }
}
