package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

/** Panel with a leading icon, title and body — the "O que é um conflito?" and hint boxes. */
@Composable
fun NgCallout(
    body: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    tone: PanelTone = PanelTone.Info,
    icon: ImageVector = if (tone == PanelTone.Warn) NgIcons.Warning else NgIcons.Info,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
    content: @Composable () -> Unit = {},
) {
    val c = NgTheme.colors
    val tint = when (tone) {
        PanelTone.Warn -> c.warn
        PanelTone.Ok -> c.ok
        PanelTone.Selected -> c.cyan
        else -> c.info
    }
    NgPanel(modifier = modifier.fillMaxWidth(), tone = tone) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(iconSize))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (title != null) {
                    Text(title, style = if (iconSize > 20.dp) NgTheme.type.h2 else NgTheme.type.h3, color = c.text)
                }
                // Large-icon status callouts read as heading + secondary text; hint boxes keep body text bright.
                Text(body, style = NgTheme.type.sub, color = if (iconSize > 20.dp) c.text2 else c.text)
            }
        }
        content()
    }
}
