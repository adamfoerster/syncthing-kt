package com.adamfoerster.meshsync.feature.folders.presentation.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.meshsync.core.designsystem.components.NgActionBar
import com.adamfoerster.meshsync.core.designsystem.components.NgAvatar
import com.adamfoerster.meshsync.core.designsystem.components.NgButton
import com.adamfoerster.meshsync.core.designsystem.components.NgButtonStyle
import com.adamfoerster.meshsync.core.designsystem.components.NgPanel
import com.adamfoerster.meshsync.core.designsystem.components.NgScreen
import com.adamfoerster.meshsync.core.designsystem.components.NgSectionLabel
import com.adamfoerster.meshsync.core.designsystem.components.NgTextField
import com.adamfoerster.meshsync.core.designsystem.components.NgTopBar
import com.adamfoerster.meshsync.core.designsystem.components.PanelTone
import com.adamfoerster.meshsync.core.designsystem.icons.NgIcons
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme
import com.adamfoerster.meshsync.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.presentation.CoreStatusCallout
import com.adamfoerster.meshsync.core.presentation.LocalPlatformActions
import com.adamfoerster.meshsync.core.presentation.ObserveAsEvents
import com.adamfoerster.meshsync.core.presentation.initialOf
import com.adamfoerster.meshsync.feature.folders.presentation.resources.Res
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_accept_title
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_choose
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_devices
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_invalid
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_label
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_label_placeholder
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_no_devices
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_path
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_path_hint
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_save
import com.adamfoerster.meshsync.feature.folders.presentation.resources.add_folder_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddFolderRoot(
    offer: FolderOffer?,
    initialPath: String?,
    onDone: () -> Unit,
) {
    val defaultRoot = LocalPlatformActions.current.defaultFolderRoot
    val viewModel: AddFolderViewModel = koinViewModel(key = offer?.folderId ?: initialPath) { parametersOf(offer, initialPath, defaultRoot) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AddFolderEvent.Saved -> onDone()
        }
    }
    val platform = LocalPlatformActions.current
    AddFolderScreen(
        state = state,
        onAction = { action ->
            when (action) {
                AddFolderAction.OnBackClick -> onDone()
                AddFolderAction.OnChooseFolder -> platform.pickFolder { path -> if (path != null) viewModel.onAction(AddFolderAction.OnFolderPicked(path)) }
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
fun AddFolderScreen(
    state: AddFolderState,
    onAction: (AddFolderAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                stringResource(if (state.isOffer) Res.string.add_folder_accept_title else Res.string.add_folder_title),
                onBack = { onAction(AddFolderAction.OnBackClick) },
            )
        },
        bottomBar = {
            NgActionBar {
                val error = state.error
                when {
                    error != null -> Text(error.asString(), style = NgTheme.type.sub, color = c.err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    !state.isValid -> Text(stringResource(Res.string.add_folder_invalid), style = NgTheme.type.sub, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                NgButton(
                    stringResource(Res.string.add_folder_save),
                    onClick = { onAction(AddFolderAction.OnSave) },
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Check,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        CoreStatusCallout(state.coreState, onStart = { onAction(AddFolderAction.OnStartCore) })
        NgTextField(
            value = state.label,
            onValueChange = { onAction(AddFolderAction.OnLabelChange(it)) },
            label = stringResource(Res.string.add_folder_label),
            placeholder = stringResource(Res.string.add_folder_label_placeholder),
        )
        NgTextField(
            value = state.path,
            onValueChange = { onAction(AddFolderAction.OnPathChange(it)) },
            label = stringResource(Res.string.add_folder_path),
            supportingText = stringResource(Res.string.add_folder_path_hint),
            textStyle = NgTheme.type.mono14,
        )
        NgButton(
            stringResource(Res.string.add_folder_choose),
            onClick = { onAction(AddFolderAction.OnChooseFolder) },
            icon = NgIcons.Folder,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NgSectionLabel(stringResource(Res.string.add_folder_devices))
            // Before the core answers there is no device list yet, which is not the same as having no devices.
            if (state.devices.isEmpty() && state.coreState == CoreState.Running) {
                Text(stringResource(Res.string.add_folder_no_devices), style = NgTheme.type.sub, color = c.text2)
            }
            state.devices.forEach { device ->
                NgPanel(
                    tone = if (device.checked) PanelTone.Selected else PanelTone.Default,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(device.checked, role = Role.Checkbox, onValueChange = { onAction(AddFolderAction.OnToggleDevice(device.id)) }),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        NgAvatar(initial = initialOf(device.name))
                        Text(device.name, style = NgTheme.type.body, color = c.text, modifier = Modifier.weight(1f))
                        Checkbox(
                            checked = device.checked,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = c.cyan, checkmarkColor = c.bg, uncheckedColor = c.lineStrong),
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AddFolderCoreStartingPreview() {
    SyncthingTheme {
        AddFolderScreen(state = AddFolderState(label = "Sync", path = "/storage/emulated/0/Sync", coreState = CoreState.Starting), onAction = {})
    }
}

@Preview
@Composable
private fun AddFolderScreenPreview() {
    SyncthingTheme {
        AddFolderScreen(
            state = AddFolderState(
                label = "Documentos",
                path = "/storage/emulated/0/Documentos",
                devices = listOf(DeviceChoiceUi("n", "Notebook da Ana", true), DeviceChoiceUi("p", "PC escritório", false)),
            ),
            onAction = {},
        )
    }
}
