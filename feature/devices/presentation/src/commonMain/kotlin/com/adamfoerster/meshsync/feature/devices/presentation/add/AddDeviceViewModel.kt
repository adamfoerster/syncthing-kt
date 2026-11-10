package com.adamfoerster.meshsync.feature.devices.presentation.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.PendingDevice
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.toUiText
import com.adamfoerster.meshsync.feature.devices.presentation.resources.Res
import com.adamfoerster.meshsync.feature.devices.presentation.resources.add_scan_invalid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AddStep { SHOW_CODE, APPROVE, FOLDERS }

data class AddDeviceState(
    val step: AddStep = AddStep.SHOW_CODE,
    val myId: String = "",
    val deviceIdInput: String = "",
    val nameInput: String = "",
    val showIdError: Boolean = false,
    /** A device that contacted us while the QR was on screen, offered for one-tap approval. */
    val incoming: IncomingDeviceUi? = null,
    val folders: List<FolderChoiceUi> = emptyList(),
    val isSaving: Boolean = false,
    val error: UiText? = null,
    /** Adding a device and showing our own code both need the core, so the flow says why it waits. */
    val coreState: CoreState = CoreState.Running,
) {
    val parsedId: DeviceId? get() = DeviceId.parse(deviceIdInput)
    val coreReady: Boolean get() = coreState == CoreState.Running
}

data class IncomingDeviceUi(val id: String, val name: String)

data class FolderChoiceUi(val id: String, val label: String, val checked: Boolean)

sealed interface AddDeviceAction {
    data object OnBackClick : AddDeviceAction
    data object OnScanClick : AddDeviceAction
    data class OnScanResult(val text: String?) : AddDeviceAction
    data object OnManualClick : AddDeviceAction
    data class OnIdChange(val value: String) : AddDeviceAction
    data class OnNameChange(val value: String) : AddDeviceAction
    data object OnUseIncoming : AddDeviceAction
    data object OnConfirmDevice : AddDeviceAction
    data class OnToggleFolder(val id: String) : AddDeviceAction
    data object OnFinish : AddDeviceAction
    data object OnSkipFolders : AddDeviceAction
    data object OnStartCore : AddDeviceAction
}

sealed interface AddDeviceEvent {
    data object LaunchScanner : AddDeviceEvent
    data object Done : AddDeviceEvent
}

