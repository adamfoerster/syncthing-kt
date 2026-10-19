package com.adamfoerster.syncthing.feature.conflicts.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgBadge
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgChip
import com.adamfoerster.syncthing.core.designsystem.components.NgChips
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTile
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.HelperText
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionAvatar
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionUi
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.Res
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_chip
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_empty_body
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_empty_title
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_folder_label
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_footer
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_open
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_pending_badge
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_scanning
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_title
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_what_body
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.conflicts_what_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ConflictListRoot(
    onBack: () -> Unit,
    onOpenConflict: (id: String, kind: ConflictKind) -> Unit,
    viewModel: ConflictListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConflictListScreen(
        state = state,
        onAction = { action ->
            when (action) {
                ConflictListAction.OnBackClick -> onBack()
                is ConflictListAction.OnConflictClick -> onOpenConflict(action.id, action.kind)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
fun ConflictListScreen(
    state: ConflictListState,
    onAction: (ConflictListAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                title = stringResource(Res.string.conflicts_title),
                onBack = { onAction(ConflictListAction.OnBackClick) },
            ) {
                if (state.total > 0) {
                    NgBadge(pluralStringResource(Res.plurals.conflicts_pending_badge, state.total, state.total), tone = Tone.Warn)
                }
            }
        },
        bottomBar = {
            NgActionBar { HelperText(stringResource(Res.string.conflicts_footer)) }
        },
    ) {
        NgCallout(
            title = stringResource(Res.string.conflicts_what_title),
            body = stringResource(Res.string.conflicts_what_body),
        )

        when {
            state.isLoading -> Text(stringResource(Res.string.conflicts_scanning), style = NgTheme.type.sub, color = c.text2)
            state.groups.isEmpty() -> NgCallout(
                title = stringResource(Res.string.conflicts_empty_title),
                body = stringResource(Res.string.conflicts_empty_body),
                tone = PanelTone.Ok,
                icon = NgIcons.Check,
            )
            else -> state.groups.forEach { group ->
                NgSectionLabel(stringResource(Res.string.conflicts_folder_label, group.folderLabel))
                group.items.forEach { item ->
                    ConflictCard(item, onClick = { onAction(ConflictListAction.OnConflictClick(item.id, item.kind)) })
                }
            }
        }
    }
}

@Composable
private fun ConflictCard(item: ConflictItemUi, onClick: () -> Unit) {
    val c = NgTheme.colors
    NgPanel(
        tone = PanelTone.Warn,
        onClick = onClick,
        onClickLabel = stringResource(Res.string.conflicts_open, item.fileName),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            NgTile(
                when (item.kind) {
                    ConflictKind.TEXT -> NgIcons.TextFile
                    ConflictKind.IMAGE -> NgIcons.Image
                    ConflictKind.BINARY -> NgIcons.File
                },
            )
            Column(Modifier.weight(1f)) {
                Text(item.fileName, style = NgTheme.type.mono14, color = c.text)
                Text(item.detail.asString(), style = NgTheme.type.sub, color = c.text2)
            }
            Icon(NgIcons.Chevron, null, tint = c.text, modifier = Modifier.size(20.dp))
        }
        NgChips {
            VersionChip(item.first, showSize = item.kind != ConflictKind.TEXT)
            VersionChip(item.second, showSize = item.kind != ConflictKind.TEXT)
        }
    }
}

@Composable
private fun VersionChip(version: VersionUi, showSize: Boolean) {
    val detail = if (showSize) version.size else version.clock
    val author = version.author.asString()
    NgChip(
        text = if (detail != null) stringResource(Res.string.conflicts_chip, author, detail) else author,
        leading = { VersionAvatar(version) },
    )
}

@Preview
@Composable
private fun ConflictListScreenPreview() {
    val mine = VersionUi(VersionSide.CURRENT, UiText.DynamicString("Este celular"), "E", true, "14:32", null, "2,1 MB")
    val theirs = VersionUi(VersionSide.COPY, UiText.DynamicString("Notebook da Ana"), "N", false, "14:35", null, "3,4 MB")
    SyncthingTheme {
        ConflictListScreen(
            state = ConflictListState(
                groups = listOf(
                    ConflictGroupUi(
                        "Documentos",
                        listOf(
                            ConflictItemUi("1", "lista-de-compras.txt", ConflictKind.TEXT, UiText.DynamicString("2 trechos diferentes"), mine, theirs),
                            ConflictItemUi("2", "convite.png", ConflictKind.IMAGE, UiText.DynamicString("Imagem: escolha uma das versões"), mine, theirs),
                        ),
                    ),
                ),
                total = 2,
                isLoading = false,
            ),
            onAction = {},
        )
    }
}
