package com.adamfoerster.meshsync.desktop.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI

/** The two REST calls the process supervisor needs. An interface so tests need no real core. */
interface CoreApi {
    /** True once `/rest/noauth/health` answers 200. */
    fun isHealthy(): Boolean

    /** Asks the core to exit cleanly (flushes its database); throws if it can't be reached. */
    fun requestShutdown()
}

class HttpCoreApi(private val config: DesktopCoreConfig) : CoreApi {
    override fun isHealthy(): Boolean = try {
        val connection = URI.create("${config.baseUrl}/rest/noauth/health").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 1_000
        connection.readTimeout = 1_000
        try {
            connection.responseCode == 200
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        // "Connection refused" is expected while the core boots.
        false
    }

    override fun requestShutdown() {
        val connection = URI.create("${config.baseUrl}/rest/system/shutdown").toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 2_000
            connection.readTimeout = 5_000
            connection.setRequestProperty("X-API-Key", config.apiKey)
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("POST /rest/system/shutdown: HTTP $code")
        } finally {
            connection.disconnect()
        }
    }
}
