package com.adamfoerster.syncthing.feature.devices.presentation.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgAvatar
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgChip
import com.adamfoerster.syncthing.core.designsystem.components.NgChips
import com.adamfoerster.syncthing.core.designsystem.components.NgIconButton
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgStatusLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTextField
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgShapes
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.CoreStatusCallout
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.label
import com.adamfoerster.syncthing.core.presentation.resources.Res as CoreRes
import com.adamfoerster.syncthing.core.presentation.resources.this_phone
import com.adamfoerster.syncthing.core.presentation.toSyncStatus
import com.adamfoerster.syncthing.feature.devices.presentation.resources.Res
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_add
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_approve
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_empty
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_menu_close
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_menu_pause
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_menu_remove
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_menu_rename
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_menu_resume
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_no_folders
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_open
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_pending_body
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_pending_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_reject
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_rename_hint
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_rename_label
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_rename_save
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_rename_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_remove_confirm
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_remove_confirm_body
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_remove_confirm_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_show_qr
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_this_phone_sub
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_title
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DeviceListRoot(
    tabBar: @Composable () -> Unit,
    onAddDevice: () -> Unit,
    onApprovePending: (id: String, name: String) -> Unit,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DeviceListScreen(
        state = state,
        tabBar = tabBar,
        onAction = { action ->
            when (action) {
                DeviceListAction.OnAddDevice -> onAddDevice()
                is DeviceListAction.OnApprovePending -> onApprovePending(action.device.id, action.device.name)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
fun DeviceListScreen(
    state: DeviceListState,
    tabBar: @Composable () -> Unit,
    onAction: (DeviceListAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(stringResource(Res.string.devices_title)) {
                if (state.isLoaded) {
                    NgIconButton(NgIcons.Plus, stringResource(Res.string.devices_add), { onAction(DeviceListAction.OnAddDevice) }, bordered = true)
                }
            }
        },
        bottomBar = tabBar,
    ) {
        if (state.coreState != CoreState.Running || !state.isLoaded) {
            CoreStatusCallout(
                if (state.coreState == CoreState.Running) CoreState.Starting else state.coreState,
                onStart = { onAction(DeviceListAction.OnStartCore) },
            )
            return@NgScreen
        }

        state.error?.let { NgCallout(body = it.asString(), tone = PanelTone.Warn) }

        state.pending.forEach { pending ->
            NgCallout(
                title = stringResource(Res.string.devices_pending_title, pending.name),
                body = stringResource(Res.string.devices_pending_body, pending.shortId, pending.address),
                tone = PanelTone.Warn,
                icon = NgIcons.Devices,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NgButton(stringResource(Res.string.devices_reject), { onAction(DeviceListAction.OnRejectPending(pending)) }, Modifier.weight(1f))
                    NgButton(
                        stringResource(Res.string.devices_approve),
                        { onAction(DeviceListAction.OnApprovePending(pending)) },
                        Modifier.weight(1f),
                        style = NgButtonStyle.Primary,
                    )
                }
            }
        }

        NgPanel(Modifier.fillMaxWidth(), tone = PanelTone.Selected) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NgAvatar(icon = NgIcons.Phone, isYou = true)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(CoreRes.string.this_phone), style = NgTheme.type.h3, color = c.text)
                    Text(stringResource(Res.string.devices_this_phone_sub), style = NgTheme.type.sub, color = c.text2)
                }
                NgButton(
                    stringResource(Res.string.devices_show_qr),
                    onClick = { onAction(DeviceListAction.OnAddDevice) },
                    icon = NgIcons.Qr,
                    minHeight = 44.dp,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                )
            }
        }

        if (state.devices.isEmpty()) {
            Text(stringResource(Res.string.devices_empty), style = NgTheme.type.sub, color = c.text2)
        }

        state.devices.forEach { device ->
            NgPanel(
                modifier = Modifier.fillMaxWidth(),
                tone = if (device.status == DeviceStatus.DISCONNECTED) PanelTone.Warn else PanelTone.Default,
                onClick = { onAction(DeviceListAction.OnDeviceClick(device)) },
                onClickLabel = stringResource(Res.string.devices_open, device.name),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NgAvatar(initial = device.initial)
                    Column(Modifier.weight(1f)) {
                        Text(device.name, style = NgTheme.type.h3, color = c.text)
                        Text(device.statusLine.asString(), style = NgTheme.type.sub, color = c.text2)
                    }
                    NgStatusLabel(device.status.toSyncStatus(), device.status.label().asString())
                }
                if (device.folders.isEmpty()) {
                    Text(stringResource(Res.string.devices_no_folders), style = NgTheme.type.sub, color = c.text2)
                } else {
                    NgChips { device.folders.forEach { NgChip(it) } }
                }
            }
        }

        NgButton(
            stringResource(Res.string.devices_add),
            onClick = { onAction(DeviceListAction.OnAddDevice) },
            style = NgButtonStyle.Primary,
            icon = NgIcons.Plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    state.menuFor?.let { device -> DeviceMenu(device, onAction) }
    state.rename?.let { rename -> RenameDialog(rename, onAction) }
    state.confirmRemove?.let { device ->
        AlertDialog(
            onDismissRequest = { onAction(DeviceListAction.OnDismissMenu) },
            shape = NgShapes.panel,
            containerColor = c.panel,
            title = { Text(stringResource(Res.string.devices_remove_confirm_title, device.name), style = NgTheme.type.h2) },
            text = { Text(stringResource(Res.string.devices_remove_confirm_body), style = NgTheme.type.body) },
            confirmButton = {
                NgButton(stringResource(Res.string.devices_remove_confirm), { onAction(DeviceListAction.OnRemoveConfirm) }, style = NgButtonStyle.Danger)
            },
            dismissButton = {
                NgButton(stringResource(Res.string.devices_menu_close), { onAction(DeviceListAction.OnDismissMenu) }, style = NgButtonStyle.Tertiary)
            },
        )
    }
}

