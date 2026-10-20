package com.adamfoerster.syncthing.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.adamfoerster.syncthing.core.data.network.HttpClientFactory
import com.adamfoerster.syncthing.core.data.network.SyncthingApi
import com.adamfoerster.syncthing.core.data.network.SyncthingEndpoint
import com.adamfoerster.syncthing.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test

class SyncthingApiPatchDeviceTest {

    @Test
    fun renamingPatchesOnlyTheNameOfThatDevice() = runTest {
        var method: HttpMethod? = null
        var path = ""
        var body = ""
        val engine = MockEngine { request ->
            method = request.method
            path = request.url.encodedPath
            body = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.OK)
        }
        val api = SyncthingApi(HttpClientFactory.create(SyncthingEndpoint("http://127.0.0.1:8384", "key"), engine))

        val result = api.patchDevice("AAAAAAA-BBBBBBB", buildJsonObject { put("name", "Notebook da Ana") })

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(method).isEqualTo(HttpMethod.Patch)
        assertThat(path).isEqualTo("/rest/config/devices/AAAAAAA-BBBBBBB")
        assertThat(body).isEqualTo("""{"name":"Notebook da Ana"}""")
    }
}
