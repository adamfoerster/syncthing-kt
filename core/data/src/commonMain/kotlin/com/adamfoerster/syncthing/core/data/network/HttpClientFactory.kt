package com.adamfoerster.syncthing.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun defaultHttpEngine(): HttpClientEngine

object HttpClientFactory {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    fun create(endpoint: SyncthingEndpoint, engine: HttpClientEngine = defaultHttpEngine()): HttpClient =
        HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                connectTimeoutMillis = 5_000
                // Event long-polls hold the request open for up to EVENT_POLL_TIMEOUT_S.
                requestTimeoutMillis = (SyncthingApi.EVENT_POLL_TIMEOUT_S + 15) * 1_000L
            }
            defaultRequest {
                url(endpoint.baseUrl.trimEnd('/') + "/")
                headers.append("X-API-Key", endpoint.apiKey)
                contentType(ContentType.Application.Json)
            }
        }
}
