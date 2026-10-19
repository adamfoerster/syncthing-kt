package com.adamfoerster.syncthing.feature.devices.presentation.add

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgAvatar
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTextField
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.presentation.LocalPlatformActions
import com.adamfoerster.syncthing.core.presentation.ObserveAsEvents
import com.adamfoerster.syncthing.feature.devices.presentation.resources.Res
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_approve_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_confirm_device
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_copy
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_device_id
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_device_id_hint
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_device_id_invalid
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_device_name
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_device_name_placeholder
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_finish
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_folders_body
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_folders_none
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_folders_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_manual
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_qr_description
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_scan
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_show_body
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_show_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_skip
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_step_approve
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_step_folders
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_step_show
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_steps
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_text_code
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_title
import com.adamfoerster.syncthing.feature.devices.presentation.resources.add_wait_hint
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_approve
import com.adamfoerster.syncthing.feature.devices.presentation.resources.devices_pending_title
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddDeviceRoot(
    prefillId: String?,
    prefillName: String?,
    onDone: () -> Unit,
) {
    val viewModel: AddDeviceViewModel = koinViewModel(key = prefillId) { parametersOf(prefillId, prefillName) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val platform = LocalPlatformActions.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AddDeviceEvent.LaunchScanner -> platform.scanQrCode { viewModel.onAction(AddDeviceAction.OnScanResult(it)) }
            AddDeviceEvent.Done -> onDone()
        }
    }
    AddDeviceScreen(
        state = state,
        onAction = { action -> if (action == AddDeviceAction.OnBackClick) onDone() else viewModel.onAction(action) },
    )
}

@Composable
fun AddDeviceScreen(
    state: AddDeviceState,
    onAction: (AddDeviceAction) -> Unit,
) {
    NgScreen(
        topBar = { NgTopBar(stringResource(Res.string.add_title), onBack = { onAction(AddDeviceAction.OnBackClick) }) },
        bottomBar = { StepActions(state, onAction) },
    ) {
        Stepper(state.step)
        when (state.step) {
            AddStep.SHOW_CODE -> ShowCodeStep(state, onAction)
            AddStep.APPROVE -> ApproveStep(state, onAction)
            AddStep.FOLDERS -> FoldersStep(state, onAction)
        }
    }
}

