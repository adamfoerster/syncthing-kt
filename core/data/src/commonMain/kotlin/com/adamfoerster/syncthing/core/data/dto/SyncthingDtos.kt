package com.adamfoerster.syncthing.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Only the fields the app reads. Unknown keys are ignored, so new core versions don't break parsing.

@Serializable
data class SystemStatusDto(
    @SerialName("myID") val myId: String,
)

@Serializable
data class SystemVersionDto(
    val version: String = "",
)

@Serializable
data class ConfigDto(
    val folders: List<FolderConfigDto> = emptyList(),
    val devices: List<DeviceConfigDto> = emptyList(),
)

@Serializable
data class FolderConfigDto(
    val id: String,
    val label: String = "",
    val path: String = "",
    val type: String = "sendreceive",
    val paused: Boolean = false,
    val devices: List<FolderDeviceDto> = emptyList(),
)

@Serializable
data class FolderDeviceDto(
    @SerialName("deviceID") val deviceId: String,
)

@Serializable
data class DeviceConfigDto(
    @SerialName("deviceID") val deviceId: String,
    val name: String = "",
    val paused: Boolean = false,
)

/** Reply of `GET /rest/db/ignores`; `ignore` is null when the folder has no `.stignore`. */
@Serializable
data class IgnoresDto(
    val ignore: List<String>? = null,
)

/** Body for `POST /rest/db/ignores`. */
@Serializable
data class SetIgnoresDto(
    val ignore: List<String>,
)

/** Body for `POST /rest/config/devices`; omitted fields are taken from the core's defaults. */
@Serializable
data class NewDeviceDto(
    @SerialName("deviceID") val deviceId: String,
    val name: String,
)

/** Body for `POST /rest/config/folders`; omitted fields are taken from the core's defaults. */
@Serializable
data class NewFolderDto(
    val id: String,
    val label: String,
    val path: String,
    val type: String,
    val devices: List<FolderDeviceDto>,
)

@Serializable
data class ConnectionsDto(
    val connections: Map<String, ConnectionDto> = emptyMap(),
)

@Serializable
data class ConnectionDto(
    val connected: Boolean = false,
    val paused: Boolean = false,
    val address: String = "",
)

@Serializable
data class FolderStatusDto(
    val globalBytes: Long = 0,
    val localBytes: Long = 0,
    val needBytes: Long = 0,
    val needFiles: Long = 0,
    val state: String = "",
    val sequence: Long = 0,
    val error: String? = null,
    val pullErrors: Int = 0,
)

@Serializable
data class CompletionDto(
    val completion: Double = 100.0,
    val needBytes: Long = 0,
)

@Serializable
data class DeviceStatsDto(
    val lastSeen: String? = null,
)

@Serializable
data class FolderStatsDto(
    val lastFile: LastFileDto? = null,
)

@Serializable
data class LastFileDto(
    val at: String? = null,
    val filename: String? = null,
)

@Serializable
data class PendingDeviceDto(
    val time: String? = null,
    val name: String = "",
    val address: String = "",
)

@Serializable
data class PendingFolderDto(
    val offeredBy: Map<String, PendingFolderOfferDto> = emptyMap(),
)

@Serializable
data class PendingFolderOfferDto(
    val time: String? = null,
    val label: String = "",
)

@Serializable
data class DbFileDto(
    val global: DbFileEntryDto? = null,
    val local: DbFileEntryDto? = null,
)

@Serializable
data class DbFileEntryDto(
    val name: String = "",
    val size: Long = 0,
    val modified: String? = null,
    val modifiedBy: String? = null,
)

@Serializable
data class EventDto(
    val id: Long,
    val type: String = "",
)
