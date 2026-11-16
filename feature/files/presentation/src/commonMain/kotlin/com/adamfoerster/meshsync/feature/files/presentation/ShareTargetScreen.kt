package com.adamfoerster.meshsync.feature.files.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.meshsync.core.designsystem.components.NgButton
import com.adamfoerster.meshsync.core.designsystem.components.NgButtonStyle
import com.adamfoerster.meshsync.core.designsystem.components.NgCallout
import com.adamfoerster.meshsync.core.designsystem.components.NgFolderIcon
import com.adamfoerster.meshsync.core.designsystem.components.NgPanel
import com.adamfoerster.meshsync.core.designsystem.components.NgScreen
import com.adamfoerster.meshsync.core.designsystem.components.NgTopBar
import com.adamfoerster.meshsync.core.designsystem.components.PanelTone
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme
import com.adamfoerster.meshsync.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.meshsync.core.presentation.ObserveAsEvents
import com.adamfoerster.meshsync.feature.files.presentation.resources.Res
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_cancel
import com.adamfoerster.meshsync.feature.files.presentation.resources.files_confirm
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_choose
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_done
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_items
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_no_folders
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_paused
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_saving
import com.adamfoerster.meshsync.feature.files.presentation.resources.share_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareTargetRoot(onExit: () -> Unit) {
    val viewModel: ShareTargetViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ShareTargetEvent.Exit -> onExit()
        }
    }
    ShareTargetScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun ShareTargetScreen(
    state: ShareTargetState,
    onAction: (ShareTargetAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                title = stringResource(Res.string.share_title),
                subtitle = if (state.itemCount > 0) pluralStringResource(Res.plurals.share_items, state.itemCount, state.itemCount) else null,
                onBack = { onAction(ShareTargetAction.OnCancel) },
            )
        },
    ) {
        state.error?.let { error ->
            NgCallout(body = error.asString(), tone = PanelTone.Warn) {
                NgButton(stringResource(Res.string.files_confirm), onClick = { onAction(ShareTargetAction.OnDismissError) }, minHeight = 40.dp)
            }
        }
        val done = state.done
        if (done != null) {
            NgCallout(
                body = pluralStringResource(Res.plurals.share_done, done.copied, done.copied, done.folderLabel),
                tone = PanelTone.Ok,
            ) {
                NgButton(stringResource(Res.string.files_confirm), onClick = { onAction(ShareTargetAction.OnDone) }, style = NgButtonStyle.Primary, minHeight = 40.dp)
            }
            return@NgScreen
        }
        if (state.folders.isEmpty()) {
            Text(stringResource(Res.string.share_no_folders), style = NgTheme.type.sub, color = c.text2)
        } else {
            Text(
                stringResource(if (state.isImporting) Res.string.share_saving else Res.string.share_choose),
                style = NgTheme.type.sub,
                color = c.text2,
            )
        }
        state.folders.forEach { folder ->
            NgPanel(
                onClick = if (state.isImporting) null else ({ onAction(ShareTargetAction.OnFolderClick(folder.id)) }),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NgFolderIcon(shared = true)
                    Text(folder.label, style = NgTheme.type.body, color = c.text, modifier = Modifier.weight(1f))
                    if (folder.paused) Text(stringResource(Res.string.share_paused), style = NgTheme.type.monoSmall, color = c.text2)
                }
                Text(folder.path, style = NgTheme.type.monoSmall, color = c.text2)
            }
        }
        NgButton(stringResource(Res.string.files_cancel), onClick = { onAction(ShareTargetAction.OnCancel) }, minHeight = 40.dp)
    }
}

@Preview
@Composable
private fun ShareTargetPreview() {
    SyncthingTheme(darkTheme = false) {
        ShareTargetScreen(
            state = ShareTargetState(
                itemCount = 2,
                folders = listOf(
                    ShareFolderUi("a", "Photos", "/storage/emulated/0/Photos", false),
                    ShareFolderUi("b", "Documents", "/storage/emulated/0/Documents", true),
                ),
            ),
            onAction = {},
        )
    }
}
