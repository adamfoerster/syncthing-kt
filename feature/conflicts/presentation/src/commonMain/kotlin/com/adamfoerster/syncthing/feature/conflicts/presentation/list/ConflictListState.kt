package com.adamfoerster.syncthing.feature.conflicts.presentation.list

import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionUi

data class ConflictListState(
    val groups: List<ConflictGroupUi> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
)

data class ConflictGroupUi(
    val folderLabel: String,
    val items: List<ConflictItemUi>,
)

data class ConflictItemUi(
    val id: String,
    val fileName: String,
    val kind: ConflictKind,
    val detail: UiText,
    val first: VersionUi,
    val second: VersionUi,
)

sealed interface ConflictListAction {
    data object OnBackClick : ConflictListAction
    data object OnRefresh : ConflictListAction
    data class OnConflictClick(val id: String, val kind: ConflictKind) : ConflictListAction
}
