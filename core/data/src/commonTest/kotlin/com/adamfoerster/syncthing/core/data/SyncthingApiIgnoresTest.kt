package com.adamfoerster.syncthing.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.adamfoerster.syncthing.core.data.network.HttpClientFactory
import com.adamfoerster.syncthing.core.data.network.SyncthingApi
import com.adamfoerster.syncthing.core.data.network.SyncthingEndpoint
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SyncthingApiIgnoresTest {

    private val json = headersOf(HttpHeaders.ContentType, "application/json")

    private fun api(engine: MockEngine) =
        SyncthingApi(HttpClientFactory.create(SyncthingEndpoint("http://127.0.0.1:8384", "key"), engine))

    @Test
    fun readsTheFoldersIgnoreLines() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/rest/db/ignores")
            assertThat(request.url.parameters["folder"]).isEqualTo("docs")
            respond("""{"ignore":["*.tmp","cache"],"expanded":[]}""", HttpStatusCode.OK, json)
        }
        val result = api(engine).ignores("docs")
        assertThat((result as Result.Success).data.ignore).isEqualTo(listOf("*.tmp", "cache"))
    }

    @Test
    fun aFolderWithoutStignoreComesBackAsNull() = runTest {
        val engine = MockEngine { respond("""{"ignore":null,"expanded":null}""", HttpStatusCode.OK, json) }
        val result = api(engine).ignores("docs")
        assertThat((result as Result.Success).data.ignore).isEqualTo(null)
    }

    @Test
    fun savingPostsTheLinesForThatFolder() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            assertThat(request.method).isEqualTo(HttpMethod.Post)
            assertThat(request.url.parameters["folder"]).isEqualTo("docs")
            body = request.body.toByteArray().decodeToString()
            respond("""{"ignore":["*.tmp"],"expanded":[]}""", HttpStatusCode.OK, json)
        }
        val result = api(engine).setIgnores("docs", listOf("*.tmp", "// note"))
        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(body).isEqualTo("""{"ignore":["*.tmp","// note"]}""")
    }

    @Test
    fun savingAnEmptyListStillSendsTheField() = runTest {
        var body = ""
        val engine = MockEngine { request ->
            body = request.body.toByteArray().decodeToString()
            respond("{}", HttpStatusCode.OK, json)
        }
        api(engine).setIgnores("docs", emptyList())
        assertThat(body).isEqualTo("""{"ignore":[]}""")
    }

    @Test
    fun rejectionIsReportedAsBadRequest() = runTest {
        val engine = MockEngine { respond("nope", HttpStatusCode.BadRequest) }
        assertThat(api(engine).setIgnores("docs", listOf("x"))).isEqualTo(Result.Failure(DataError.Remote.BAD_REQUEST))
    }
}
