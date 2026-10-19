package com.adamfoerster.syncthing.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.FolderState
import com.adamfoerster.syncthing.core.domain.repository.ConflictSummary
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.presentation.toRelativeUiText
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
        val global = active.sumOf { it.status.globalBytes }
        val need = active.sumOf { it.status.needBytes }
        val syncing = active.any { it.status.state == FolderState.SYNCING } || need > 0
        HomeState(
            coreState = coreState,
            isLoaded = true,
            conflictFiles = conflictCounts.values.sum(),
            lastSync = overview.lastSync?.toRelativeUiText(),
            pendingDevices = overview.pendingDevices.size,
            pendingFolders = overview.pendingFolders.size,
            devices = overview.devices.map { MapDeviceUi(it.id.value, it.name, it.status, it.completion) },
            syncPercent = if (syncing && global > 0) ((global - need) * 100 / global).toInt() else null,
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
