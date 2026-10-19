package com.adamfoerster.syncthing.feature.sharing.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgChip
import com.adamfoerster.syncthing.core.designsystem.components.NgChips
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgStatusLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.CoreStatusCallout
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.label
import com.adamfoerster.syncthing.core.presentation.toSyncStatus
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.Res
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_device_count
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_devices
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_empty
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_folder_count
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_folders
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_intro
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_map_description
import com.adamfoerster.syncthing.feature.sharing.presentation.resources.sharing_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SharingRoot(
    onBack: () -> Unit,
    onStartCore: () -> Unit,
    viewModel: SharingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SharingScreen(
        state = state,
        onStartCore = onStartCore,
        onAction = { action ->
            if (action == SharingAction.OnBackClick) onBack() else viewModel.onAction(action)
        },
    )
}

@Composable
fun SharingScreen(
    state: SharingState,
    onStartCore: () -> Unit,
    onAction: (SharingAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = { NgTopBar(stringResource(Res.string.sharing_title), onBack = { onAction(SharingAction.OnBackClick) }) },
    ) {
        if (state.coreState != CoreState.Running) {
            CoreStatusCallout(state.coreState, onStart = onStartCore)
            return@NgScreen
        }
        if (state.devices.isEmpty() && state.folders.isEmpty()) {
            Text(stringResource(Res.string.sharing_empty), style = NgTheme.type.sub, color = c.text2)
            return@NgScreen
        }
        Text(stringResource(Res.string.sharing_intro), style = NgTheme.type.sub, color = c.text2)

        val mapDescription = stringResource(Res.string.sharing_map_description)
        NgPanel(Modifier.fillMaxWidth().semantics { contentDescription = mapDescription }) {
            Row {
                NgSectionLabel(stringResource(Res.string.sharing_devices), Modifier.weight(1f))
                NgSectionLabel(stringResource(Res.string.sharing_folders))
            }
            ShareGraph(state, onSelect = { onAction(SharingAction.OnSelectDevice(it)) })
        }

        val selected = state.selected
        if (selected != null) {
            NgPanel(Modifier.fillMaxWidth(), tone = PanelTone.Selected) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(selected.name, style = NgTheme.type.h3, color = c.text, modifier = Modifier.weight(1f))
                    val count = state.selectedFolders.size
                    Text(pluralStringResource(Res.plurals.sharing_folder_count, count, count), style = NgTheme.type.monoSmall, color = c.text2)
                }
                if (state.selectedFolders.isNotEmpty()) {
                    NgChips {
                        state.selectedFolders.forEach { folder ->
                            NgChip(folder.label, leading = { Icon(NgIcons.Folder, null, tint = c.text, modifier = Modifier.size(14.dp)) })
                        }
                    }
                }
                state.selectedNote?.let { Text(it.asString(), style = NgTheme.type.sub, color = c.text2) }
            }
        }
    }
}

private val DeviceNode = 72.dp
private val FolderNode = 68.dp
private val NodeWidth = 140.dp

@Composable
private fun ShareGraph(state: SharingState, onSelect: (String) -> Unit) {
    val c = NgTheme.colors
    val rows = maxOf(state.devices.size, state.folders.size, 1)
    val graphHeight = (rows * 90).dp.coerceAtLeast(160.dp)
    val selectedLinks = state.links[state.selectedId].orEmpty()

    BoxWithConstraints(Modifier.fillMaxWidth().height(graphHeight)) {
        val width = maxWidth
        val deviceTops = tops(state.devices.size, graphHeight, DeviceNode)
        val folderTops = tops(state.folders.size, graphHeight, FolderNode)
        val folderIndex = state.folders.withIndex().associate { (i, f) -> f.id to i }

        Canvas(Modifier.fillMaxSize()) {
            // Draw unselected links first so the highlighted ones sit on top.
            val ordered = state.devices.withIndex().sortedBy { it.value.id == state.selectedId }
            for ((di, device) in ordered) {
                val on = device.id == state.selectedId
                for (folderId in state.links[device.id].orEmpty()) {
                    val fi = folderIndex[folderId] ?: continue
                    val start = Offset(NodeWidth.toPx(), (deviceTops[di] + DeviceNode / 2).toPx())
                    val end = Offset((width - NodeWidth).toPx(), (folderTops[fi] + FolderNode / 2).toPx())
                    val offline = device.status == DeviceStatus.DISCONNECTED || device.status == DeviceStatus.PAUSED
                    drawLine(
                        color = if (on) c.cyan else c.lineStrong.copy(alpha = 0.4f),
                        start = start,
                        end = end,
                        strokeWidth = (if (on) 3 else 2).dp.toPx(),
                        pathEffect = if (on && offline) PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())) else null,
                    )
                }
            }
        }

        state.folders.forEachIndexed { i, folder ->
            val on = folder.id in selectedLinks
            Column(
                Modifier
                    .offset(width - NodeWidth, folderTops[i])
                    .size(NodeWidth, FolderNode)
                    .background(if (on) c.cyanSub else c.raised)
                    .border(1.dp, if (on) c.cyan else c.lineStrong)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(folder.label, style = NgTheme.type.node, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(Res.plurals.sharing_device_count, folder.deviceCount, folder.deviceCount),
                    style = NgTheme.type.monoSmall.copy(fontSize = NgTheme.type.tag.fontSize),
                    color = c.text2,
                )
            }
        }

        state.devices.forEachIndexed { i, device ->
            val on = device.id == state.selectedId
            Column(
                Modifier
                    .offset(0.dp, deviceTops[i])
                    .size(NodeWidth, DeviceNode)
                    .background(if (on) c.cyanSub else c.raised)
                    .border(1.dp, if (on) c.cyan else c.lineStrong)
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(device.id) })
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(device.name, style = NgTheme.type.node, color = c.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                NgStatusLabel(device.status.toSyncStatus(), device.status.label().asString())
            }
        }
    }
}

/** Spreads [count] nodes of [node] height evenly over [total], each centered in its slot. */
private fun tops(count: Int, total: Dp, node: Dp): List<Dp> {
    if (count == 0) return emptyList()
    val slot = total / count
    return List(count) { i -> slot * i + (slot - node) / 2 }
}

@Preview
@Composable
private fun SharingScreenPreview() {
    SyncthingTheme {
        SharingScreen(
            state = SharingState(
                coreState = CoreState.Running,
                devices = listOf(
                    ShareDeviceUi("n", "Notebook da Ana", DeviceStatus.CONNECTED),
                    ShareDeviceUi("p", "PC escritório", DeviceStatus.SYNCING),
                    ShareDeviceUi("t", "Tablet da sala", DeviceStatus.DISCONNECTED),
                ),
                folders = listOf(
                    ShareFolderUi("f", "Fotos da câmera", 2),
                    ShareFolderUi("d", "Documentos", 3),
                    ShareFolderUi("m", "Músicas", 1),
                ),
                links = mapOf("n" to setOf("f", "d"), "p" to setOf("f", "d"), "t" to setOf("d", "m")),
                selectedId = "n",
                selectedNote = UiText.DynamicString("Recebe e envia mudanças agora mesmo."),
            ),
            onStartCore = {},
            onAction = {},
        )
    }
}
