package com.adamfoerster.meshsync.feature.devices.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.model.Device
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.DeviceStatus
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.core.domain.util.onFailure
import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.initialOf
import com.adamfoerster.meshsync.core.presentation.toDayAndTimeUiText
import com.adamfoerster.meshsync.core.presentation.formatBytes
import com.adamfoerster.meshsync.core.presentation.toUiText
import com.adamfoerster.meshsync.feature.devices.presentation.resources.Res
import com.adamfoerster.meshsync.feature.devices.presentation.resources.devices_line_connected
import com.adamfoerster.meshsync.feature.devices.presentation.resources.devices_line_never
import com.adamfoerster.meshsync.feature.devices.presentation.resources.devices_line_offline_since
import com.adamfoerster.meshsync.feature.devices.presentation.resources.devices_line_paused
import com.adamfoerster.meshsync.feature.devices.presentation.resources.devices_line_syncing
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    /** Device being renamed and the name typed so far; null when the rename dialog is closed. */
    val rename: RenameUi? = null,
    /** Files the device still has to receive; null when that dialog is closed. */
    val pendingFiles: PendingFilesUi? = null,
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

data class RenameUi(val device: DeviceUi, val input: String) {
    val canSave: Boolean get() = input.isNotBlank()
}

/** Files a device has yet to receive, grouped by folder; [isLoading] until the core answers. */
data class PendingFilesUi(
    val device: DeviceUi,
    val isLoading: Boolean = true,
    val folders: List<PendingFolderUi> = emptyList(),
    /** At least one folder has more entries than were fetched. */
    val truncated: Boolean = false,
    val error: UiText? = null,
) {
    val isEmpty: Boolean get() = !isLoading && error == null && folders.isEmpty()
}

data class PendingFolderUi(val label: String, val files: List<PendingFileUi>)

data class PendingFileUi(val path: String, val size: String?, val isDirectory: Boolean, val isDeletion: Boolean)

data class PendingDeviceUi(val id: String, val shortId: String, val name: String, val address: String)

sealed interface DeviceListAction {
    data object OnAddDevice : DeviceListAction
    data class OnApprovePending(val device: PendingDeviceUi) : DeviceListAction
    data class OnRejectPending(val device: PendingDeviceUi) : DeviceListAction
    data class OnDeviceClick(val device: DeviceUi) : DeviceListAction
    data object OnDismissMenu : DeviceListAction
    data class OnTogglePause(val device: DeviceUi) : DeviceListAction
    data class OnShowPendingFiles(val device: DeviceUi) : DeviceListAction
    data class OnRenameRequest(val device: DeviceUi) : DeviceListAction
    data class OnRenameInputChange(val value: String) : DeviceListAction
    data object OnRenameConfirm : DeviceListAction
    data class OnRemoveRequest(val device: DeviceUi) : DeviceListAction
    data object OnRemoveConfirm : DeviceListAction
    data object OnStartCore : DeviceListAction
}

private data class Dialogs(
    val menuFor: DeviceUi? = null,
    val confirmRemove: DeviceUi? = null,
    val rename: RenameUi? = null,
    val pendingFiles: PendingFilesUi? = null,
    val error: UiText? = null,
)

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
            rename = d.rename,
            pendingFiles = d.pendingFiles,
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
            is DeviceListAction.OnShowPendingFiles -> loadPendingFiles(action.device)
            is DeviceListAction.OnRenameRequest -> dialogs.value = Dialogs(rename = RenameUi(action.device, action.device.name))
            is DeviceListAction.OnRenameInputChange -> dialogs.update { d -> d.copy(rename = d.rename?.copy(input = action.value)) }
            DeviceListAction.OnRenameConfirm -> {
                val rename = dialogs.value.rename ?: return
                val name = rename.input.trim()
                if (name.isEmpty()) return
                dialogs.value = Dialogs()
                if (name != rename.device.name) launchCall { repository.renameDevice(DeviceId(rename.device.id), name) }
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

    private fun loadPendingFiles(device: DeviceUi) {
        val overview = repository.overview.value ?: return
        val shared = overview.devices.firstOrNull { it.id.value == device.id }?.folderIds.orEmpty()
        val folders = shared.mapNotNull { id -> overview.folders.firstOrNull { it.id == id } }
        dialogs.value = Dialogs(pendingFiles = PendingFilesUi(device))
        viewModelScope.launch {
            val results = folders
                .map { folder -> async { folder to repository.pendingFiles(DeviceId(device.id), folder.id) } }
                .awaitAll()
            val groups = results.mapNotNull { (folder, result) ->
                val files = (result as? Result.Success)?.data.orEmpty()
                if (files.isEmpty()) return@mapNotNull null
                PendingFolderUi(
                    folder.label,
                    files.sortedBy { it.path.lowercase() }
                        .map { PendingFileUi(it.path, it.size.takeIf { size -> !it.isDirectory && !it.isDeletion }?.let(::formatBytes), it.isDirectory, it.isDeletion) },
                )
            }.sortedBy { it.label.lowercase() }
            val failure = results.firstNotNullOfOrNull { (_, result) -> result as? Result.Failure }
            val loaded = PendingFilesUi(
                device = device,
                isLoading = false,
                folders = groups,
                truncated = groups.any { it.files.size >= SyncthingRepository.PENDING_FILES_LIMIT },
                // A partial list is still useful; only report the error when nothing came back.
                error = failure?.takeIf { groups.isEmpty() }?.error?.toUiText(),
            )
            // The dialog may have been closed (or reopened for another device) meanwhile.
            dialogs.update { d -> if (d.pendingFiles?.device?.id == device.id) d.copy(pendingFiles = loaded) else d }
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
