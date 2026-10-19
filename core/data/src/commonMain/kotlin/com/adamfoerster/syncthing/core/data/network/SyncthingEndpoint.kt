package com.adamfoerster.syncthing.core.data.network

/**
 * Where the local core's REST API listens. The platform launches the core with these values
 * (`--gui-address`, `--gui-apikey`), so the app never has to parse Syncthing's config.xml.
 */
data class SyncthingEndpoint(
    val baseUrl: String,
    val apiKey: String,
)
