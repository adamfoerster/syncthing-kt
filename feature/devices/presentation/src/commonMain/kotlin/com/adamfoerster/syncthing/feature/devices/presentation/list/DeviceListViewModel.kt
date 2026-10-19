package com.adamfoerster.syncthing.feature.devices.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.onFailure
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.initialOf
import com.adamfoerster.syncthing.core.presentation.toDayAndTimeUiText
import com.adamfoerster.syncthing.core.presentation.toUiText
import com.adamfoerster.syncthing.feature.devices.presentation.resources.Res
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_line_connected
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_line_never
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_line_offline_since
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_line_paused
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_line_syncing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceListState(
    val coreState: CoreState = CoreState.Starting,
    val isLoaded: Boolean = false,
    val devices: List<DeviceUi> = emptyList(),
    val pending: List<PendingDeviceUi> = emptyList(),
    val menuFor: DeviceUi? = null,
    val confirmRemove: DeviceUi? = null,
    val error: UiText? = null,
)

data class DeviceUi(
    val id: String,
    val name: String,
    val initial: String,
    val status: DeviceStatus,
    val statusLine: UiText,
    val folders: List<String>,
)

data class PendingDeviceUi(val id: String, val shortId: String, val name: String, val address: String)

sealed interface DeviceListAction {
    data object OnAddDevice : DeviceListAction
    data class OnApprovePending(val device: PendingDeviceUi) : DeviceListAction
    data class OnRejectPending(val device: PendingDeviceUi) : DeviceListAction
    data class OnDeviceClick(val device: DeviceUi) : DeviceListAction
    data object OnDismissMenu : DeviceListAction
    data class OnTogglePause(val device: DeviceUi) : DeviceListAction
    data class OnRemoveRequest(val device: DeviceUi) : DeviceListAction
    data object OnRemoveConfirm : DeviceListAction
    data object OnStartCore : DeviceListAction
}

private data class Dialogs(val menuFor: DeviceUi? = null, val confirmRemove: DeviceUi? = null, val error: UiText? = null)

class DeviceListViewModel(
    private val repository: SyncthingRepository,
    private val core: SyncthingCore,
) : ViewModel() {

    private val dialogs = MutableStateFlow(Dialogs())

    val state: StateFlow<DeviceListState> = combine(core.state, repository.overview, dialogs) { coreState, overview, d ->
        if (overview == null) return@combine DeviceListState(coreState = coreState)
        val folderLabels = overview.folders.associate { it.id to it.label }
        DeviceListState(
            coreState = coreState,
            isLoaded = true,
            devices = overview.devices.sortedBy { it.name.lowercase() }.map { it.toUi(folderLabels) },
            pending = overview.pendingDevices.map { PendingDeviceUi(it.id.value, it.id.short, it.name.ifBlank { it.id.short }, it.address) },
            menuFor = d.menuFor,
            confirmRemove = d.confirmRemove,
            error = d.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceListState())

    fun onAction(action: DeviceListAction) {
        when (action) {
            is DeviceListAction.OnRejectPending -> launchCall { repository.dismissPendingDevice(DeviceId(action.device.id)) }
            is DeviceListAction.OnDeviceClick -> dialogs.update { it.copy(menuFor = action.device, error = null) }
            DeviceListAction.OnDismissMenu -> dialogs.value = Dialogs()
            is DeviceListAction.OnTogglePause -> {
                dialogs.value = Dialogs()
                launchCall { repository.setDevicePaused(DeviceId(action.device.id), action.device.status != DeviceStatus.PAUSED) }
            }
            is DeviceListAction.OnRemoveRequest -> dialogs.value = Dialogs(confirmRemove = action.device)
            DeviceListAction.OnRemoveConfirm -> {
                val target = dialogs.value.confirmRemove ?: return
                dialogs.value = Dialogs()
                launchCall { repository.removeDevice(DeviceId(target.id)) }
            }
            DeviceListAction.OnStartCore -> core.start()
            // Navigation is handled by the Root.
            DeviceListAction.OnAddDevice, is DeviceListAction.OnApprovePending -> Unit
        }
    }

    private fun launchCall(call: suspend () -> EmptyResult<DataError.Remote>) {
        viewModelScope.launch {
            call().onFailure { error -> dialogs.update { it.copy(error = error.toUiText()) } }
        }
    }

    private fun Device.toUi(folderLabels: Map<String, String>) = DeviceUi(
        id = id.value,
        name = name,
        initial = initialOf(name),
        status = status,
        statusLine = when (status) {
            DeviceStatus.CONNECTED -> UiText.Resource(Res.string.devices_line_connected)
            DeviceStatus.SYNCING -> UiText.Resource(Res.string.devices_line_syncing)
            DeviceStatus.PAUSED -> UiText.Resource(Res.string.devices_line_paused)
            DeviceStatus.DISCONNECTED -> lastSeen?.let { UiText.Resource(Res.string.devices_line_offline_since, it.toDayAndTimeUiText()) }
                ?: UiText.Resource(Res.string.devices_line_never)
        },
        folders = folderIds.mapNotNull { folderLabels[it] },
    )
}
