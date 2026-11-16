package com.adamfoerster.meshsync.feature.files.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.toUiText
import com.adamfoerster.meshsync.feature.files.domain.IncomingShareInbox
import com.adamfoerster.meshsync.feature.files.domain.ShareImporter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShareFolderUi(val id: String, val label: String, val path: String, val paused: Boolean)

data class ShareTargetState(
    val itemCount: Int = 0,
    val folders: List<ShareFolderUi> = emptyList(),
    val isImporting: Boolean = false,
    val error: UiText? = null,
    /** Set once the files were copied; the screen then confirms and waits for OK. */
    val done: ShareDone? = null,
)

data class ShareDone(val copied: Int, val folderLabel: String)

sealed interface ShareTargetAction {
    data class OnFolderClick(val folderId: String) : ShareTargetAction
    data object OnCancel : ShareTargetAction
    data object OnDismissError : ShareTargetAction
    data object OnDone : ShareTargetAction
}

sealed interface ShareTargetEvent {
    /** Leave the screen. */
    data object Exit : ShareTargetEvent
}

/** Lets the user choose which synced folder receives what another app shared with MeshSync. */
class ShareTargetViewModel(
    private val inbox: IncomingShareInbox,
    private val repository: SyncthingRepository,
    private val importer: ShareImporter,
) : ViewModel() {

    private val _state = MutableStateFlow(ShareTargetState(itemCount = inbox.pending.value?.itemCount ?: 0))
    val state = _state.asStateFlow()

    private val _events = Channel<ShareTargetEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.overview.collect { overview ->
                // Encrypted-receive folders cannot take plain files.
                val folders = overview?.folders.orEmpty()
                    .filter { it.type != FolderType.RECEIVE_ENCRYPTED }
                    .map { ShareFolderUi(it.id, it.label, it.path, it.paused) }
                _state.update { it.copy(folders = folders) }
            }
        }
    }

    fun onAction(action: ShareTargetAction) {
        when (action) {
            is ShareTargetAction.OnFolderClick -> import(action.folderId)
            ShareTargetAction.OnCancel, ShareTargetAction.OnDone -> exit()
            ShareTargetAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun import(folderId: String) {
        val share = inbox.pending.value ?: return
        val folder = _state.value.folders.firstOrNull { it.id == folderId } ?: return
        if (_state.value.isImporting) return
        _state.update { it.copy(isImporting = true, error = null) }
        viewModelScope.launch {
            when (val result = importer.import(share, folder.path)) {
                is Result.Success -> {
                    // The folder watcher usually notices, but a rescan makes the new files travel right away.
                    repository.scan(folder.id)
                    inbox.clear()
                    _state.update { it.copy(isImporting = false, done = ShareDone(result.data, folder.label)) }
                }
                is Result.Failure -> _state.update { it.copy(isImporting = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun exit() {
        inbox.clear()
        viewModelScope.launch { _events.send(ShareTargetEvent.Exit) }
    }
}
