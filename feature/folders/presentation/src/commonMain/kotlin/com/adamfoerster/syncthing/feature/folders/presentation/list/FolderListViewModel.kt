package com.adamfoerster.syncthing.feature.folders.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderState
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.ConflictSummary
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.formatBytes
import com.adamfoerster.syncthing.core.presentation.formatPercent
import com.adamfoerster.syncthing.core.presentation.initialOf
import com.adamfoerster.syncthing.feature.folders.presentation.resources.Res
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_device_offline
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_devices_offline
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_not_shared
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_with_devices
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FolderListViewModel(
    private val repository: SyncthingRepository,
    private val core: SyncthingCore,
    conflicts: ConflictSummary,
) : ViewModel() {

    val state: StateFlow<FolderListState> = combine(core.state, repository.overview, conflicts.countsByFolder) { coreState, overview, counts ->
        if (overview == null) return@combine FolderListState(coreState = coreState)
        FolderListState(
            coreState = coreState,
            isLoaded = true,
            folders = overview.folders.sortedBy { it.label.lowercase() }.map { it.toUi(overview, counts[it.id] ?: 0) },
            offers = overview.pendingFolders.map { FolderOfferUi(it.folderId, it.label, it.offeredBy.value, it.offeredByName) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderListState())

    fun onAction(action: FolderListAction) {
        when (action) {
            is FolderListAction.OnDismissOffer -> viewModelScope.launch {
                repository.dismissPendingFolder(action.offer.folderId, DeviceId(action.offer.deviceId))
            }
            FolderListAction.OnStartCore -> core.start()
            else -> Unit
        }
    }

    private fun Folder.toUi(overview: SyncOverview, conflictCount: Int): FolderUi {
        val members = overview.devices.filter { it.id in deviceIds }
        val online = members.filter { it.status == DeviceStatus.CONNECTED || it.status == DeviceStatus.SYNCING }
        val waiting = members.isNotEmpty() && online.isEmpty()
        val syncing = status.state == FolderState.SYNCING || (status.needBytes > 0 && !waiting)
        val badge = when {
            paused -> FolderBadge(FolderBadgeKind.PAUSED, Tone.Neutral)
            conflictCount > 0 -> FolderBadge(FolderBadgeKind.CONFLICTS, Tone.Warn, conflictCount)
            status.state == FolderState.ERROR -> FolderBadge(FolderBadgeKind.ERROR, Tone.Error)
            syncing -> FolderBadge(FolderBadgeKind.SYNCING, Tone.Info)
            status.state == FolderState.SCANNING -> FolderBadge(FolderBadgeKind.SCANNING, Tone.Info)
            waiting -> FolderBadge(FolderBadgeKind.WAITING, Tone.Warn)
            members.isEmpty() -> FolderBadge(FolderBadgeKind.LOCAL_ONLY, Tone.Neutral)
            else -> FolderBadge(FolderBadgeKind.UP_TO_DATE, Tone.Ok)
        }
        return FolderUi(
            id = id,
            label = label,
            size = formatBytes(status.globalBytes),
            badge = badge,
            progress = if (syncing) (status.completion / 100).toFloat() else null,
            percentText = if (syncing) formatPercent(status.completion) else null,
            remainingText = if (syncing) formatBytes(status.needBytes) else null,
            deviceInitials = members.map { initialOf(it.name) },
            devicesLine = when {
                members.isEmpty() -> UiText.Resource(Res.string.folders_not_shared)
                waiting && members.size == 1 -> UiText.Resource(Res.string.folders_device_offline, members.single().name)
                waiting -> UiText.Resource(Res.string.folders_devices_offline)
                else -> UiText.Plural(Res.plurals.folders_with_devices, members.size, members.size)
            },
            conflicts = conflictCount,
        )
    }
}
