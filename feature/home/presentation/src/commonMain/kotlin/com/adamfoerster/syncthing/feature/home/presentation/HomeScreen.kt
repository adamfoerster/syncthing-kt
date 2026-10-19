package com.adamfoerster.syncthing.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgBadge
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.presentation.CoreStatusCallout
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.home.presentation.resources.Res
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_add_device
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_badge_attention
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_badge_stopped
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_badge_syncing
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_badge_up_to_date
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_conflicts_body
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_conflicts_title
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_connected_count
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_last_sync
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_last_sync_never
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_no_devices_body
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_no_devices_title
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_ok_body
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_ok_title
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_pending_devices
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_pending_folders
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_resolve_now
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_review
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_show_sharing
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_syncing_body
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_syncing_title
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_title
import com.adamfoerster.syncthing.feature.home.presentation.resources.home_your_devices
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeRoot(
    tabBar: @Composable () -> Unit,
    onResolveConflicts: () -> Unit,
    onShowSharing: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenFolders: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        tabBar = tabBar,
        onAction = { action ->
            when (action) {
                HomeAction.OnResolveConflicts -> onResolveConflicts()
                HomeAction.OnShowSharing -> onShowSharing()
                HomeAction.OnOpenDevices -> onOpenDevices()
                HomeAction.OnOpenFolders -> onOpenFolders()
                else -> viewModel.onAction(action)
            }
        },
    )
}

@Composable
fun HomeScreen(
    state: HomeState,
    tabBar: @Composable () -> Unit,
    onAction: (HomeAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(title = stringResource(Res.string.home_title)) {
                when (state.headline) {
                    Headline.ATTENTION -> NgBadge(stringResource(Res.string.home_badge_attention), tone = Tone.Warn, icon = NgIcons.Warning)
                    Headline.SYNCING -> NgBadge(stringResource(Res.string.home_badge_syncing), tone = Tone.Accent, icon = NgIcons.StatusDiamond)
                    Headline.UP_TO_DATE -> NgBadge(stringResource(Res.string.home_badge_up_to_date), tone = Tone.Ok, icon = NgIcons.Check)
                    Headline.STOPPED -> NgBadge(stringResource(Res.string.home_badge_stopped), tone = Tone.Neutral)
                }
            }
        },
        bottomBar = tabBar,
    ) {
        if (state.coreState != CoreState.Running || !state.isLoaded) {
            CoreStatusCallout(
                state = if (state.coreState == CoreState.Running) CoreState.Starting else state.coreState,
                onStart = { onAction(HomeAction.OnStartCore) },
            )
            return@NgScreen
        }

        StatusPanel(state, onAction)

        if (state.pendingDevices > 0 || state.pendingFolders > 0) {
            NgCallout(
                body = listOfNotNull(
                    state.pendingDevices.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.home_pending_devices, it, it) },
                    state.pendingFolders.takeIf { it > 0 }?.let { pluralStringResource(Res.plurals.home_pending_folders, it, it) },
                ).joinToString("\n"),
                icon = NgIcons.Plus,
            ) {
                NgButton(
                    stringResource(Res.string.home_review),
                    onClick = { onAction(if (state.pendingDevices > 0) HomeAction.OnOpenDevices else HomeAction.OnOpenFolders) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.devices.isEmpty()) {
            NgCallout(
                title = stringResource(Res.string.home_no_devices_title),
                body = stringResource(Res.string.home_no_devices_body),
                icon = NgIcons.Devices,
                iconSize = 24.dp,
            ) {
                NgButton(
                    stringResource(Res.string.home_add_device),
                    onClick = { onAction(HomeAction.OnOpenDevices) },
                    style = NgButtonStyle.Primary,
                    icon = NgIcons.Plus,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            NgPanel(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NgSectionLabel(stringResource(Res.string.home_your_devices), Modifier.weight(1f))
                    Text(
                        pluralStringResource(Res.plurals.home_connected_count, state.connectedCount, state.connectedCount),
                        style = NgTheme.type.monoSmall,
                        color = c.text2,
                    )
                }
                DeviceMap(state.devices)
                MapLegend()
            }
            NgButton(
                stringResource(Res.string.home_show_sharing),
                onClick = { onAction(HomeAction.OnShowSharing) },
                icon = NgIcons.ShareMap,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StatusPanel(state: HomeState, onAction: (HomeAction) -> Unit) {
    val c = NgTheme.colors
    val lastSync = state.lastSync?.let { stringResource(Res.string.home_last_sync, it.asString()) }
        ?: stringResource(Res.string.home_last_sync_never)
    when (state.headline) {
        Headline.ATTENTION -> NgCallout(
            title = pluralStringResource(Res.plurals.home_conflicts_title, state.conflictFiles, state.conflictFiles),
            body = stringResource(Res.string.home_conflicts_body),
            tone = PanelTone.Warn,
            iconSize = 24.dp,
        ) {
            NgButton(
                stringResource(Res.string.home_resolve_now),
                onClick = { onAction(HomeAction.OnResolveConflicts) },
                style = NgButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(lastSync, style = NgTheme.type.monoSmall, color = c.text2)
        }
        Headline.SYNCING -> NgCallout(
            title = stringResource(Res.string.home_syncing_title, "${state.syncPercent ?: 0}%"),
            body = stringResource(Res.string.home_syncing_body),
            tone = PanelTone.Selected,
            icon = NgIcons.Transfer,
            iconSize = 24.dp,
        ) {
            Text(lastSync, style = NgTheme.type.monoSmall, color = c.text2)
        }
        else -> NgCallout(
            title = stringResource(Res.string.home_ok_title),
            body = stringResource(Res.string.home_ok_body),
            tone = PanelTone.Ok,
            icon = NgIcons.Check,
            iconSize = 24.dp,
        ) {
            Text(lastSync, style = NgTheme.type.monoSmall, color = c.text2)
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    SyncthingTheme {
        HomeScreen(
            state = HomeState(
                coreState = CoreState.Running,
                isLoaded = true,
                conflictFiles = 2,
                lastSync = UiText.DynamicString("há 2 min"),
                devices = listOf(
                    MapDeviceUi("n", "Notebook da Ana", DeviceStatus.CONNECTED, 100.0),
                    MapDeviceUi("p", "PC escritório", DeviceStatus.SYNCING, 64.0),
                    MapDeviceUi("t", "Tablet da sala", DeviceStatus.DISCONNECTED, null),
                ),
            ),
            tabBar = {},
            onAction = {},
        )
    }
}
