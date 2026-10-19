package com.adamfoerster.syncthing.core.domain.model

import kotlin.time.Instant

/** Everything the screens need about the local Syncthing instance, refreshed as events arrive. */
data class SyncOverview(
    val myId: DeviceId,
    val myName: String,
    val version: String,
    val devices: List<Device>,
    val folders: List<Folder>,
    val pendingDevices: List<PendingDevice>,
    val pendingFolders: List<PendingFolder>,
    val lastSync: Instant?,
)

/** Resolves a short device ID (as found in conflict names and `modifiedBy`) to a known device. */
fun SyncOverview.deviceRef(shortId: String?): DeviceRef? = when (shortId) {
    null -> null
    myId.short -> DeviceRef(myId, myName, isLocal = true)
    else -> devices.firstOrNull { it.id.short == shortId }?.let { DeviceRef(it.id, it.name, isLocal = false) }
}

data class DeviceRef(
    val id: DeviceId,
    val name: String,
    val isLocal: Boolean,
)
