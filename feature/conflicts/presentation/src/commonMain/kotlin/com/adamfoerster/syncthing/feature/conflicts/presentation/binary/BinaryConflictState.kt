package com.adamfoerster.syncthing.feature.conflicts.presentation.binary

import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionUi
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.ResolverPhase

data class BinaryConflictState(
    val phase: ResolverPhase = ResolverPhase.LOADING,
    val fileName: String = "",
    val folderLabel: String = "",
    val isImage: Boolean = false,
    val first: BinaryVersionUi? = null,
    val second: BinaryVersionUi? = null,
    val pick: BinaryPick? = null,
    /** Name the second file gets with "keep both", e.g. "convite (cópia).png". */
    val keepBothName: String = "",
    val isSaving: Boolean = false,
    val error: UiText? = null,
    val doneMessage: UiText? = null,
) {
    val canConfirm: Boolean get() = pick != null && !isSaving && phase == ResolverPhase.READY
}

data class BinaryVersionUi(
    val version: VersionUi,
    val preview: ImagePreview?,
)

enum class BinaryPick { FIRST, SECOND, BOTH }

sealed interface BinaryConflictAction {
    data object OnBackClick : BinaryConflictAction
    data class OnPick(val pick: BinaryPick) : BinaryConflictAction
    data object OnConfirm : BinaryConflictAction
}
