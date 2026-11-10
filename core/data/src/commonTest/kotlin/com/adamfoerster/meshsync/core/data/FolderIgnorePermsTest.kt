package com.adamfoerster.meshsync.core.data

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.adamfoerster.meshsync.core.data.dto.FolderDeviceDto
import com.adamfoerster.meshsync.core.data.dto.NewFolderDto
import com.adamfoerster.meshsync.core.data.network.HttpClientFactory
import com.adamfoerster.meshsync.core.data.network.SyncthingApi
import com.adamfoerster.meshsync.core.data.network.SyncthingEndpoint
import com.adamfoerster.meshsync.core.data.repository.FolderPermissionsMigrator
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FolderIgnorePermsTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) =
        SyncthingApi(HttpClientFactory.create(SyncthingEndpoint("http://127.0.0.1:8384", "key"), engine))

    private val config = """{"folders":[
        {"id":"off","ignorePerms":false,"devices":[]},
        {"id":"on","ignorePerms":true,"devices":[]},
        {"id":"missing","devices":[]}
    ],"devices":[]}"""

    @Test
    fun newFoldersAreCreatedWithIgnorePermsOn() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("", HttpStatusCode.OK)
        }
        api(engine).addFolder(NewFolderDto("f", "F", "/p", "sendreceive", true, listOf(FolderDeviceDto("D"))))
        assertThat(body).contains("\"ignorePerms\":true")
    }

    @Test
    fun onlyFoldersWithIgnorePermsOffArePatched() = runTest {
        val patched = mutableListOf<String>()
        val bodies = mutableListOf<String>()
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Patch) {
                patched += request.url.encodedPath
                bodies += request.body.toByteArray().decodeToString()
                respond("", HttpStatusCode.OK)
            } else {
                respond(config, HttpStatusCode.OK, json)
            }
        }
        val result = FolderPermissionsMigrator(api(engine)).run()

        assertThat(result).containsExactly("off")
        assertThat(patched).containsExactly("/rest/config/folders/off")
        assertThat(bodies).containsExactly("""{"ignorePerms":true}""")
    }

    @Test
    fun aFolderIsNotRetriedAfterItWasAttempted() = runTest {
        var patches = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Patch) {
                patches++
                respond("nope", HttpStatusCode.BadRequest)
            } else {
                respond(config, HttpStatusCode.OK, json)
            }
        }
        val migrator = FolderPermissionsMigrator(api(engine))

        assertThat(migrator.run()).isEmpty() // the patch failed, so nothing is reported as fixed
        migrator.run()
        assertThat(patches).isEqualTo(1)
    }
}