@Composable
private fun Stepper(step: AddStep) {
    val c = NgTheme.colors
    val labels = listOf(Res.string.add_step_show, Res.string.add_step_approve, Res.string.add_step_folders)
    val stepsLabel = stringResource(Res.string.add_steps)
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = stepsLabel },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val current = index == step.ordinal
            val done = index < step.ordinal
            Row(
                Modifier.weight(if (index < labels.lastIndex) 1f else 0.8f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    NgAvatar(icon = NgIcons.Check, size = 28.dp, isYou = true)
                } else {
                    NgAvatar(initial = "${index + 1}", size = 28.dp, isYou = current)
                }
                Text(
                    stringResource(label),
                    style = if (current) NgTheme.type.bodyStrong.copy(fontSize = NgTheme.type.sub.fontSize) else NgTheme.type.sub,
                    color = if (current) c.cyan else c.text2,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ShowCodeStep(state: AddDeviceState, onAction: (AddDeviceAction) -> Unit) {
    val c = NgTheme.colors
    val clipboard = LocalClipboardManager.current
    state.incoming?.let { incoming ->
        NgCallout(
            title = stringResource(Res.string.devices_pending_title, incoming.name),
            body = incoming.id,
            tone = PanelTone.Warn,
            icon = NgIcons.Devices,
        ) {
            NgButton(
                stringResource(Res.string.devices_approve),
                { onAction(AddDeviceAction.OnUseIncoming) },
                Modifier.fillMaxWidth(),
                style = NgButtonStyle.Primary,
            )
        }
    }
    NgPanel(Modifier.fillMaxWidth(), spacing = 16.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(Res.string.add_show_title), style = NgTheme.type.h2, color = c.text, textAlign = TextAlign.Center)
            Text(stringResource(Res.string.add_show_body), style = NgTheme.type.sub, color = c.text2, textAlign = TextAlign.Center)
            Box(
                Modifier.size(220.dp).border(1.dp, c.lineStrong).background(Color.White).padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (state.myId.isNotEmpty()) {
                    Image(
                        painter = rememberQrCodePainter(state.myId),
                        contentDescription = stringResource(Res.string.add_qr_description),
                        modifier = Modifier.size(192.dp),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                NgSectionLabel(stringResource(Res.string.add_text_code))
                Text(state.myId, style = NgTheme.type.code, color = c.text, textAlign = TextAlign.Center)
            }
            NgButton(
                stringResource(Res.string.add_copy),
                onClick = { clipboard.setText(AnnotatedString(state.myId)) },
                style = NgButtonStyle.Tertiary,
                icon = NgIcons.Copy,
                minHeight = 44.dp,
            )
        }
    }
    NgCallout(body = stringResource(Res.string.add_wait_hint))
}

@Composable
private fun ApproveStep(state: AddDeviceState, onAction: (AddDeviceAction) -> Unit) {
    Text(stringResource(Res.string.add_approve_title), style = NgTheme.type.h2, color = NgTheme.colors.text)
    NgTextField(
        value = state.deviceIdInput,
        onValueChange = { onAction(AddDeviceAction.OnIdChange(it)) },
        label = stringResource(Res.string.add_device_id),
        supportingText = stringResource(if (state.showIdError) Res.string.add_device_id_invalid else Res.string.add_device_id_hint),
        isError = state.showIdError,
        singleLine = false,
        textStyle = NgTheme.type.code,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
    )
    NgTextField(
        value = state.nameInput,
        onValueChange = { onAction(AddDeviceAction.OnNameChange(it)) },
        label = stringResource(Res.string.add_device_name),
        placeholder = stringResource(Res.string.add_device_name_placeholder),
    )
}

@Composable
private fun FoldersStep(state: AddDeviceState, onAction: (AddDeviceAction) -> Unit) {
    val c = NgTheme.colors
    Text(
        stringResource(Res.string.add_folders_title, state.nameInput.ifBlank { state.parsedId?.short.orEmpty() }),
        style = NgTheme.type.h2,
        color = c.text,
    )
    Text(stringResource(Res.string.add_folders_body), style = NgTheme.type.sub, color = c.text2)
    if (state.folders.isEmpty()) {
        Text(stringResource(Res.string.add_folders_none), style = NgTheme.type.sub, color = c.text2)
    }
    state.folders.forEach { folder ->
        NgPanel(
            tone = if (folder.checked) PanelTone.Selected else PanelTone.Default,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(folder.checked, role = Role.Checkbox, onValueChange = { onAction(AddDeviceAction.OnToggleFolder(folder.id)) }),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NgAvatar(icon = NgIcons.Folder)
                Text(folder.label, style = NgTheme.type.body, color = c.text, modifier = Modifier.weight(1f))
                Checkbox(
                    checked = folder.checked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = c.cyan, checkmarkColor = c.bg, uncheckedColor = c.lineStrong),
                )
            }
        }
    }
}

@Composable
private fun StepActions(state: AddDeviceState, onAction: (AddDeviceAction) -> Unit) {
    val c = NgTheme.colors
    NgActionBar {
        state.error?.let {
            Text(it.asString(), style = NgTheme.type.sub, color = c.err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
        when (state.step) {
            AddStep.SHOW_CODE -> {
                NgButton(stringResource(Res.string.add_scan), { onAction(AddDeviceAction.OnScanClick) }, Modifier.fillMaxWidth(), style = NgButtonStyle.Primary, icon = NgIcons.Qr)
                NgButton(stringResource(Res.string.add_manual), { onAction(AddDeviceAction.OnManualClick) }, Modifier.fillMaxWidth(), style = NgButtonStyle.Tertiary)
            }
            AddStep.APPROVE -> {
                NgButton(
                    stringResource(Res.string.add_confirm_device),
                    { onAction(AddDeviceAction.OnConfirmDevice) },
                    Modifier.fillMaxWidth(),
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Plus,
                    enabled = state.deviceIdInput.isNotBlank() && !state.isSaving,
                )
                NgButton(stringResource(Res.string.add_scan), { onAction(AddDeviceAction.OnScanClick) }, Modifier.fillMaxWidth(), style = NgButtonStyle.Tertiary, icon = NgIcons.Qr)
            }
            AddStep.FOLDERS -> {
                NgButton(
                    stringResource(Res.string.add_finish),
                    { onAction(AddDeviceAction.OnFinish) },
                    Modifier.fillMaxWidth(),
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Check,
                    enabled = !state.isSaving,
                )
                NgButton(stringResource(Res.string.add_skip), { onAction(AddDeviceAction.OnSkipFolders) }, Modifier.fillMaxWidth(), style = NgButtonStyle.Tertiary)
            }
        }
    }
}

@Preview
@Composable
private fun AddDeviceScreenPreview() {
    SyncthingTheme {
        AddDeviceScreen(
            state = AddDeviceState(myId = "MFZWI3D-BONSGYC-YLTMRWG-C43ENR5-QXGZDMM-FZWI3DP-BONSGYY-LTMRWAD"),
            onAction = {},
        )
    }
}
