package com.adamfoerster.syncthing.feature.folders.presentation.add

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Set when accepting a folder another device offered: its ID is fixed and that device is pre-selected. */
data class FolderOffer(val folderId: String, val label: String, val deviceId: String)

data class AddFolderState(
    val label: String = "",
    val path: String = "",
    val devices: List<DeviceChoiceUi> = emptyList(),
    val isOffer: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
) {
    val isValid: Boolean get() = label.isNotBlank() && path.isNotBlank()
}

data class DeviceChoiceUi(val id: String, val name: String, val checked: Boolean)

sealed interface AddFolderAction {
    data object OnBackClick : AddFolderAction
    data class OnLabelChange(val value: String) : AddFolderAction
    data class OnPathChange(val value: String) : AddFolderAction
    data class OnToggleDevice(val id: String) : AddFolderAction
    data object OnSave : AddFolderAction
}

sealed interface AddFolderEvent {
    data object Saved : AddFolderEvent
}

class AddFolderViewModel(
    private val offer: FolderOffer?,
    private val defaultRoot: String,
    private val repository: SyncthingRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AddFolderState(
            label = savedStateHandle[KEY_LABEL] ?: offer?.label.orEmpty(),
            path = savedStateHandle[KEY_PATH] ?: offer?.label?.let { suggestedPath(it) }.orEmpty(),
            isOffer = offer != null,
        ),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<AddFolderEvent>()
    val events = _events.receiveAsFlow()

    /** The path follows the name until the user types a path of their own. */
    private var pathEdited: Boolean = savedStateHandle[KEY_PATH_EDITED] ?: false

    init {
        viewModelScope.launch {
            val overview = repository.overview.filterNotNull().first()
            _state.update { s ->
                s.copy(devices = overview.devices.map { DeviceChoiceUi(it.id.value, it.name, it.id.value == offer?.deviceId) })
            }
        }
    }

    fun onAction(action: AddFolderAction) {
        when (action) {
            is AddFolderAction.OnLabelChange -> {
                savedStateHandle[KEY_LABEL] = action.value
                _state.update {
                    it.copy(label = action.value, path = if (pathEdited) it.path else suggestedPath(action.value), error = null)
                }
            }
            is AddFolderAction.OnPathChange -> {
                pathEdited = true
                savedStateHandle[KEY_PATH_EDITED] = true
                savedStateHandle[KEY_PATH] = action.value
                _state.update { it.copy(path = action.value, error = null) }
            }
            is AddFolderAction.OnToggleDevice -> _state.update { s ->
                s.copy(devices = s.devices.map { if (it.id == action.id) it.copy(checked = !it.checked) else it })
            }
            AddFolderAction.OnSave -> save()
            AddFolderAction.OnBackClick -> Unit
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.isValid || s.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val result = repository.addFolder(
                id = offer?.folderId ?: newFolderId(),
                label = s.label.trim(),
                path = s.path.trim(),
                type = FolderType.SEND_RECEIVE,
                devices = s.devices.filter { it.checked }.map { DeviceId(it.id) },
            )
            when (result) {
                is Result.Success -> _events.send(AddFolderEvent.Saved)
                is Result.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun suggestedPath(label: String): String {
        val safe = label.trim().replace(Regex("""[\\/:*?"<>|]"""), "_")
        return if (safe.isEmpty()) "" else "${defaultRoot.trimEnd('/')}/$safe"
    }

    /** Same shape as the IDs Syncthing's web UI generates: `abcde-fgh12`. */
    private fun newFolderId(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        fun part() = (1..5).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        return "${part()}-${part()}"
    }

    private companion object {
        const val KEY_LABEL = "label"
        const val KEY_PATH = "path"
        const val KEY_PATH_EDITED = "pathEdited"
    }
}
