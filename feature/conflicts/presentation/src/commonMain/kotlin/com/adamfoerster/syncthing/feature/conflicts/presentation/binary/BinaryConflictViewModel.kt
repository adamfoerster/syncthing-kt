package com.adamfoerster.syncthing.feature.conflicts.presentation.binary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.domain.util.getOrNull
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.toUiText
import com.adamfoerster.syncthing.feature.conflicts.domain.BinaryChoice
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictVersion
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.orderedVersions
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.toUi
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.Res
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_copy_suffix
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_done_both
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_done_mine
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_done_one
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.ResolverPhase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

class BinaryConflictViewModel(
    private val conflictId: String,
    private val repository: ConflictRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BinaryConflictState(pick = savedStateHandle.get<String>(KEY_PICK)?.let(BinaryPick::valueOf)),
    )
    val state = _state.asStateFlow()

    private var conflict: Conflict? = null
    private var copySuffix = ""

    init {
        load()
    }

    private fun load() = viewModelScope.launch {
        val found = repository.find(conflictId) ?: run {
            repository.rescan()
            repository.find(conflictId)
        }
        if (found == null) {
            _state.update { it.copy(phase = ResolverPhase.NOT_FOUND) }
            return@launch
        }
        conflict = found
        copySuffix = getString(Res.string.binary_copy_suffix)
        val (first, second) = found.orderedVersions()
        val isImage = found.kind == ConflictKind.IMAGE
        val previews = if (isImage) {
            val a = async { preview(found, first) }
            val b = async { preview(found, second) }
            a.await() to b.await()
        } else {
            null to null
        }
        val (base, ext) = found.fileName.let { name ->
            val dot = name.lastIndexOf('.')
            if (dot <= 0) name to "" else name.substring(0, dot) to name.substring(dot)
        }
        _state.update {
            it.copy(
                phase = ResolverPhase.READY,
                fileName = found.fileName,
                folderLabel = found.folderLabel,
                isImage = isImage,
                first = BinaryVersionUi(first.toUi(), previews.first),
                second = BinaryVersionUi(second.toUi(), previews.second),
                keepBothName = "$base ($copySuffix)$ext",
            )
        }
    }

    private suspend fun preview(conflict: Conflict, version: ConflictVersion): ImagePreview? =
        repository.loadBytes(conflict, version.side).getOrNull()?.let { bytes ->
            withContext(Dispatchers.Default) { decodeImagePreview(bytes, PREVIEW_MAX_SIDE) }
        }

    fun onAction(action: BinaryConflictAction) {
        when (action) {
            is BinaryConflictAction.OnPick -> {
                savedStateHandle[KEY_PICK] = action.pick.name
                _state.update { it.copy(pick = action.pick, error = null) }
            }
            BinaryConflictAction.OnConfirm -> confirm()
            BinaryConflictAction.OnBackClick -> Unit
        }
    }

    private fun confirm() {
        val target = conflict ?: return
        val s = _state.value
        val pick = s.pick ?: return
        if (!s.canConfirm) return
        val first = target.orderedVersions().first
        val choice = when (pick) {
            BinaryPick.FIRST -> if (first.side == VersionSide.CURRENT) BinaryChoice.KEEP_CURRENT else BinaryChoice.KEEP_COPY
            BinaryPick.SECOND -> if (first.side == VersionSide.CURRENT) BinaryChoice.KEEP_COPY else BinaryChoice.KEEP_CURRENT
            BinaryPick.BOTH -> BinaryChoice.KEEP_BOTH
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.resolveBinary(target, choice, copySuffix)) {
                is Result.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
                is Result.Success -> _state.update {
                    val kept = if (pick == BinaryPick.SECOND) it.second else it.first
                    it.copy(
                        isSaving = false,
                        phase = ResolverPhase.DONE,
                        doneMessage = when (pick) {
                            BinaryPick.BOTH -> UiText.Resource(Res.string.binary_done_both, target.fileName, result.data)
                            else -> if (kept?.version?.isLocal == true) {
                                UiText.Resource(Res.string.binary_done_mine)
                            } else {
                                UiText.Resource(Res.string.binary_done_one, kept?.version?.author ?: UiText.DynamicString(""))
                            }
                        },
                    )
                }
            }
        }
    }

    private companion object {
        const val KEY_PICK = "pick"
        const val PREVIEW_MAX_SIDE = 720
    }
}
