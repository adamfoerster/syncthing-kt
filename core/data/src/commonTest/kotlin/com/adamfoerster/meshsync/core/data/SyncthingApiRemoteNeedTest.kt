package com.adamfoerster.meshsync.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.adamfoerster.meshsync.core.data.mapper.toPendingFile
import com.adamfoerster.meshsync.core.data.network.HttpClientFactory
import com.adamfoerster.meshsync.core.data.network.SyncthingApi
import com.adamfoerster.meshsync.core.data.network.SyncthingEndpoint
import com.adamfoerster.meshsync.core.domain.model.PendingFile
import com.adamfoerster.meshsync.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncthingApiRemoteNeedTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) =
        SyncthingApi(HttpClientFactory.create(SyncthingEndpoint("http://127.0.0.1:8384", "key"), engine))

    @Test
    fun asksWhatTheDeviceNeedsForThatFolder() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/rest/db/remoteneed")
            assertThat(request.url.parameters["folder"]).isEqualTo("docs")
            assertThat(request.url.parameters["device"]).isEqualTo("DEV")
            assertThat(request.url.parameters["perpage"]).isEqualTo("500")
            respond(
                """{"files":[
                    {"name":"a.txt","size":10,"type":"FILE_INFO_TYPE_FILE","deleted":false},
                    {"name":"dir","size":128,"type":"FILE_INFO_TYPE_DIRECTORY","deleted":true}
                ],"page":1,"perpage":500}""",
                HttpStatusCode.OK,
                json,
            )
        }
        val result = api(engine).remoteNeed("docs", "DEV", 500)
        val files = (result as Result.Success).data.files.orEmpty().map { it.toPendingFile() }
        assertThat(files).isEqualTo(
            listOf(
                PendingFile("a.txt", 10, isDirectory = false, isDeletion = false),
                PendingFile("dir", 128, isDirectory = true, isDeletion = true),
            ),
        )
    }

    @Test
    fun aNullFileListMeansNothingIsPending() = runTest {
        val engine = MockEngine { respond("""{"files":null,"page":1,"perpage":500}""", HttpStatusCode.OK, json) }
        val result = api(engine).remoteNeed("docs", "DEV", 500)
        assertThat((result as Result.Success).data.files.orEmpty()).isEqualTo(emptyList())
    }
}
