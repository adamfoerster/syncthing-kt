package com.adamfoerster.syncthing.feature.conflicts.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictList
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.orderedVersions
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.toUi
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.Res
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_hunks
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_kind_binary
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_kind_image
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_same_content
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConflictListViewModel(
    private val repository: ConflictRepository,
) : ViewModel() {

    val state: StateFlow<ConflictListState> = repository.conflicts
        .map { it.toState() }
        .onStart { viewModelScope.launch { repository.rescan() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConflictListState())

    fun onAction(action: ConflictListAction) {
        when (action) {
            ConflictListAction.OnRefresh -> viewModelScope.launch { repository.rescan() }
            // Navigation is handled by the Root.
            is ConflictListAction.OnConflictClick, ConflictListAction.OnBackClick -> Unit
        }
    }

    private fun ConflictList.toState() = ConflictListState(
        groups = items.groupBy { it.folderLabel }.map { (folder, conflicts) ->
            ConflictGroupUi(folder, conflicts.map { it.toItemUi() })
        },
        total = items.size,
        isLoading = !isLoaded,
        isScanning = isScanning,
    )

    private fun Conflict.toItemUi(): ConflictItemUi {
        val (first, second) = orderedVersions()
        return ConflictItemUi(
            id = id,
            fileName = fileName,
            kind = kind,
            detail = when (kind) {
                ConflictKind.TEXT -> when (val hunks = hunkCount ?: 0) {
                    0 -> UiText.Resource(Res.string.conflicts_same_content)
                    else -> UiText.Plural(Res.plurals.conflicts_hunks, hunks, hunks)
                }
                ConflictKind.IMAGE -> UiText.Resource(Res.string.conflicts_kind_image)
                ConflictKind.BINARY -> UiText.Resource(Res.string.conflicts_kind_binary)
            },
            first = first.toUi(),
            second = second.toUi(),
        )
    }
}
