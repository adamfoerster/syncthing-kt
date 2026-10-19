package com.adamfoerster.syncthing.core.presentation

import com.adamfoerster.syncthing.core.designsystem.components.SyncStatus
import com.adamfoerster.syncthing.core.domain.model.DeviceRef
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.resources.Res
import com.adamfoerster.syncthing.core.presentation.resources.status_connected
import com.adamfoerster.syncthing.core.presentation.resources.status_offline
import com.adamfoerster.syncthing.core.presentation.resources.status_paused
import com.adamfoerster.syncthing.core.presentation.resources.status_syncing
import com.adamfoerster.syncthing.core.presentation.resources.this_phone
import com.adamfoerster.syncthing.core.presentation.resources.unknown_device

/** The local device is always "Este celular", whatever its configured name. */
fun DeviceRef?.displayName(): UiText = when {
    this == null -> UiText.Resource(Res.string.unknown_device)
    isLocal -> UiText.Resource(Res.string.this_phone)
    else -> UiText.DynamicString(name)
}

fun DeviceStatus.toSyncStatus(): SyncStatus = when (this) {
    DeviceStatus.CONNECTED -> SyncStatus.Connected
    DeviceStatus.SYNCING -> SyncStatus.Syncing
    DeviceStatus.DISCONNECTED -> SyncStatus.Offline
    DeviceStatus.PAUSED -> SyncStatus.Paused
}

fun DeviceStatus.label(): UiText = UiText.Resource(
    when (this) {
        DeviceStatus.CONNECTED -> Res.string.status_connected
        DeviceStatus.SYNCING -> Res.string.status_syncing
        DeviceStatus.DISCONNECTED -> Res.string.status_offline
        DeviceStatus.PAUSED -> Res.string.status_paused
    },
)

/** First letter used in avatars ("N" for "Notebook da Ana"). */
fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"
