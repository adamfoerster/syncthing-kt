package com.adamfoerster.meshsync.feature.files.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.meshsync.core.designsystem.components.NgActionBar
import com.adamfoerster.meshsync.core.designsystem.components.NgButton
import com.adamfoerster.meshsync.core.designsystem.components.NgButtonStyle
import com.adamfoerster.meshsync.core.designsystem.components.NgCallout
import com.adamfoerster.meshsync.core.designsystem.components.NgFolderIcon
import com.adamfoerster.meshsync.core.designsystem.components.NgIconButton
import com.adamfoerster.meshsync.core.designsystem.components.NgPanel
import com.adamfoerster.meshsync.core.designsystem.components.NgScreen
import com.adamfoerster.meshsync.core.designsystem.components.NgTextField
import com.adamfoerster.meshsync.core.designsystem.components.NgTopBar
import com.adamfoerster.meshsync.core.designsystem.components.PanelTone
import com.adamfoerster.meshsync.core.designsystem.icons.NgIcons
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme
import com.adamfoerster.meshsync.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.meshsync.core.presentation.LocalPlatformActions
import com.adamfoerster.meshsync.core.presentation.ObserveAsEvents
import com.adamfoerster.meshsync.feature.files.presentation.resources.Res
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_cancel
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_clear_selection
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_clipboard_clear
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_clipboard_copy
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_clipboard_move
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_confirm
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_copy
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_cut
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_delete
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_delete_body
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_delete_title
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_empty
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_folder_synced
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_name_label
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_new_folder
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_paste
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_rename
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_selected
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_share
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_sync
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun FilesRoot(
    onBack: () -> Unit,
    onSyncFolder: (path: String) -> Unit,
) {
    val platform = LocalPlatformActions.current
    val viewModel: FilesViewModel = koinViewModel { parametersOf(platform.defaultFolderRoot) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            FilesEvent.Exit -> onBack()
            is FilesEvent.Share -> platform.shareFiles(event.paths)
            is FilesEvent.Sync -> onSyncFolder(event.path)
        }
    }
    FilesScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun FilesScreen(
    state: FilesState,
    onAction: (FilesAction) -> Unit,
) {
    val c = NgTheme.colors
    // Back climbs one folder (or drops the selection) before leaving the screen.
    BackHandler(enabled = !state.atRoot || state.isSelecting || state.dialog != null) { onAction(FilesAction.OnBackClick) }

    NgScreen(
        topBar = {
            val selection = state.selection
            NgTopBar(
                title = if (selection.isNotEmpty()) {
                    pluralStringResource(Res.plurals.files_selected, selection.size, selection.size)
                } else {
                    state.directory.trimEnd('/').substringAfterLast('/').ifEmpty { stringResource(Res.string.files_title) }
                },
                subtitle = state.directory.takeIf { selection.isEmpty() },
                onBack = { onAction(FilesAction.OnBackClick) },
            ) {
                if (selection.isNotEmpty()) {
                    NgIconButton(NgIcons.Close, stringResource(Res.string.files_clear_selection), { onAction(FilesAction.OnClearSelection) })
                } else {
                    NgIconButton(NgIcons.Plus, stringResource(Res.string.files_new_folder), { onAction(FilesAction.OnNewFolderClick) }, bordered = true)
                }
            }
        },
        bottomBar = if (state.isSelecting || state.clipboard != null) {
            { ActionsBar(state, onAction) }
        } else {
            null
        },
        scrollable = false,
        bodyArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.error?.let { error ->
            NgCallout(body = error.asString(), tone = PanelTone.Warn) {
                NgButton(stringResource(Res.string.files_confirm), onClick = { onAction(FilesAction.OnDismissError) }, minHeight = 40.dp)
            }
        }
        state.dialog?.let { DialogPanel(it, onAction) }

        if (!state.isLoading && state.entries.isEmpty()) {
            Text(stringResource(Res.string.files_empty), style = NgTheme.type.sub, color = c.text2)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(state.entries, key = { it.path }) { entry ->
                val syncedLabel = stringResource(Res.string.files_folder_synced)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 56.dp)
                        .background(if (entry.selected) c.cyanSub else c.panel)
                        .border(1.dp, if (entry.selected) c.cyanEdge else c.line)
                        .semantics { selected = entry.selected }
                        .combinedClickable(
                            role = Role.Button,
                            onClick = { onAction(FilesAction.OnEntryClick(entry.path)) },
                            onLongClick = { onAction(FilesAction.OnEntryLongClick(entry.path)) },
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (entry.isDirectory) {
                        NgFolderIcon(shared = entry.isShared, contentDescription = if (entry.isShared) syncedLabel else null)
                    } else {
                        Icon(NgIcons.File, null, tint = c.text2, modifier = Modifier.size(28.dp))
                    }
                    Text(
                        entry.name,
                        style = NgTheme.type.body,
                        color = c.text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (entry.size.isNotEmpty()) Text(entry.size, style = NgTheme.type.monoSmall, color = c.text2)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionsBar(state: FilesState, onAction: (FilesAction) -> Unit) {
    val c = NgTheme.colors
    NgActionBar {
        state.clipboard?.let { clipboard ->
            Text(
                pluralStringResource(
                    if (clipboard.move) Res.plurals.files_clipboard_move else Res.plurals.files_clipboard_copy,
                    clipboard.paths.size,
                    clipboard.paths.size,
                ),
                style = NgTheme.type.sub,
                color = c.text2,
            )
            // Own line for the text, then equal-width buttons: a long translation can no longer squeeze the label.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NgButton(
                    stringResource(Res.string.files_clipboard_clear),
                    onClick = { onAction(FilesAction.OnClearClipboard) },
                    minHeight = 40.dp,
                    modifier = Modifier.weight(1f),
                )
                NgButton(
                    stringResource(Res.string.files_paste),
                    onClick = { onAction(FilesAction.OnPaste) },
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Paste,
                    minHeight = 40.dp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (state.isSelecting) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canSync) {
                    NgButton(stringResource(Res.string.files_sync), { onAction(FilesAction.OnSync) }, style = NgButtonStyle.Primary, icon = NgIcons.Transfer, minHeight = 40.dp)
                }
                NgButton(stringResource(Res.string.files_copy), { onAction(FilesAction.OnCopy) }, icon = NgIcons.Copy, minHeight = 40.dp)
                NgButton(stringResource(Res.string.files_cut), { onAction(FilesAction.OnCut) }, icon = NgIcons.Cut, minHeight = 40.dp)
                if (state.canRename) {
                    NgButton(stringResource(Res.string.files_rename), { onAction(FilesAction.OnRenameClick) }, icon = NgIcons.Edit, minHeight = 40.dp)
                }
                if (state.canShare) {
                    NgButton(stringResource(Res.string.files_share), { onAction(FilesAction.OnShare) }, icon = NgIcons.Share, minHeight = 40.dp)
                }
                NgButton(stringResource(Res.string.files_delete), { onAction(FilesAction.OnDeleteClick) }, style = NgButtonStyle.Danger, icon = NgIcons.Delete, minHeight = 40.dp)
            }
        }
    }
}

/** Delete confirmation and the rename / new-folder prompts, shown above the list like the app's other callouts. */
@Composable
private fun DialogPanel(dialog: FilesDialog, onAction: (FilesAction) -> Unit) {
    val c = NgTheme.colors
    NgPanel(modifier = Modifier.fillMaxWidth(), tone = if (dialog is FilesDialog.Delete) PanelTone.Warn else PanelTone.Selected) {
        when (dialog) {
            is FilesDialog.Delete -> {
                Text(pluralStringResource(Res.plurals.files_delete_title, dialog.paths.size, dialog.paths.size), style = NgTheme.type.h3, color = c.text)
                Text(stringResource(Res.string.files_delete_body), style = NgTheme.type.sub, color = c.text2)
            }
            is FilesDialog.Rename -> NgTextField(
                value = dialog.name,
                onValueChange = { onAction(FilesAction.OnDialogTextChange(it)) },
                label = stringResource(Res.string.files_rename),
                placeholder = stringResource(Res.string.files_name_label),
            )
            is FilesDialog.NewFolder -> NgTextField(
                value = dialog.name,
                onValueChange = { onAction(FilesAction.OnDialogTextChange(it)) },
                label = stringResource(Res.string.files_new_folder),
                placeholder = stringResource(Res.string.files_name_label),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NgButton(stringResource(Res.string.files_cancel), { onAction(FilesAction.OnDismissDialog) }, modifier = Modifier.weight(1f))
            NgButton(
                stringResource(if (dialog is FilesDialog.Delete) Res.string.files_delete else Res.string.files_confirm),
                { onAction(FilesAction.OnConfirmDialog) },
                style = if (dialog is FilesDialog.Delete) NgButtonStyle.Danger else NgButtonStyle.Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
private fun FilesScreenPreview() {
    SyncthingTheme {
        FilesScreen(
            state = FilesState(
                root = "/storage/emulated/0",
                directory = "/storage/emulated/0",
                isLoading = false,
                entries = listOf(
                    FileEntryUi("/storage/emulated/0/DCIM", "DCIM", true, true, "", false),
                    FileEntryUi("/storage/emulated/0/Documents", "Documents", true, false, "", true),
                    FileEntryUi("/storage/emulated/0/notes.txt", "notes.txt", false, false, "4,2 KB", false),
                ),
            ),
            onAction = {},
        )
    }
}
