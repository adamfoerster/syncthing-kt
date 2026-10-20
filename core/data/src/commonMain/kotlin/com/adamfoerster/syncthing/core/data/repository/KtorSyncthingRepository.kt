package com.adamfoerster.syncthing.core.data.repository

import com.adamfoerster.syncthing.core.data.dto.FolderDeviceDto
import com.adamfoerster.syncthing.core.data.dto.NewDeviceDto
import com.adamfoerster.syncthing.core.data.dto.NewFolderDto
import com.adamfoerster.syncthing.core.data.mapper.toDevice
import com.adamfoerster.syncthing.core.data.mapper.toFileInfo
import com.adamfoerster.syncthing.core.data.mapper.toFolder
import com.adamfoerster.syncthing.core.data.mapper.toFolderStatus
import com.adamfoerster.syncthing.core.data.mapper.toInstantOrNull
import com.adamfoerster.syncthing.core.data.mapper.toPendingDevice
import com.adamfoerster.syncthing.core.data.mapper.toPendingFolders
import com.adamfoerster.syncthing.core.data.network.SyncthingApi
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.domain.util.getOrNull
import com.adamfoerster.syncthing.core.domain.util.map
import com.adamfoerster.syncthing.core.domain.util.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Keeps [overview] current while the core runs: one full snapshot, then a long-poll on
 * `/rest/events` that wakes us up for the next snapshot as soon as anything happens.
 */
