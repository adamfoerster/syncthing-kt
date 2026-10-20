package com.adamfoerster.syncthing.core.data.network

import com.adamfoerster.syncthing.core.data.dto.CompletionDto
import com.adamfoerster.syncthing.core.data.dto.ConfigDto
import com.adamfoerster.syncthing.core.data.dto.ConnectionsDto
import com.adamfoerster.syncthing.core.data.dto.DbFileDto
import com.adamfoerster.syncthing.core.data.dto.DeviceStatsDto
import com.adamfoerster.syncthing.core.data.dto.EventDto
import com.adamfoerster.syncthing.core.data.dto.FolderStatsDto
import com.adamfoerster.syncthing.core.data.dto.FolderStatusDto
import com.adamfoerster.syncthing.core.data.dto.IgnoresDto
import com.adamfoerster.syncthing.core.data.dto.NewDeviceDto
import com.adamfoerster.syncthing.core.data.dto.NewFolderDto
import com.adamfoerster.syncthing.core.data.dto.PendingDeviceDto
import com.adamfoerster.syncthing.core.data.dto.PendingFolderDto
import com.adamfoerster.syncthing.core.data.dto.SetIgnoresDto
import com.adamfoerster.syncthing.core.data.dto.SystemStatusDto
import com.adamfoerster.syncthing.core.data.dto.SystemVersionDto
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.encodeURLPathPart
import kotlinx.serialization.json.JsonObject

/** Thin wrapper over the Syncthing REST API (https://docs.syncthing.net/dev/rest.html). */
class SyncthingApi(private val client: HttpClient) {

    suspend fun systemStatus(): Result<SystemStatusDto, DataError.Remote> =
        safeCall { client.get("rest/system/status") }

    suspend fun version(): Result<SystemVersionDto, DataError.Remote> =
        safeCall { client.get("rest/system/version") }

    suspend fun config(): Result<ConfigDto, DataError.Remote> =
        safeCall { client.get("rest/config") }

    suspend fun connections(): Result<ConnectionsDto, DataError.Remote> =
        safeCall { client.get("rest/system/connections") }

    suspend fun folderStatus(folderId: String): Result<FolderStatusDto, DataError.Remote> =
        safeCall { client.get("rest/db/status") { parameter("folder", folderId) } }

    suspend fun deviceCompletion(deviceId: String): Result<CompletionDto, DataError.Remote> =
        safeCall { client.get("rest/db/completion") { parameter("device", deviceId) } }

    suspend fun deviceStats(): Result<Map<String, DeviceStatsDto>, DataError.Remote> =
        safeCall { client.get("rest/stats/device") }

    suspend fun folderStats(): Result<Map<String, FolderStatsDto>, DataError.Remote> =
        safeCall { client.get("rest/stats/folder") }

    suspend fun pendingDevices(): Result<Map<String, PendingDeviceDto>, DataError.Remote> =
        safeCall { client.get("rest/cluster/pending/devices") }

    suspend fun pendingFolders(): Result<Map<String, PendingFolderDto>, DataError.Remote> =
        safeCall { client.get("rest/cluster/pending/folders") }

    suspend fun dismissPendingDevice(deviceId: String): EmptyResult<DataError.Remote> =
        safeCall { client.delete("rest/cluster/pending/devices") { parameter("device", deviceId) } }

    suspend fun dismissPendingFolder(folderId: String, deviceId: String): EmptyResult<DataError.Remote> =
        safeCall {
            client.delete("rest/cluster/pending/folders") {
                parameter("folder", folderId)
                parameter("device", deviceId)
            }
        }

    suspend fun addDevice(device: NewDeviceDto): EmptyResult<DataError.Remote> =
        safeCall { client.post("rest/config/devices") { setBody(device) } }

    suspend fun patchDevice(deviceId: String, patch: JsonObject): EmptyResult<DataError.Remote> =
        safeCall { client.patch("rest/config/devices/${deviceId.encodeURLPathPart()}") { setBody(patch) } }

    suspend fun deleteDevice(deviceId: String): EmptyResult<DataError.Remote> =
        safeCall { client.delete("rest/config/devices/${deviceId.encodeURLPathPart()}") }

    suspend fun addFolder(folder: NewFolderDto): EmptyResult<DataError.Remote> =
        safeCall { client.post("rest/config/folders") { setBody(folder) } }

    suspend fun patchFolder(folderId: String, patch: JsonObject): EmptyResult<DataError.Remote> =
        safeCall { client.patch("rest/config/folders/${folderId.encodeURLPathPart()}") { setBody(patch) } }

    suspend fun ignores(folderId: String): Result<IgnoresDto, DataError.Remote> =
        safeCall { client.get("rest/db/ignores") { parameter("folder", folderId) } }

    suspend fun setIgnores(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> =
        safeCall {
            client.post("rest/db/ignores") {
                parameter("folder", folderId)
                setBody(SetIgnoresDto(lines))
            }
        }

    suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> =
        safeCall {
            client.post("rest/db/scan") {
                parameter("folder", folderId)
                if (subPath != null) parameter("sub", subPath)
            }
        }

    suspend fun file(folderId: String, path: String): Result<DbFileDto, DataError.Remote> =
        safeCall {
            client.get("rest/db/file") {
                parameter("folder", folderId)
                parameter("file", path)
            }
        }

    /** Long-poll: returns as soon as there are events after [since], or after the timeout. */
    suspend fun events(since: Long): Result<List<EventDto>, DataError.Remote> =
        safeCall {
            client.get("rest/events") {
                parameter("since", since)
                parameter("timeout", EVENT_POLL_TIMEOUT_S)
            }
        }

    companion object {
        const val EVENT_POLL_TIMEOUT_S = 30
    }
}
