package com.adamfoerster.syncthing.core.domain.model

import kotlin.time.Instant

data class Device(
    val id: DeviceId,
    val name: String,
    val paused: Boolean,
    val status: DeviceStatus,
    /** 0..100 — how much of the shared data this device has; null when unknown. */
    val completion: Double?,
    val lastSeen: Instant?,
    val folderIds: List<String>,
)

enum class DeviceStatus {
    CONNECTED,
    SYNCING,
    DISCONNECTED,
    PAUSED,
}

data class PendingDevice(
    val id: DeviceId,
    val name: String,
    val address: String,
    val time: Instant?,
)

data class PendingFolder(
    val folderId: String,
    val label: String,
    val offeredBy: DeviceId,
    val offeredByName: String,
    val time: Instant?,
)
