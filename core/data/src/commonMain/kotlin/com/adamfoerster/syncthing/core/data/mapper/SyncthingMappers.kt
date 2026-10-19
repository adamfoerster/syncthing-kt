package com.adamfoerster.syncthing.core.data.mapper

import com.adamfoerster.syncthing.core.data.dto.CompletionDto
import com.adamfoerster.syncthing.core.data.dto.ConnectionDto
import com.adamfoerster.syncthing.core.data.dto.DbFileDto
import com.adamfoerster.syncthing.core.data.dto.DeviceConfigDto
import com.adamfoerster.syncthing.core.data.dto.DeviceStatsDto
import com.adamfoerster.syncthing.core.data.dto.FolderConfigDto
import com.adamfoerster.syncthing.core.data.dto.FolderStatusDto
import com.adamfoerster.syncthing.core.data.dto.PendingDeviceDto
import com.adamfoerster.syncthing.core.data.dto.PendingFolderDto
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderState
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.PendingDevice
import com.adamfoerster.syncthing.core.domain.model.PendingFolder
import kotlin.time.Instant

/** Syncthing uses the zero time (`0001-01-01T00:00:00Z`) and the Unix epoch for "never". */
fun String?.toInstantOrNull(): Instant? {
    if (isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(this) }.getOrNull() ?: return null
    return instant.takeIf { it.epochSeconds > 0 }
}

fun FolderStatusDto.toFolderStatus(paused: Boolean): FolderStatus = FolderStatus(
    state = when {
        paused -> FolderState.PAUSED
        else -> when (state) {
            "idle" -> if (pullErrors > 0) FolderState.ERROR else FolderState.IDLE
            "scanning", "scan-waiting" -> FolderState.SCANNING
            "syncing", "sync-waiting", "sync-preparing", "cleaning", "clean-waiting" -> FolderState.SYNCING
            "error" -> FolderState.ERROR
            else -> FolderState.UNKNOWN
        }
    },
    globalBytes = globalBytes,
    localBytes = localBytes,
    needBytes = needBytes,
    needFiles = needFiles,
    sequence = sequence,
    error = error?.takeIf { it.isNotBlank() },
)

fun FolderConfigDto.toFolder(status: FolderStatus, myId: DeviceId): Folder = Folder(
    id = id,
    label = label.ifBlank { id },
    path = path,
    type = FolderType.fromApi(type),
    paused = paused,
    // The core lists the local device among a folder's devices; screens only care about the others.
    deviceIds = devices.map { DeviceId(it.deviceId) }.filter { it != myId },
    status = status,
)

fun DeviceConfigDto.toDevice(
    connection: ConnectionDto?,
    completion: CompletionDto?,
    stats: DeviceStatsDto?,
    folderIds: List<String>,
): Device {
    val percent = completion?.completion
    return Device(
        id = DeviceId(deviceId),
        name = name.ifBlank { DeviceId(deviceId).short },
        paused = paused,
        status = when {
            paused -> DeviceStatus.PAUSED
            connection?.connected != true -> DeviceStatus.DISCONNECTED
            percent != null && percent < 100.0 -> DeviceStatus.SYNCING
            else -> DeviceStatus.CONNECTED
        },
        completion = percent,
        lastSeen = stats?.lastSeen.toInstantOrNull(),
        folderIds = folderIds,
    )
}

fun Map.Entry<String, PendingDeviceDto>.toPendingDevice(): PendingDevice = PendingDevice(
    id = DeviceId(key),
    name = value.name,
    address = value.address,
    time = value.time.toInstantOrNull(),
)

fun Map.Entry<String, PendingFolderDto>.toPendingFolders(deviceName: (DeviceId) -> String): List<PendingFolder> =
    value.offeredBy.map { (deviceId, offer) ->
        val id = DeviceId(deviceId)
        PendingFolder(
            folderId = key,
            label = offer.label.ifBlank { key },
            offeredBy = id,
            offeredByName = deviceName(id),
            time = offer.time.toInstantOrNull(),
        )
    }

fun DbFileDto.toFileInfo(path: String): FileInfo {
    val entry = global ?: local
    return FileInfo(
        path = path,
        size = entry?.size ?: 0,
        modified = entry?.modified.toInstantOrNull(),
        modifiedBy = entry?.modifiedBy?.takeIf { it.isNotBlank() },
    )
}
