package com.adamfoerster.syncthing.feature.folders.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgAvatarStack
import com.adamfoerster.syncthing.core.designsystem.components.NgBadge
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgIconButton
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgProgressBar
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgTile
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.presentation.CoreStatusCallout
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.folders.presentation.resources.Res
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_add
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_conflicts
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_error
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_local
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_paused
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_scanning
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_syncing
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_up_to_date
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_badge_waiting
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_empty_body
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_empty_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_offer_accept
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_offer_dismiss
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_offer_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_open
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_progress
import com.adamfoerster.syncthing.feature.folders.presentation.resources.folders_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FolderListRoot(
    tabBar: @Composable () -> Unit,
    onOpenConflicts: () -> Unit,
    onOpenSharing: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenIgnores: (folderId: String) -> Unit,
    onAcceptOffer: (folderId: String, label: String, deviceId: String) -> Unit,
    viewModel: FolderListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    FolderListScreen(
        state = state,
        tabBar = tabBar,
        onAction = { action ->
            when (action) {
                is FolderListAction.OnFolderClick -> if (action.hasConflicts) onOpenConflicts() else onOpenSharing()
                FolderListAction.OnAddClick -> onAddFolder()
                is FolderListAction.OnIgnoreClick -> onOpenIgnores(action.id)
                is FolderListAction.OnAcceptOffer -> onAcceptOffer(action.offer.folderId, action.offer.label, action.offer.deviceId)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
fun FolderListScreen(
    state: FolderListState,
    tabBar: @Composable () -> Unit,
    onAction: (FolderListAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(stringResource(Res.string.folders_title)) {
                if (state.isLoaded) {
                    NgIconButton(NgIcons.Plus, stringResource(Res.string.folders_add), { onAction(FolderListAction.OnAddClick) }, bordered = true)
                }
            }
        },
        bottomBar = tabBar,
    ) {
        if (state.coreState != CoreState.Running || !state.isLoaded) {
            CoreStatusCallout(
                if (state.coreState == CoreState.Running) CoreState.Starting else state.coreState,
                onStart = { onAction(FolderListAction.OnStartCore) },
            )
            return@NgScreen
        }

        state.offers.forEach { offer ->
            NgCallout(
                body = stringResource(Res.string.folders_offer_title, offer.deviceName, offer.label),
                icon = NgIcons.Folder,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NgButton(
                        stringResource(Res.string.folders_offer_dismiss),
                        onClick = { onAction(FolderListAction.OnDismissOffer(offer)) },
                        modifier = Modifier.weight(1f),
                    )
                    NgButton(
                        stringResource(Res.string.folders_offer_accept),
                        onClick = { onAction(FolderListAction.OnAcceptOffer(offer)) },
                        style = NgButtonStyle.Primary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (state.folders.isEmpty()) {
            NgCallout(
                title = stringResource(Res.string.folders_empty_title),
                body = stringResource(Res.string.folders_empty_body),
                icon = NgIcons.Folder,
                iconSize = 24.dp,
            ) {
                NgButton(
                    stringResource(Res.string.folders_add),
                    onClick = { onAction(FolderListAction.OnAddClick) },
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Plus,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        state.folders.forEach { folder ->
            NgPanel(
                modifier = Modifier.fillMaxWidth(),
                tone = if (folder.badge.tone == Tone.Warn) PanelTone.Warn else PanelTone.Default,
                onClick = { onAction(FolderListAction.OnFolderClick(folder.id, folder.conflicts > 0)) },
                onClickLabel = stringResource(Res.string.folders_open, folder.label),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NgTile(NgIcons.Folder)
                    Column(Modifier.weight(1f)) {
                        Text(folder.label, style = NgTheme.type.h3, color = c.text)
                        Text(folder.size, style = NgTheme.type.monoSmall, color = c.text2)
                    }
                    FolderBadgeView(folder.badge)
                }
                if (folder.progress != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        NgProgressBar(folder.progress, label = folder.label)
                        Text(
                            stringResource(Res.string.folders_progress, folder.percentText.orEmpty(), folder.remainingText.orEmpty()),
                            style = NgTheme.type.monoSmall,
                            color = c.text2,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (folder.deviceInitials.isNotEmpty()) NgAvatarStack(folder.deviceInitials.take(4))
                    Text(folder.devicesLine.asString(), style = NgTheme.type.sub, color = c.text2)
                }
                NgButton(
                    stringResource(Res.string.ignore_title),
                    onClick = { onAction(FolderListAction.OnIgnoreClick(folder.id)) },
                    icon = NgIcons.Block,
                    minHeight = 40.dp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun FolderBadgeView(badge: FolderBadge) {
    val (text, icon) = when (badge.kind) {
        FolderBadgeKind.UP_TO_DATE -> stringResource(Res.string.folders_badge_up_to_date) to NgIcons.Check
        FolderBadgeKind.CONFLICTS -> pluralStringResource(Res.plurals.folders_badge_conflicts, badge.count, badge.count) to NgIcons.Warning
        FolderBadgeKind.SYNCING -> stringResource(Res.string.folders_badge_syncing) to NgIcons.Transfer
        FolderBadgeKind.SCANNING -> stringResource(Res.string.folders_badge_scanning) to NgIcons.StatusDiamond
        FolderBadgeKind.WAITING -> stringResource(Res.string.folders_badge_waiting) to NgIcons.Warning
        FolderBadgeKind.ERROR -> stringResource(Res.string.folders_badge_error) to NgIcons.Warning
        FolderBadgeKind.PAUSED -> stringResource(Res.string.folders_badge_paused) to NgIcons.Pause
        FolderBadgeKind.LOCAL_ONLY -> stringResource(Res.string.folders_badge_local) to NgIcons.Phone
    }
    NgBadge(text, tone = badge.tone, icon = icon)
}

@Preview
@Composable
private fun FolderListScreenPreview() {
    SyncthingTheme {
        FolderListScreen(
            state = FolderListState(
                coreState = CoreState.Running,
                isLoaded = true,
                folders = listOf(
                    FolderUi("f", "Fotos da câmera", "4,2 GB", FolderBadge(FolderBadgeKind.UP_TO_DATE, Tone.Ok), null, null, null, listOf("N", "P"), UiText.DynamicString("Com 2 aparelhos"), 0),
                    FolderUi("d", "Documentos", "312 MB", FolderBadge(FolderBadgeKind.CONFLICTS, Tone.Warn, 2), null, null, null, listOf("N", "P", "T"), UiText.DynamicString("Com 3 aparelhos"), 2),
                    FolderUi("b", "Backup do WhatsApp", "1,1 GB", FolderBadge(FolderBadgeKind.SYNCING, Tone.Info), 0.64f, "64%", "410 MB", listOf("N"), UiText.DynamicString("Com 1 aparelho"), 0),
                ),
            ),
            tabBar = {},
            onAction = {},
        )
    }
}
