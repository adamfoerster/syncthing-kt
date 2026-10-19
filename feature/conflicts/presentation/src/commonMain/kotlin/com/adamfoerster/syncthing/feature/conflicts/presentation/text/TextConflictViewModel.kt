package com.adamfoerster.syncthing.feature.conflicts.presentation.text

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.presentation.toUiText
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.TextVersions
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.HunkResolution
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.InlineHighlight
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeBlock
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeDocument
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.orderedVersions
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.toUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TextConflictViewModel(
    private val conflictId: String,
    private val repository: ConflictRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(TextConflictState())
    val state = _state.asStateFlow()

    private var conflict: Conflict? = null
    private var loaded: TextVersions? = null
    private var document: MergeDocument? = null
    private var firstSide = VersionSide.CURRENT
    private val expanded = mutableSetOf<Int>()

    /** Survives process death: hunk index → pick. */
    private var picks: Map<Int, Pick>
        get() = savedStateHandle.get<String>(KEY_PICKS).orEmpty().split(',').filter { it.isNotBlank() }
            .associate { entry -> entry.substringBefore(':').toInt() to Pick.valueOf(entry.substringAfter(':')) }
        set(value) {
            savedStateHandle[KEY_PICKS] = value.entries.joinToString(",") { "${it.key}:${it.value.name}" }
        }

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
        when (val text = repository.loadText(found)) {
            is Result.Failure -> _state.update { it.copy(phase = ResolverPhase.NOT_FOUND, error = text.error.toUiText()) }
            is Result.Success -> {
                val (first, second) = found.orderedVersions()
                conflict = found
                loaded = text.data
                firstSide = first.side
                document = MergeDocument.of(text.data.current, text.data.copy)
                _state.update {
                    it.copy(
                        phase = ResolverPhase.READY,
                        fileName = found.fileName,
                        folderLabel = found.folderLabel,
                        first = first.toUi(),
                        second = second.toUi(),
                    )
                }
                rebuild()
            }
        }
    }

    fun onAction(action: TextConflictAction) {
        when (action) {
            is TextConflictAction.OnPick -> {
                val current = picks
                // Tapping the selected option again clears it, as in the mockup.
                picks = if (current[action.hunkIndex] == action.pick) current - action.hunkIndex else current + (action.hunkIndex to action.pick)
                rebuild()
            }
            is TextConflictAction.OnPickAll -> {
                picks = document?.hunks.orEmpty().associate { it.index to action.pick }
                rebuild()
            }
            is TextConflictAction.OnViewChange -> _state.update { it.copy(view = action.view) }
            is TextConflictAction.OnExpandContext -> {
                expanded += action.blockIndex
                rebuild()
            }
            TextConflictAction.OnStartEditing -> startEditing()
            is TextConflictAction.OnEditorChange -> _state.update {
                it.copy(editorText = action.text, editorHasMarkers = MergeDocument.hasConflictMarkers(action.text))
            }
            TextConflictAction.OnDiscardEdit -> _state.update { it.copy(isEditing = false, editorText = "", editorHasMarkers = false) }
            TextConflictAction.OnFinish -> finish()
            TextConflictAction.OnBackClick -> Unit
        }
    }

    private fun startEditing() {
        val doc = document ?: return
        val target = conflict ?: return
        // Marker labels end up inside the file, so they use device names rather than UI strings.
        val text = doc.renderWithMarkers(
            resolutions = resolutions(),
            currentLabel = target.current.author?.name ?: target.fileName,
            copyLabel = target.copy.author?.name ?: target.copy.path.substringAfterLast('/'),
        )
        _state.update {
            it.copy(
                view = TextView.RESULT,
                isEditing = true,
                editorText = text,
                editorHasMarkers = MergeDocument.hasConflictMarkers(text),
            )
        }
    }

    private fun finish() {
        val target = conflict ?: return
        val versions = loaded ?: return
        val s = _state.value
        if (!s.canFinish) return
        val merged = if (s.isEditing) s.editorText else document?.render(resolutions()) ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            when (val result = repository.resolveText(target, versions, merged)) {
                is Result.Success -> _state.update { it.copy(isSaving = false, phase = ResolverPhase.DONE) }
                is Result.Failure -> _state.update { it.copy(isSaving = false, error = result.error.toUiText()) }
            }
        }
    }

    private fun resolutions(): Map<Int, HunkResolution> = picks.mapValues { (_, pick) -> pick.toResolution() }

    private fun Pick.toResolution(): HunkResolution {
        val firstIsCurrent = firstSide == VersionSide.CURRENT
        return when (this) {
            Pick.FIRST -> if (firstIsCurrent) HunkResolution.CURRENT else HunkResolution.COPY
            Pick.SECOND -> if (firstIsCurrent) HunkResolution.COPY else HunkResolution.CURRENT
            Pick.BOTH -> if (firstIsCurrent) HunkResolution.BOTH_CURRENT_FIRST else HunkResolution.BOTH_COPY_FIRST
        }
    }

    /** Recomputes the compare rows and the result preview from the document and current picks. */
    private fun rebuild() {
        val doc = document ?: return
        val picks = picks
        val firstIsCurrent = firstSide == VersionSide.CURRENT
        val total = doc.hunks.size
        val blocks = ArrayList<DiffBlockUi>()
        val result = ArrayList<ResultLineUi>()
        var line = 1
        var resultLine = 1

        doc.blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is MergeBlock.Common -> {
                    val numbered = block.lines.mapIndexed { i, text -> NumberedLine(line + i, text) }
                    line += numbered.size
                    blocks += foldContext(blockIndex, numbered, isFirst = blockIndex == 0, isLast = blockIndex == doc.blocks.lastIndex)
                    block.lines.forEach { result += ResultLineUi(resultLine++, it, LineOrigin.COMMON) }
                }
                is MergeBlock.Hunk -> {
                    val firstLines = if (firstIsCurrent) block.current else block.copy
                    val secondLines = if (firstIsCurrent) block.copy else block.current
                    val pick = picks[block.index]
                    blocks += DiffBlockUi.Hunk(
                        hunkIndex = block.index,
                        number = block.index + 1,
                        pick = pick,
                        firstLines = highlightLines(firstLines, secondLines, line, isFirst = true),
                        secondLines = highlightLines(secondLines, firstLines, line, isFirst = false),
                    )
                    line += when (pick) {
                        Pick.SECOND -> secondLines.size
                        Pick.BOTH -> firstLines.size + secondLines.size
                        else -> firstLines.size
                    }
                    when (pick) {
                        null -> result += ResultLineUi(resultLine++, "${block.index + 1}", LineOrigin.PENDING)
                        Pick.FIRST -> firstLines.forEach { result += ResultLineUi(resultLine++, it, LineOrigin.FIRST) }
                        Pick.SECOND -> secondLines.forEach { result += ResultLineUi(resultLine++, it, LineOrigin.SECOND) }
                        Pick.BOTH -> {
                            firstLines.forEach { result += ResultLineUi(resultLine++, it, LineOrigin.FIRST) }
                            secondLines.forEach { result += ResultLineUi(resultLine++, it, LineOrigin.SECOND) }
                        }
                    }
                }
            }
        }
        _state.update {
            it.copy(
                blocks = blocks,
                hunkTotal = total,
                resolvedCount = picks.keys.count { index -> index < total },
                result = result,
            )
        }
    }

    private fun foldContext(blockIndex: Int, lines: List<NumberedLine>, isFirst: Boolean, isLast: Boolean): DiffBlockUi.Context {
        if (lines.size <= FOLD_THRESHOLD || blockIndex in expanded) {
            return DiffBlockUi.Context(blockIndex, lines, 0, emptyList())
        }
        // Keep a few lines of context next to each hunk; the file's start/end needs none on its outer side.
        val head = if (isFirst) emptyList() else lines.take(CONTEXT_LINES)
        val tail = if (isLast) emptyList() else lines.takeLast(CONTEXT_LINES)
        return DiffBlockUi.Context(blockIndex, head, lines.size - head.size - tail.size, tail)
    }

    private fun highlightLines(lines: List<String>, other: List<String>, start: Int, isFirst: Boolean): List<HunkLineUi> =
        lines.mapIndexed { i, text ->
            val spans = other.getOrNull(i)?.let { counterpart ->
                if (isFirst) InlineHighlight.between(text, counterpart) else InlineHighlight.between(counterpart, text)
            }
            val span = if (isFirst) spans?.first else spans?.second
            HunkLineUi(start + i, text, span?.takeUnless { it.isEmpty }?.let { it.start until it.end })
        }

    private companion object {
        const val KEY_PICKS = "picks"
        const val FOLD_THRESHOLD = 8
        const val CONTEXT_LINES = 3
    }
}