@Composable
private fun DeviceMenu(device: DeviceUi, onAction: (DeviceListAction) -> Unit) {
    val c = NgTheme.colors
    AlertDialog(
        onDismissRequest = { onAction(DeviceListAction.OnDismissMenu) },
        shape = NgShapes.panel,
        containerColor = c.panel,
        title = { Text(device.name, style = NgTheme.type.h2) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val paused = device.status == DeviceStatus.PAUSED
                NgButton(
                    stringResource(if (paused) Res.string.devices_menu_resume else Res.string.devices_menu_pause),
                    { onAction(DeviceListAction.OnTogglePause(device)) },
                    Modifier.fillMaxWidth(),
                    icon = if (paused) NgIcons.Play else NgIcons.Pause,
                )
                NgButton(
                    stringResource(Res.string.devices_menu_rename),
                    { onAction(DeviceListAction.OnRenameRequest(device)) },
                    Modifier.fillMaxWidth(),
                    icon = NgIcons.Edit,
                )
                NgButton(
                    stringResource(Res.string.devices_menu_remove),
                    { onAction(DeviceListAction.OnRemoveRequest(device)) },
                    Modifier.fillMaxWidth(),
                    style = NgButtonStyle.Danger,
                    icon = NgIcons.Close,
                )
            }
        },
        confirmButton = {
            NgButton(stringResource(Res.string.devices_menu_close), { onAction(DeviceListAction.OnDismissMenu) }, style = NgButtonStyle.Tertiary)
        },
    )
}

@Composable
private fun RenameDialog(rename: RenameUi, onAction: (DeviceListAction) -> Unit) {
    val c = NgTheme.colors
    AlertDialog(
        onDismissRequest = { onAction(DeviceListAction.OnDismissMenu) },
        shape = NgShapes.panel,
        containerColor = c.panel,
        title = { Text(stringResource(Res.string.devices_rename_title), style = NgTheme.type.h2) },
        text = {
            NgTextField(
                value = rename.input,
                onValueChange = { onAction(DeviceListAction.OnRenameInputChange(it)) },
                label = stringResource(Res.string.devices_rename_label),
                supportingText = stringResource(Res.string.devices_rename_hint),
            )
        },
        confirmButton = {
            NgButton(
                stringResource(Res.string.devices_rename_save),
                { onAction(DeviceListAction.OnRenameConfirm) },
                style = NgButtonStyle.Primary,
                enabled = rename.canSave,
            )
        },
        dismissButton = {
            NgButton(stringResource(Res.string.devices_menu_close), { onAction(DeviceListAction.OnDismissMenu) }, style = NgButtonStyle.Tertiary)
        },
    )
}

@Preview
@Composable
private fun DeviceListScreenPreview() {
    SyncthingTheme {
        DeviceListScreen(
            state = DeviceListState(
                coreState = CoreState.Running,
                isLoaded = true,
                devices = listOf(
                    DeviceUi("n", "Notebook da Ana", "N", DeviceStatus.CONNECTED, UiText.DynamicString("Conectado agora"), listOf("Fotos da câmera", "Documentos")),
                    DeviceUi("t", "Tablet da sala", "T", DeviceStatus.DISCONNECTED, UiText.DynamicString("Sem conexão desde sábado"), listOf("Músicas")),
                ),
            ),
            tabBar = {},
            onAction = {},
        )
    }
}
