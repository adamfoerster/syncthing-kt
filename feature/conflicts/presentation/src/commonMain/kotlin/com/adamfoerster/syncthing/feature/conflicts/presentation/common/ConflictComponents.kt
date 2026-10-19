package com.adamfoerster.syncthing.feature.conflicts.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgAvatar
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.Res
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_back_to_list
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_done_title
import org.jetbrains.compose.resources.stringResource

/** Small avatar for a version: the phone glyph for this device, the initial otherwise. */
@Composable
fun VersionAvatar(version: VersionUi, size: androidx.compose.ui.unit.Dp = 20.dp) {
    if (version.isLocal) {
        NgAvatar(icon = NgIcons.Phone, size = size, isYou = true)
    } else {
        NgAvatar(initial = version.initial, size = size)
    }
}

/** Success state shared by both resolvers. */
@Composable
fun ResolvedPanel(message: String, modifier: Modifier = Modifier) {
    val c = NgTheme.colors
    NgPanel(modifier = modifier.fillMaxWidth(), tone = PanelTone.Ok) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(NgIcons.Check, null, tint = c.ok, modifier = Modifier.size(40.dp))
            Text(stringResource(Res.string.resolver_done_title), style = NgTheme.type.h2, color = c.text)
            Text(message, style = NgTheme.type.sub, color = c.text2, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BackToListBar(onClick: () -> Unit) {
    NgActionBar {
        NgButton(
            text = stringResource(Res.string.resolver_back_to_list),
            onClick = onClick,
            style = NgButtonStyle.Primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Helper line above the primary action, as in the mockups' action bars. */
@Composable
fun HelperText(text: String) {
    Text(
        text,
        style = NgTheme.type.sub,
        color = NgTheme.colors.text2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
