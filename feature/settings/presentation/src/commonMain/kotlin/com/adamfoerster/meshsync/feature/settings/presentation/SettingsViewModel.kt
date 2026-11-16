package com.adamfoerster.meshsync.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.preferences.AppInfo
import com.adamfoerster.meshsync.core.domain.preferences.AppPreferences
import com.adamfoerster.meshsync.core.domain.preferences.AppLanguages
import com.adamfoerster.meshsync.core.domain.preferences.ThemeMode
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.toUiText
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
    /** Version of the Syncthing core, once it answers. */
    val version: String = "",
    /** Version of this MeshSync build. */
    val appVersion: String = "",
    val isSaving: Boolean = false,
    val nameSaved: Boolean = false,
    val autoAcceptKnownFolders: Boolean = AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS,
    /** Chosen language tag, or null to follow the device. */
    val languageTag: String? = null,
    val languages: List<String> = AppLanguages.supported,
    val themeMode: ThemeMode = ThemeMode.System,
    /** Whether the "What's new" popup, reopened from the version line, is showing. */
    val showReleaseNotes: Boolean = false,
    val error: UiText? = null,
) {
    val name: String get() = nameInput ?: savedName
    val canSaveName: Boolean get() = !isSaving && nameInput != null && nameInput.isNotBlank() && nameInput != savedName
}

sealed interface SettingsAction {
    data class OnNameChange(val value: String) : SettingsAction
    data object OnSaveName : SettingsAction
    data class OnAutoAcceptChange(val enabled: Boolean) : SettingsAction
    data class OnLanguageChange(val tag: String?) : SettingsAction
    data class OnThemeChange(val mode: ThemeMode) : SettingsAction
    data object OnShowReleaseNotes : SettingsAction
    data object OnDismissReleaseNotes : SettingsAction
    data object OnStart : SettingsAction
    data object OnStop : SettingsAction
    data object OnRestart : SettingsAction
}

private data class Edit(
    val nameInput: String? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: UiText? = null,
    val showReleaseNotes: Boolean = false,
)

class SettingsViewModel(
    private val repository: SyncthingRepository,
    private val core: SyncthingCore,
    private val preferences: AppPreferences,
    private val appInfo: AppInfo,
) : ViewModel() {

    private val edit = MutableStateFlow(Edit())

    val state: StateFlow<SettingsState> = combine(
        combine(core.state, repository.overview, edit, ::Triple),
        preferences.autoAcceptKnownFolders,
        combine(preferences.languageTag, preferences.themeMode, ::Pair),
    ) { (coreState, overview, e), autoAccept, (languageTag, themeMode) ->
        SettingsState(
            coreState = coreState,
            myId = overview?.myId?.value.orEmpty(),
            savedName = overview?.myName.orEmpty(),
            nameInput = e.nameInput,
            version = overview?.version.orEmpty(),
            appVersion = appInfo.version,
            isSaving = e.isSaving,
            nameSaved = e.saved,
            error = e.error,
            showReleaseNotes = e.showReleaseNotes,
            autoAcceptKnownFolders = autoAccept,
            languageTag = languageTag,
            themeMode = themeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnNameChange -> edit.update { it.copy(nameInput = action.value, saved = false, error = null) }
            SettingsAction.OnSaveName -> saveName()
            is SettingsAction.OnAutoAcceptChange -> preferences.setAutoAcceptKnownFolders(action.enabled)
            is SettingsAction.OnLanguageChange -> preferences.setLanguageTag(AppLanguages.sanitize(action.tag))
            is SettingsAction.OnThemeChange -> preferences.setThemeMode(action.mode)
            SettingsAction.OnShowReleaseNotes -> edit.update { it.copy(showReleaseNotes = true) }
            SettingsAction.OnDismissReleaseNotes -> edit.update { it.copy(showReleaseNotes = false) }
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
                is Result.Success -> edit.update { Edit(saved = true, showReleaseNotes = it.showReleaseNotes) }
                is Result.Failure -> edit.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }
}
