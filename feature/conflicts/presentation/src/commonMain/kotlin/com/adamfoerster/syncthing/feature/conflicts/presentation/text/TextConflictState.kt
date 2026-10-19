package com.adamfoerster.syncthing.feature.conflicts.presentation.text

import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionUi

data class TextConflictState(
    val phase: ResolverPhase = ResolverPhase.LOADING,
    val fileName: String = "",
    val folderLabel: String = "",
    val first: VersionUi? = null,
    val second: VersionUi? = null,
    val blocks: List<DiffBlockUi> = emptyList(),
    val hunkTotal: Int = 0,
    val resolvedCount: Int = 0,
    val view: TextView = TextView.COMPARE,
    val result: List<ResultLineUi> = emptyList(),
    /** GitHub-style manual edit of the result, starting from the text with conflict markers. */
    val isEditing: Boolean = false,
    val editorText: String = "",
    val editorHasMarkers: Boolean = false,
    val isSaving: Boolean = false,
    val error: UiText? = null,
) {
    val canFinish: Boolean
        get() = phase == ResolverPhase.READY && !isSaving &&
            if (isEditing) !editorHasMarkers else resolvedCount == hunkTotal
}

enum class ResolverPhase { LOADING, NOT_FOUND, READY, DONE }

enum class TextView { COMPARE, RESULT }

/** Which version a passage keeps. [BOTH] keeps the first version's lines, then the second's. */
enum class Pick { FIRST, SECOND, BOTH }

sealed interface DiffBlockUi {
    /** Unchanged lines; long runs are folded to a few lines around the hunks. */
    data class Context(
        val blockIndex: Int,
        val head: List<NumberedLine>,
        val hiddenCount: Int,
        val tail: List<NumberedLine>,
    ) : DiffBlockUi

    data class Hunk(
        val hunkIndex: Int,
        val number: Int,
        val pick: Pick?,
        val firstLines: List<HunkLineUi>,
        val secondLines: List<HunkLineUi>,
    ) : DiffBlockUi
}

data class NumberedLine(val number: Int, val text: String)

data class HunkLineUi(
    val number: Int,
    val text: String,
    /** The part that differs from the paired line on the other side. */
    val highlight: IntRange?,
)

data class ResultLineUi(val number: Int, val text: String, val origin: LineOrigin)

enum class LineOrigin { COMMON, FIRST, SECOND, PENDING }

sealed interface TextConflictAction {
    data object OnBackClick : TextConflictAction
    data class OnPick(val hunkIndex: Int, val pick: Pick) : TextConflictAction
    data class OnPickAll(val pick: Pick) : TextConflictAction
    data class OnViewChange(val view: TextView) : TextConflictAction
    data class OnExpandContext(val blockIndex: Int) : TextConflictAction
    data object OnStartEditing : TextConflictAction
    data class OnEditorChange(val text: String) : TextConflictAction
    data object OnDiscardEdit : TextConflictAction
    data object OnFinish : TextConflictAction
}
