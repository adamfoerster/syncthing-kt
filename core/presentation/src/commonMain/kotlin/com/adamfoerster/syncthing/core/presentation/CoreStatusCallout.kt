package com.adamfoerster.syncthing.core.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.presentation.resources.Res
import com.adamfoerster.syncthing.core.presentation.resources.core_binary_missing_body
import com.adamfoerster.syncthing.core.presentation.resources.core_binary_missing_title
import com.adamfoerster.syncthing.core.presentation.resources.core_failed_title
import com.adamfoerster.syncthing.core.presentation.resources.core_grant_access
import com.adamfoerster.syncthing.core.presentation.resources.core_permission_body
import com.adamfoerster.syncthing.core.presentation.resources.core_permission_title
import com.adamfoerster.syncthing.core.presentation.resources.core_start
import com.adamfoerster.syncthing.core.presentation.resources.core_starting_body
import com.adamfoerster.syncthing.core.presentation.resources.core_starting_title
import com.adamfoerster.syncthing.core.presentation.resources.core_stopped_body
import com.adamfoerster.syncthing.core.presentation.resources.core_stopped_title
import org.jetbrains.compose.resources.stringResource

/** Explains why there is nothing to show while the core is not running, with the way out. */
@Composable
fun CoreStatusCallout(
    state: CoreState,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val platform = LocalPlatformActions.current
    when (state) {
        CoreState.Running -> Unit
        CoreState.Starting -> NgCallout(
            title = stringResource(Res.string.core_starting_title),
            body = stringResource(Res.string.core_starting_body),
            icon = NgIcons.StatusDiamond,
            modifier = modifier,
        )
        CoreState.Stopped -> NgCallout(
            title = stringResource(Res.string.core_stopped_title),
            body = stringResource(Res.string.core_stopped_body),
            tone = PanelTone.Warn,
            iconSize = 24.dp,
            modifier = modifier,
        ) {
            NgButton(stringResource(Res.string.core_start), onStart, style = NgButtonStyle.Primary, icon = NgIcons.Play, modifier = Modifier.fillMaxWidth())
        }
        CoreState.StoragePermissionMissing -> NgCallout(
            title = stringResource(Res.string.core_permission_title),
            body = stringResource(Res.string.core_permission_body),
            tone = PanelTone.Warn,
            iconSize = 24.dp,
            modifier = modifier,
        ) {
            NgButton(
                stringResource(Res.string.core_grant_access),
                platform::requestStorageAccess,
                style = NgButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CoreState.BinaryMissing -> NgCallout(
            title = stringResource(Res.string.core_binary_missing_title),
            body = stringResource(Res.string.core_binary_missing_body),
            tone = PanelTone.Warn,
            iconSize = 24.dp,
            modifier = modifier,
        )
        is CoreState.Failed -> NgCallout(
            title = stringResource(Res.string.core_failed_title),
            body = state.message,
            tone = PanelTone.Warn,
            iconSize = 24.dp,
            modifier = modifier,
        ) {
            NgButton(stringResource(Res.string.core_start), onStart, style = NgButtonStyle.Primary, icon = NgIcons.Restart, modifier = Modifier.fillMaxWidth())
        }
    }
}
