package com.adamfoerster.meshsync.feature.files.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.formatBytes
import com.adamfoerster.meshsync.core.presentation.toUiText
import com.adamfoerster.meshsync.feature.files.domain.FileEntry
import com.adamfoerster.meshsync.feature.files.domain.FileManager
import com.adamfoerster.meshsync.feature.files.domain.parentOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the file manager lists: [directory] is absolute, [name] its last segment. */
data class FileEntryUi(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    /** A folder MeshSync syncs; drawn with the app mark inside the folder icon. */
    val isShared: Boolean,
    /** Empty for folders. */
    val size: String,
    val selected: Boolean,
)

/** Copied or cut items waiting for a paste. */
data class FileClipboard(val paths: List<String>, val move: Boolean)

/** An in-screen prompt; [Rename] and [NewFolder] carry the text being typed. */
sealed interface FilesDialog {
    data class Delete(val paths: List<String>) : FilesDialog
    data class Rename(val path: String, val name: String) : FilesDialog
    data class NewFolder(val name: String) : FilesDialog
}

data class FilesState(
    val root: String = "",
    val directory: String = "",
    val entries: List<FileEntryUi> = emptyList(),
    val isLoading: Boolean = true,
    val clipboard: FileClipboard? = null,
    val dialog: FilesDialog? = null,
    val error: UiText? = null,
) {
    val selection: List<FileEntryUi> get() = entries.filter { it.selected }
    val isSelecting: Boolean get() = entries.any { it.selected }

    /** The system share dialog sends files, not folders. */
    val canShare: Boolean get() = selection.let { it.isNotEmpty() && it.none(FileEntryUi::isDirectory) }
    val canRename: Boolean get() = selection.size == 1

    /** Only a single folder that MeshSync does not sync yet can start a sync. */
    val canSync: Boolean get() = selection.singleOrNull()?.let { it.isDirectory && !it.isShared } == true
    val atRoot: Boolean get() = directory == root
}

sealed interface FilesAction {
    data object OnBackClick : FilesAction
    data class OnEntryClick(val path: String) : FilesAction
    data class OnEntryLongClick(val path: String) : FilesAction
    data object OnClearSelection : FilesAction
    data object OnCopy : FilesAction
    data object OnCut : FilesAction
    data object OnPaste : FilesAction
    data object OnClearClipboard : FilesAction
    data object OnShare : FilesAction
    data object OnSync : FilesAction
    data object OnDeleteClick : FilesAction
    data object OnRenameClick : FilesAction
    data object OnNewFolderClick : FilesAction
    data class OnDialogTextChange(val text: String) : FilesAction
    data object OnConfirmDialog : FilesAction
    data object OnDismissDialog : FilesAction
    data object OnDismissError : FilesAction
}

sealed interface FilesEvent {
    /** Back at the top of the storage: leave the file manager. */
    data object Exit : FilesEvent
    data class Share(val paths: List<String>) : FilesEvent

    /** Open the add-folder flow with [path] filled in. */
    data class Sync(val path: String) : FilesEvent
}

