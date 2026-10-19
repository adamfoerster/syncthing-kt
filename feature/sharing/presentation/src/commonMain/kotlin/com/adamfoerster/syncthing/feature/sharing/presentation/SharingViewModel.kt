package com.adamfoerster.syncthing.feature.sharing.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.toDayAndTimeUiText
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.Res
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_no_folders
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_note_connected
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_note_offline
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_note_offline_since
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_note_paused
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_note_syncing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SharingState(
    val coreState: CoreState = CoreState.Starting,
    val devices: List<ShareDeviceUi> = emptyList(),
    val folders: List<ShareFolderUi> = emptyList(),
    /** deviceId → folderIds */
    val links: Map<String, Set<String>> = emptyMap(),
    val selectedId: String? = null,
    val selectedNote: UiText? = null,
) {
    val selected: ShareDeviceUi? get() = devices.firstOrNull { it.id == selectedId }
    val selectedFolders: List<ShareFolderUi>
        get() = links[selectedId].orEmpty().let { ids -> folders.filter { it.id in ids } }
}

data class ShareDeviceUi(val id: String, val name: String, val status: DeviceStatus)

data class ShareFolderUi(val id: String, val label: String, val deviceCount: Int)

sealed interface SharingAction {
    data object OnBackClick : SharingAction
    data class OnSelectDevice(val id: String) : SharingAction
}

class SharingViewModel(
    repository: SyncthingRepository,
    core: SyncthingCore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val selection = savedStateHandle.getStateFlow<String?>(KEY_SELECTED, null)

    val state: StateFlow<SharingState> = combine(core.state, repository.overview, selection) { coreState, overview, selected ->
        if (overview == null) return@combine SharingState(coreState = coreState)
        val devices = overview.devices
        val current = devices.firstOrNull { it.id.value == selected } ?: devices.firstOrNull()
        SharingState(
            coreState = coreState,
            devices = devices.map { ShareDeviceUi(it.id.value, it.name, it.status) },
            folders = overview.folders.map { ShareFolderUi(it.id, it.label, it.deviceIds.size) },
            links = devices.associate { it.id.value to it.folderIds.toSet() },
            selectedId = current?.id?.value,
            selectedNote = current?.note(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharingState())

    fun onAction(action: SharingAction) {
        when (action) {
            is SharingAction.OnSelectDevice -> savedStateHandle[KEY_SELECTED] = action.id
            SharingAction.OnBackClick -> Unit
        }
    }

    private fun Device.note(): UiText = when {
        folderIds.isEmpty() -> UiText.Resource(Res.string.sharing_no_folders)
        else -> when (status) {
            DeviceStatus.CONNECTED -> UiText.Resource(Res.string.sharing_note_connected)
            DeviceStatus.SYNCING -> UiText.Resource(Res.string.sharing_note_syncing)
            DeviceStatus.PAUSED -> UiText.Resource(Res.string.sharing_note_paused)
            DeviceStatus.DISCONNECTED -> lastSeen
                ?.let { UiText.Resource(Res.string.sharing_note_offline_since, it.toDayAndTimeUiText()) }
                ?: UiText.Resource(Res.string.sharing_note_offline)
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected"
    }
}
