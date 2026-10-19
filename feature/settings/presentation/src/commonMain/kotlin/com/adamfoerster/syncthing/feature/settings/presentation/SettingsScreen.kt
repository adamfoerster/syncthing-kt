package com.adamfoerster.syncthing.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgStatusLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTextField
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.SyncStatus
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.presentation.CoreStatusCallout
import com.adamfoerster.syncthing.feature.settings.presentation.resources.Res
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_copy_id
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_device_id
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_history_body
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_history_title
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_name
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_name_saved
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_restart
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_save_name
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_start
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_state_error
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_state_running
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_state_starting
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_state_stopped
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_stop
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_sync
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_this_device
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_title
import com.adamfoerster.syncthing.feature.settings.presentation.resources.settings_version
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoot(
    tabBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(state, tabBar, viewModel::onAction)
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    tabBar: @Composable () -> Unit,
    onAction: (SettingsAction) -> Unit,
) {
    val c = NgTheme.colors
    val clipboard = LocalClipboardManager.current
    NgScreen(
        topBar = { NgTopBar(stringResource(Res.string.settings_title)) },
        bottomBar = tabBar,
    ) {
        if (state.coreState != CoreState.Running && state.coreState != CoreState.Stopped) {
            CoreStatusCallout(state.coreState, onStart = { onAction(SettingsAction.OnStart) })
        }

        NgPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NgSectionLabel(stringResource(Res.string.settings_sync), Modifier.weight(1f))
                val (status, label) = when (state.coreState) {
                    CoreState.Running -> SyncStatus.Connected to Res.string.settings_state_running
                    CoreState.Starting -> SyncStatus.Syncing to Res.string.settings_state_starting
                    CoreState.Stopped -> SyncStatus.Paused to Res.string.settings_state_stopped
                    else -> SyncStatus.Error to Res.string.settings_state_error
                }
                NgStatusLabel(status, stringResource(label))
            }
            if (state.version.isNotEmpty()) {
                Text(stringResource(Res.string.settings_version, state.version), style = NgTheme.type.monoSmall, color = c.text2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.coreState == CoreState.Stopped) {
                    NgButton(stringResource(Res.string.settings_start), { onAction(SettingsAction.OnStart) }, Modifier.weight(1f), style = NgButtonStyle.Primary, icon = NgIcons.Play)
                } else {
                    NgButton(stringResource(Res.string.settings_stop), { onAction(SettingsAction.OnStop) }, Modifier.weight(1f), style = NgButtonStyle.Danger, icon = NgIcons.Power)
                    NgButton(stringResource(Res.string.settings_restart), { onAction(SettingsAction.OnRestart) }, Modifier.weight(1f), icon = NgIcons.Restart)
                }
            }
        }

        if (state.myId.isNotEmpty()) {
            NgPanel(Modifier.fillMaxWidth()) {
                NgSectionLabel(stringResource(Res.string.settings_this_device))
                NgTextField(
                    value = state.name,
                    onValueChange = { onAction(SettingsAction.OnNameChange(it)) },
                    label = stringResource(Res.string.settings_name),
                    supportingText = when {
                        state.error != null -> state.error.asString()
                        state.nameSaved -> stringResource(Res.string.settings_name_saved)
                        else -> null
                    },
                    isError = state.error != null,
                )
                NgButton(
                    stringResource(Res.string.settings_save_name),
                    { onAction(SettingsAction.OnSaveName) },
                    Modifier.fillMaxWidth(),
                    enabled = state.canSaveName,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NgSectionLabel(stringResource(Res.string.settings_device_id))
                    Text(state.myId, style = NgTheme.type.code, color = c.text)
                }
                NgButton(
                    stringResource(Res.string.settings_copy_id),
                    { clipboard.setText(AnnotatedString(state.myId)) },
                    style = NgButtonStyle.Tertiary,
                    icon = NgIcons.Copy,
                    minHeight = 44.dp,
                )
            }
        }

        NgCallout(
            title = stringResource(Res.string.settings_history_title),
            body = stringResource(Res.string.settings_history_body),
        )
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SyncthingTheme {
        SettingsScreen(
            state = SettingsState(
                coreState = CoreState.Running,
                myId = "MFZWI3D-BONSGYC-YLTMRWG-C43ENR5-QXGZDMM-FZWI3DP-BONSGYY-LTMRWAD",
                savedName = "Pixel do Adam",
                version = "v2.1.6",
            ),
            tabBar = {},
            onAction = {},
        )
    }
}
