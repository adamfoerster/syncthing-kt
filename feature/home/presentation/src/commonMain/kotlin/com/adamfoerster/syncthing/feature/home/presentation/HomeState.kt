package com.adamfoerster.syncthing.feature.home.presentation

import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.UiText

data class HomeState(
    val coreState: CoreState = CoreState.Starting,
    val isLoaded: Boolean = false,
    val conflictFiles: Int = 0,
    val lastSync: UiText? = null,
    val pendingDevices: Int = 0,
    val pendingFolders: Int = 0,
    val devices: List<MapDeviceUi> = emptyList(),
    /** 0..100 while folders are still receiving, null when everything is in sync. */
    val syncPercent: Int? = null,
) {
    val connectedCount: Int get() = devices.count { it.status == DeviceStatus.CONNECTED || it.status == DeviceStatus.SYNCING }

    val headline: Headline
        get() = when {
            coreState != CoreState.Running -> Headline.STOPPED
            conflictFiles > 0 -> Headline.ATTENTION
            syncPercent != null || devices.any { it.status == DeviceStatus.SYNCING } -> Headline.SYNCING
            else -> Headline.UP_TO_DATE
        }
}

enum class Headline { ATTENTION, SYNCING, UP_TO_DATE, STOPPED }

data class MapDeviceUi(
    val id: String,
    val name: String,
    val status: DeviceStatus,
    val completion: Double?,
)

sealed interface HomeAction {
    data object OnResolveConflicts : HomeAction
    data object OnShowSharing : HomeAction
    data object OnOpenDevices : HomeAction
    data object OnOpenFolders : HomeAction
    data object OnStartCore : HomeAction
}
