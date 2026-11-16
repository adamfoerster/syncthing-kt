package com.adamfoerster.meshsync.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.model.Device
import com.adamfoerster.meshsync.core.domain.model.DeviceStatus
import com.adamfoerster.meshsync.core.domain.model.Folder
import com.adamfoerster.meshsync.core.domain.model.FolderState
import com.adamfoerster.meshsync.core.domain.repository.ConflictSummary
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.presentation.toRelativeUiText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Overall progress while something is still moving, null when everything is in sync.
 *
 * Local folders come first (we are receiving); otherwise it is the slowest connected device that is still
 * catching up, so the headline matches the device map instead of showing 0% while a peer sits at 99%.
 * Always rounded down: 99.99% must never read as done.
 */
internal fun syncPercentOf(activeFolders: List<Folder>, devices: List<Device>): Int? {
    val global = activeFolders.sumOf { it.status.globalBytes }
    val need = activeFolders.sumOf { it.status.needBytes }
    if (activeFolders.any { it.status.state == FolderState.SYNCING } || need > 0) {
        return if (global > 0) ((global - need) * 100 / global).toInt() else null
    }
    return devices.filter { it.status == DeviceStatus.SYNCING }
        .mapNotNull { it.completion }
        .minOrNull()
        ?.toInt()
        ?.coerceIn(0, 99)
}

class HomeViewModel(
    repository: SyncthingRepository,
    private val core: SyncthingCore,
    conflicts: ConflictSummary,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        core.state,
        repository.overview,
        conflicts.countsByFolder,
    ) { coreState, overview, conflictCounts ->
        if (overview == null) return@combine HomeState(coreState = coreState)
        val active = overview.folders.filter { !it.paused }
        HomeState(
            coreState = coreState,
            isLoaded = true,
            conflictFiles = conflictCounts.values.sum(),
            lastSync = overview.lastSync?.toRelativeUiText(),
            pendingDevices = overview.pendingDevices.size,
            pendingFolders = overview.pendingFolders.size,
            devices = overview.devices.map { MapDeviceUi(it.id.value, it.name, it.status, it.completion) },
            syncPercent = syncPercentOf(active, overview.devices),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnStartCore -> core.start()
            // Navigation is handled by the Root.
            else -> Unit
        }
    }
}
