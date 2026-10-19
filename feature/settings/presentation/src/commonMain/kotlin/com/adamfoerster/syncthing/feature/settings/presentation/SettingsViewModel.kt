package com.adamfoerster.syncthing.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.toUiText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsState(
    val coreState: CoreState = CoreState.Starting,
    val myId: String = "",
    val savedName: String = "",
    val nameInput: String? = null,
    val version: String = "",
    val isSaving: Boolean = false,
    val nameSaved: Boolean = false,
    val error: UiText? = null,
) {
    val name: String get() = nameInput ?: savedName
    val canSaveName: Boolean get() = !isSaving && nameInput != null && nameInput.isNotBlank() && nameInput != savedName
}

sealed interface SettingsAction {
    data class OnNameChange(val value: String) : SettingsAction
    data object OnSaveName : SettingsAction
    data object OnStart : SettingsAction
    data object OnStop : SettingsAction
    data object OnRestart : SettingsAction
}

private data class Edit(val nameInput: String? = null, val isSaving: Boolean = false, val saved: Boolean = false, val error: UiText? = null)

class SettingsViewModel(
    private val repository: SyncthingRepository,
    private val core: SyncthingCore,
) : ViewModel() {

    private val edit = MutableStateFlow(Edit())

    val state: StateFlow<SettingsState> = combine(core.state, repository.overview, edit) { coreState, overview, e ->
        SettingsState(
            coreState = coreState,
            myId = overview?.myId?.value.orEmpty(),
            savedName = overview?.myName.orEmpty(),
            nameInput = e.nameInput,
            version = overview?.version.orEmpty(),
            isSaving = e.isSaving,
            nameSaved = e.saved,
            error = e.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnNameChange -> edit.update { it.copy(nameInput = action.value, saved = false, error = null) }
            SettingsAction.OnSaveName -> saveName()
            SettingsAction.OnStart -> core.start()
            SettingsAction.OnStop -> core.stop()
            SettingsAction.OnRestart -> core.restart()
        }
    }

    private fun saveName() {
        val name = edit.value.nameInput?.trim() ?: return
        viewModelScope.launch {
            edit.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.setMyName(name)) {
                is Result.Success -> edit.value = Edit(saved = true)
                is Result.Failure -> edit.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}