class FilesViewModel(
    private val root: String,
    private val repository: SyncthingRepository,
    private val files: FileManager,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(FilesState(root = root, directory = savedStateHandle[KEY_DIRECTORY] ?: root))
    val state = _state.asStateFlow()

    private val _events = Channel<FilesEvent>()
    val events = _events.receiveAsFlow()

    private var listing: List<FileEntry> = emptyList()
    private var selected: Set<String> = emptySet()
    private var sharedPaths: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            repository.overview.collect { overview ->
                sharedPaths = overview?.folders.orEmpty().map { it.path.trimEnd('/') }.toSet()
                publish()
            }
        }
        load(_state.value.directory)
    }

    fun onAction(action: FilesAction) {
        when (action) {
            FilesAction.OnBackClick -> goBack()
            is FilesAction.OnEntryClick -> {
                val entry = listing.firstOrNull { it.path == action.path } ?: return
                if (entry.isDirectory && selected.isEmpty()) load(entry.path) else toggle(action.path)
            }
            is FilesAction.OnEntryLongClick -> toggle(action.path)
            FilesAction.OnClearSelection -> select(emptySet())
            FilesAction.OnCopy -> toClipboard(move = false)
            FilesAction.OnCut -> toClipboard(move = true)
            FilesAction.OnPaste -> paste()
            FilesAction.OnClearClipboard -> _state.update { it.copy(clipboard = null) }
            FilesAction.OnShare -> {
                val paths = _state.value.selection.map { it.path }
                if (_state.value.canShare) viewModelScope.launch { _events.send(FilesEvent.Share(paths)) }
            }
            FilesAction.OnSync -> {
                val folder = _state.value.selection.singleOrNull()
                if (_state.value.canSync && folder != null) viewModelScope.launch { _events.send(FilesEvent.Sync(folder.path)) }
            }
            FilesAction.OnDeleteClick -> {
                val paths = _state.value.selection.map { it.path }
                if (paths.isNotEmpty()) _state.update { it.copy(dialog = FilesDialog.Delete(paths), error = null) }
            }
            FilesAction.OnRenameClick -> {
                val entry = _state.value.selection.singleOrNull() ?: return
                _state.update { it.copy(dialog = FilesDialog.Rename(entry.path, entry.name), error = null) }
            }
            FilesAction.OnNewFolderClick -> _state.update { it.copy(dialog = FilesDialog.NewFolder(""), error = null) }
            is FilesAction.OnDialogTextChange -> _state.update { s ->
                s.copy(
                    dialog = when (val d = s.dialog) {
                        is FilesDialog.Rename -> d.copy(name = action.text)
                        is FilesDialog.NewFolder -> d.copy(name = action.text)
                        else -> d
                    },
                    error = null,
                )
            }
            FilesAction.OnConfirmDialog -> confirmDialog()
            FilesAction.OnDismissDialog -> _state.update { it.copy(dialog = null, error = null) }
            FilesAction.OnDismissError -> _state.update { it.copy(error = null) }
        }
    }

    private fun goBack() {
        val s = _state.value
        val parent = parentOf(s.directory)
        when {
            s.isSelecting -> select(emptySet())
            s.dialog != null -> _state.update { it.copy(dialog = null, error = null) }
            s.atRoot || parent == null -> viewModelScope.launch { _events.send(FilesEvent.Exit) }
            else -> load(parent)
        }
    }

    private fun load(directory: String, keepError: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = if (keepError) it.error else null) }
            when (val result = files.list(directory)) {
                is Result.Success -> {
                    listing = result.data
                    selected = emptySet()
                    savedStateHandle[KEY_DIRECTORY] = directory
                    _state.update { it.copy(directory = directory, isLoading = false) }
                    publish()
                }
                // Stay where we were: the previous listing is still on screen.
                is Result.Failure -> _state.update { it.copy(isLoading = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun reload(keepError: Boolean = false) = load(_state.value.directory, keepError)

    private fun toggle(path: String) = select(if (path in selected) selected - path else selected + path)

    private fun select(paths: Set<String>) {
        selected = paths
        publish()
    }

    private fun toClipboard(move: Boolean) {
        val paths = _state.value.selection.map { it.path }
        if (paths.isEmpty()) return
        selected = emptySet()
        _state.update { it.copy(clipboard = FileClipboard(paths, move), error = null) }
        publish()
    }

    private fun paste() {
        val clipboard = _state.value.clipboard ?: return
        val target = _state.value.directory
        viewModelScope.launch {
            finish(files.paste(clipboard.paths, target, clipboard.move)) { if (clipboard.move) it.copy(clipboard = null) else it }
        }
    }

    private fun confirmDialog() {
        when (val dialog = _state.value.dialog ?: return) {
            is FilesDialog.Delete -> viewModelScope.launch { finish(files.delete(dialog.paths)) { it.copy(dialog = null) } }
            is FilesDialog.Rename -> viewModelScope.launch { finish(files.rename(dialog.path, dialog.name)) { it.copy(dialog = null) } }
            is FilesDialog.NewFolder -> viewModelScope.launch {
                finish(files.createDirectory(_state.value.directory, dialog.name)) { it.copy(dialog = null) }
            }
        }
    }

    /** After a change: on success refresh the listing, on failure keep the dialog open and say why. */
    private fun finish(result: EmptyResult<DataError.Local>, onSuccess: (FilesState) -> FilesState) {
        when (result) {
            is Result.Success -> {
                selected = emptySet()
                _state.update(onSuccess)
                reload()
            }
            is Result.Failure -> {
                _state.update { it.copy(error = result.error.toUiText()) }
                // A partial change may have happened (e.g. a few files deleted before an error).
                reload(keepError = true)
            }
        }
    }

    private fun publish() {
        _state.update { s ->
            s.copy(
                entries = listing.map { e ->
                    FileEntryUi(
                        path = e.path,
                        name = e.name,
                        isDirectory = e.isDirectory,
                        isShared = e.isDirectory && e.path.trimEnd('/') in sharedPaths,
                        size = if (e.isDirectory) "" else formatBytes(e.sizeBytes),
                        selected = e.path in selected,
                    )
                },
            )
        }
    }

    private companion object {
        const val KEY_DIRECTORY = "directory"
    }
}