class KtorSyncthingRepository(
    private val api: SyncthingApi,
    core: SyncthingCore,
    scope: CoroutineScope,
) : SyncthingRepository {

    private val _overview = MutableStateFlow<SyncOverview?>(null)
    override val overview: StateFlow<SyncOverview?> = _overview.asStateFlow()

    private val refreshLock = Mutex()

    init {
        scope.launch {
            core.state.collectLatest { state ->
                if (state == CoreState.Running) pollLoop() else _overview.value = null
            }
        }
    }

    private suspend fun pollLoop() = coroutineScope {
        var since = 0L
        while (isActive) {
            if (!refreshNow()) {
                delay(RETRY_DELAY_MS)
                continue
            }
            when (val events = api.events(since)) {
                is Result.Success -> {
                    since = events.data.maxOfOrNull { it.id } ?: since
                    // Events come in bursts while syncing; coalesce them into one snapshot.
                    delay(COALESCE_DELAY_MS)
                }
                is Result.Failure -> delay(RETRY_DELAY_MS)
            }
        }
    }

    override suspend fun refresh() {
        refreshNow()
    }

    private suspend fun refreshNow(): Boolean = refreshLock.withLock {
        val snapshot = loadSnapshot() ?: return false
        _overview.value = snapshot
        true
    }

    private suspend fun loadSnapshot(): SyncOverview? = coroutineScope {
        val statusCall = async { api.systemStatus() }
        val versionCall = async { api.version() }
        val configCall = async { api.config() }
        val connectionsCall = async { api.connections() }
        val deviceStatsCall = async { api.deviceStats() }
        val folderStatsCall = async { api.folderStats() }
        val pendingDevicesCall = async { api.pendingDevices() }
        val pendingFoldersCall = async { api.pendingFolders() }

        val myId = DeviceId(statusCall.await().getOrNull()?.myId ?: return@coroutineScope null)
        val config = configCall.await().getOrNull() ?: return@coroutineScope null
        val connections = connectionsCall.await().getOrNull()?.connections.orEmpty()
        val deviceStats = deviceStatsCall.await().getOrNull().orEmpty()

        val folderStatuses = config.folders.map { folder ->
            async {
                folder.id to (api.folderStatus(folder.id).getOrNull()?.toFolderStatus(folder.paused) ?: FolderStatus.Unknown)
            }
        }.awaitAll().toMap()

        val otherDevices = config.devices.filter { it.deviceId != myId.value }
        val completions = otherDevices
            .filter { connections[it.deviceId]?.connected == true }
            .map { device -> async { device.deviceId to api.deviceCompletion(device.deviceId).getOrNull() } }
            .awaitAll()
            .toMap()

        val folders = config.folders.map { it.toFolder(folderStatuses.getValue(it.id), myId) }
        val devices = otherDevices.map { dto ->
            dto.toDevice(
                connection = connections[dto.deviceId],
                completion = completions[dto.deviceId],
                stats = deviceStats[dto.deviceId],
                folderIds = folders.filter { folder -> folder.deviceIds.any { it.value == dto.deviceId } }.map { it.id },
            )
        }
        val myName = config.devices.firstOrNull { it.deviceId == myId.value }?.name.orEmpty()
        val nameOf = { id: DeviceId -> devices.firstOrNull { it.id == id }?.name ?: id.short }

        SyncOverview(
            myId = myId,
            myName = myName,
            version = versionCall.await().getOrNull()?.version.orEmpty(),
            devices = devices,
            folders = folders,
            pendingDevices = pendingDevicesCall.await().getOrNull().orEmpty().map { it.toPendingDevice() },
            pendingFolders = pendingFoldersCall.await().getOrNull().orEmpty().flatMap { it.toPendingFolders(nameOf) },
            lastSync = folderStatsCall.await().getOrNull().orEmpty().values
                .mapNotNull { it.lastFile?.at.toInstantOrNull() }
                .maxOrNull(),
        )
    }

    override suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> =
        api.addDevice(NewDeviceDto(id.value, name)).refreshOnSuccess()

    override suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote> =
        api.deleteDevice(id.value).refreshOnSuccess()

    override suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote> =
        api.patchDevice(id.value, buildJsonObject { put("paused", paused) }).refreshOnSuccess()

    override suspend fun renameDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> =
        api.patchDevice(id.value, buildJsonObject { put("name", name) }).refreshOnSuccess()

    override suspend fun dismissPendingDevice(id: DeviceId): EmptyResult<DataError.Remote> =
        api.dismissPendingDevice(id.value).refreshOnSuccess()

    override suspend fun addFolder(
        id: String,
        label: String,
        path: String,
        type: FolderType,
        devices: List<DeviceId>,
    ): EmptyResult<DataError.Remote> {
        val myId = _overview.value?.myId
        val members = (listOfNotNull(myId) + devices).distinct().map { FolderDeviceDto(it.value) }
        return api.addFolder(NewFolderDto(id, label, path, type.apiValue, members)).refreshOnSuccess()
    }

    override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote> {
        val myId = _overview.value?.myId
        val patch = buildJsonObject {
            put("devices", buildJsonArray {
                (listOfNotNull(myId) + devices).distinct().forEach {
                    add(buildJsonObject { put("deviceID", JsonPrimitive(it.value)) })
                }
            })
        }
        return api.patchFolder(folderId, patch).refreshOnSuccess()
    }

    override suspend fun setFolderPaused(folderId: String, paused: Boolean): EmptyResult<DataError.Remote> =
        api.patchFolder(folderId, buildJsonObject { put("paused", paused) }).refreshOnSuccess()

    override suspend fun dismissPendingFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote> =
        api.dismissPendingFolder(folderId, device.value).refreshOnSuccess()

    override suspend fun setMyName(name: String): EmptyResult<DataError.Remote> {
        val myId = _overview.value?.myId ?: return Result.Failure(DataError.Remote.CORE_UNAVAILABLE)
        return api.patchDevice(myId.value, buildJsonObject { put("name", name) }).refreshOnSuccess()
    }

    override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> =
        api.scan(folderId, subPath)

    override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> =
        api.ignores(folderId).map { it.ignore.orEmpty() }

    override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> =
        api.setIgnores(folderId, lines)

    override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> =
        api.file(folderId, path).map { it.toFileInfo(path) }

    private suspend fun EmptyResult<DataError.Remote>.refreshOnSuccess(): EmptyResult<DataError.Remote> =
        onSuccess { refreshNow() }

    private companion object {
        const val RETRY_DELAY_MS = 2_000L
        const val COALESCE_DELAY_MS = 500L
    }
}