/** Pairing flow: show our code, approve theirs, then pick the folders to share. */
class AddDeviceViewModel(
    prefillId: String?,
    prefillName: String?,
    private val repository: SyncthingRepository,
    private val core: SyncthingCore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddDeviceState(
            step = savedStateHandle.get<String>(KEY_STEP)?.let(AddStep::valueOf)
                ?: if (prefillId != null) AddStep.APPROVE else AddStep.SHOW_CODE,
            deviceIdInput = savedStateHandle[KEY_ID] ?: prefillId.orEmpty(),
            nameInput = savedStateHandle[KEY_NAME] ?: prefillName.orEmpty(),
            coreState = core.state.value,
        ),
    )
    val state = _state.asStateFlow()

    /** True once the user typed a name (or one was prefilled), so a suggestion never overwrites it. */
    private var nameEdited = _state.value.nameInput.isNotBlank()

    private val _events = Channel<AddDeviceEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { core.state.collect { c -> _state.update { it.copy(coreState = c) } } }
        viewModelScope.launch {
            repository.overview.filterNotNull().collect { overview ->
                val known = overview.devices.map { it.id }.toSet()
                pendingDevices = overview.pendingDevices
                _state.update { s ->
                    s.copy(
                        myId = overview.myId.value,
                        incoming = overview.pendingDevices.firstOrNull { it.id !in known }
                            ?.let { IncomingDeviceUi(it.id.value, it.name.ifBlank { it.id.short }) },
                        folders = overview.folders.map { folder ->
                            FolderChoiceUi(folder.id, folder.label, s.folders.firstOrNull { it.id == folder.id }?.checked ?: false)
                        },
                    ).withSuggestedName()
                }
            }
        }
    }

    private var pendingDevices: List<PendingDevice> = emptyList()

    /**
     * Fills the name with the one the device announced about itself (its machine name) when its ID matches a
     * pending device, until the user types their own.
     */
    private fun AddDeviceState.withSuggestedName(): AddDeviceState {
        if (nameEdited) return this
        val suggestion = parsedId?.let { id -> pendingDevices.firstOrNull { it.id == id }?.name?.trim() }
        return if (suggestion.isNullOrEmpty() || suggestion == nameInput) this else copy(nameInput = suggestion)
    }

    fun onAction(action: AddDeviceAction) {
        when (action) {
            AddDeviceAction.OnScanClick -> viewModelScope.launch { _events.send(AddDeviceEvent.LaunchScanner) }
            is AddDeviceAction.OnScanResult -> onScanned(action.text)
            AddDeviceAction.OnManualClick -> goTo(AddStep.APPROVE)
            is AddDeviceAction.OnIdChange -> {
                savedStateHandle[KEY_ID] = action.value
                _state.update { it.copy(deviceIdInput = action.value, showIdError = false, error = null).withSuggestedName() }
            }
            is AddDeviceAction.OnNameChange -> {
                savedStateHandle[KEY_NAME] = action.value
                nameEdited = action.value.isNotBlank()
                _state.update { it.copy(nameInput = action.value) }
            }
            AddDeviceAction.OnUseIncoming -> {
                val incoming = _state.value.incoming ?: return
                onAction(AddDeviceAction.OnIdChange(incoming.id))
                onAction(AddDeviceAction.OnNameChange(incoming.name))
                goTo(AddStep.APPROVE)
            }
            AddDeviceAction.OnConfirmDevice -> confirmDevice()
            is AddDeviceAction.OnToggleFolder -> _state.update { s ->
                s.copy(folders = s.folders.map { if (it.id == action.id) it.copy(checked = !it.checked) else it })
            }
            AddDeviceAction.OnFinish -> shareFolders()
            AddDeviceAction.OnSkipFolders -> viewModelScope.launch { _events.send(AddDeviceEvent.Done) }
            AddDeviceAction.OnStartCore -> core.start()
            AddDeviceAction.OnBackClick -> Unit
        }
    }

    private fun onScanned(text: String?) {
        if (text == null) return
        // Syncthing QR codes carry just the ID; tolerate URLs or extra text around it.
        val id = DeviceId.parse(text) ?: Regex("[A-Z2-7]{7}(-?[A-Z2-7]{7}){7}").find(text.uppercase())?.value?.let(DeviceId::parse)
        if (id == null) {
            _state.update { it.copy(error = UiText.Resource(Res.string.add_scan_invalid)) }
            return
        }
        onAction(AddDeviceAction.OnIdChange(id.value))
        goTo(AddStep.APPROVE)
    }

    private fun confirmDevice() {
        val s = _state.value
        val id = s.parsedId
        if (id == null) {
            _state.update { it.copy(showIdError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.addDevice(id, s.nameInput.trim().ifEmpty { id.short })) {
                is Result.Success -> {
                    _state.update { it.copy(isSaving = false) }
                    goTo(AddStep.FOLDERS)
                }
                is Result.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun shareFolders() {
        val s = _state.value
        val id = s.parsedId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val overview = repository.overview.value
            for (choice in s.folders.filter { it.checked }) {
                val members = overview?.folders?.firstOrNull { it.id == choice.id }?.deviceIds.orEmpty()
                val result = repository.setFolderDevices(choice.id, members + id)
                if (result is Result.Failure) {
                    _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
                    return@launch
                }
            }
            _events.send(AddDeviceEvent.Done)
        }
    }

    private fun goTo(step: AddStep) {
        savedStateHandle[KEY_STEP] = step.name
        _state.update { it.copy(step = step, error = null) }
    }

    private companion object {
        const val KEY_STEP = "step"
        const val KEY_ID = "deviceId"
        const val KEY_NAME = "name"
    }
}
