package com.adamfoerster.syncthing.feature.folders.presentation.ignore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.model.IgnorePreset
import com.adamfoerster.syncthing.core.domain.model.IgnoreRule
import com.adamfoerster.syncthing.core.domain.model.IgnoreRules
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.toUiText
import com.adamfoerster.syncthing.feature.folders.presentation.resources.Res
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_error_empty
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class IgnorePhase { LOADING, READY, LOAD_ERROR }

/**
 * [text] is the single source of truth, in the same one-rule-per-line form as `.stignore`: the simple
 * editor and the advanced one are two views of it, so switching between them never loses anything.
 */
data class IgnoreState(
    val phase: IgnorePhase = IgnorePhase.LOADING,
    val folderLabel: String = "",
    val advanced: Boolean = false,
    val text: String = "",
    val savedText: String = "",
    val input: String = "",
    val inputError: UiText? = null,
    val isSaving: Boolean = false,
    val error: UiText? = null,
) {
    val lines: List<String> get() = IgnoreRules.fromText(text)

    /** What the simple editor lists: comments stay in the file but out of sight. */
    val rules: List<IgnoreRule> get() = IgnoreRules.parse(lines).filterNot { it is IgnoreRule.Comment }

    val presets: List<IgnorePresetUi>
        get() = lines.let { current -> IgnorePreset.entries.map { IgnorePresetUi(it, IgnoreRules.isActive(current, it)) } }

    val isDirty: Boolean get() = lines != IgnoreRules.fromText(savedText)
}

data class IgnorePresetUi(val preset: IgnorePreset, val active: Boolean)

sealed interface IgnoreAction {
    data object OnBackClick : IgnoreAction
    data object OnRetry : IgnoreAction
    data class OnAdvancedChange(val advanced: Boolean) : IgnoreAction
    data class OnInputChange(val value: String) : IgnoreAction
    data object OnAddClick : IgnoreAction
    data class OnRemove(val line: String) : IgnoreAction
    data class OnTogglePreset(val preset: IgnorePreset) : IgnoreAction
    data class OnTextChange(val value: String) : IgnoreAction
    data object OnSave : IgnoreAction
}

sealed interface IgnoreEvent {
    data object Saved : IgnoreEvent
}

class IgnoreViewModel(
    private val folderId: String,
    private val repository: SyncthingRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        IgnoreState(
            folderLabel = repository.overview.value?.folders?.firstOrNull { it.id == folderId }?.label ?: folderId,
            advanced = savedStateHandle[KEY_ADVANCED] ?: false,
            input = savedStateHandle[KEY_INPUT] ?: "",
        ),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<IgnoreEvent>()
    val events = _events.receiveAsFlow()

    init {
        load()
    }

    fun onAction(action: IgnoreAction) {
        when (action) {
            IgnoreAction.OnRetry -> load()
            is IgnoreAction.OnAdvancedChange -> {
                savedStateHandle[KEY_ADVANCED] = action.advanced
                _state.update { it.copy(advanced = action.advanced, inputError = null) }
            }
            is IgnoreAction.OnInputChange -> {
                savedStateHandle[KEY_INPUT] = action.value
                _state.update { it.copy(input = action.value, inputError = null) }
            }
            IgnoreAction.OnAddClick -> add()
            is IgnoreAction.OnRemove -> edit { IgnoreRules.remove(it, action.line) }
            is IgnoreAction.OnTogglePreset -> edit { IgnoreRules.toggle(it, action.preset) }
            is IgnoreAction.OnTextChange -> setText(action.value)
            IgnoreAction.OnSave -> save()
            IgnoreAction.OnBackClick -> Unit
        }
    }

    private fun load() {
        _state.update { it.copy(phase = IgnorePhase.LOADING) }
        viewModelScope.launch {
            when (val result = repository.ignorePatterns(folderId)) {
                is Result.Success -> {
                    val saved = IgnoreRules.toText(result.data)
                    // After process death the unsaved edit comes back; otherwise start from what the core has.
                    val text = savedStateHandle[KEY_TEXT] ?: saved
                    _state.update { it.copy(phase = IgnorePhase.READY, savedText = saved, text = text, error = null) }
                }
                is Result.Failure -> _state.update { it.copy(phase = IgnorePhase.LOAD_ERROR, error = result.error.toUiText()) }
            }
        }
    }

    private fun add() {
        val line = IgnoreRules.normalize(_state.value.input)
        if (line == null) {
            _state.update { it.copy(inputError = UiText.Resource(Res.string.ignore_error_empty)) }
            return
        }
        savedStateHandle[KEY_INPUT] = ""
        _state.update { it.copy(input = "", inputError = null) }
        edit { IgnoreRules.add(it, line) }
    }

    private fun edit(change: (List<String>) -> List<String>) = setText(IgnoreRules.toText(change(_state.value.lines)))

    private fun setText(text: String) {
        savedStateHandle[KEY_TEXT] = text
        _state.update { it.copy(text = text, error = null) }
    }

    private fun save() {
        val s = _state.value
        if (s.phase != IgnorePhase.READY || !s.isDirty || s.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.setIgnorePatterns(folderId, s.lines)) {
                is Result.Success -> {
                    savedStateHandle.remove<String>(KEY_TEXT)
                    _state.update { it.copy(isSaving = false, savedText = s.text) }
                    _events.send(IgnoreEvent.Saved)
                }
                is Result.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }

    private companion object {
        const val KEY_TEXT = "text"
        const val KEY_ADVANCED = "advanced"
        const val KEY_INPUT = "input"
    }
}
