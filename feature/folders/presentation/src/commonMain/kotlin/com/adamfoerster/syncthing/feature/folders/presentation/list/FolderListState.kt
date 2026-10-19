package com.adamfoerster.syncthing.feature.folders.presentation.list

import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.presentation.UiText

data class FolderListState(
    val coreState: CoreState = CoreState.Starting,
    val isLoaded: Boolean = false,
    val folders: List<FolderUi> = emptyList(),
    val offers: List<FolderOfferUi> = emptyList(),
)

data class FolderUi(
    val id: String,
    val label: String,
    val size: String,
    val badge: FolderBadge,
    /** 0..1 while syncing. */
    val progress: Float?,
    val percentText: String?,
    val remainingText: String?,
    val deviceInitials: List<String>,
    val devicesLine: UiText,
    val conflicts: Int,
)

enum class FolderBadgeKind { UP_TO_DATE, CONFLICTS, SYNCING, SCANNING, WAITING, ERROR, PAUSED, LOCAL_ONLY }

data class FolderBadge(val kind: FolderBadgeKind, val tone: Tone, val count: Int = 0)

data class FolderOfferUi(
    val folderId: String,
    val label: String,
    val deviceId: String,
    val deviceName: String,
)

sealed interface FolderListAction {
    data class OnFolderClick(val id: String, val hasConflicts: Boolean) : FolderListAction
    data object OnAddClick : FolderListAction
    data class OnAcceptOffer(val offer: FolderOfferUi) : FolderListAction
    data class OnDismissOffer(val offer: FolderOfferUi) : FolderListAction
    data object OnStartCore : FolderListAction
}
