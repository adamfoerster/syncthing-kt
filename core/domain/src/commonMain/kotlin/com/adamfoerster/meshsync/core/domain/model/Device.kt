package com.adamfoerster.meshsync.core.domain.model

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

/**
 * A folder offer the user declined. The core remembers it (the device's `ignoredFolders`) so it stops being
 * offered, and it can be accepted later like any pending offer.
 */
data class DeclinedFolderOffer(
    val folderId: String,
    val label: String,
    val offeredBy: DeviceId,
    val offeredByName: String,
    val time: Instant?,
)
