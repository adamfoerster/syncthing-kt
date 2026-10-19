package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgColors
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

enum class Tone { Neutral, Ok, Warn, Error, Info, Accent }

fun Tone.foreground(c: NgColors): Color = when (this) {
    Tone.Neutral -> c.text2
    Tone.Ok -> c.ok
    Tone.Warn -> c.warn
    Tone.Error -> c.err
    Tone.Info -> c.info
    Tone.Accent -> c.cyan
}

fun Tone.background(c: NgColors): Color = when (this) {
    Tone.Neutral -> Color.Transparent
    Tone.Ok -> c.okSub
    Tone.Warn -> c.warnSub
    Tone.Error -> c.errSub
    Tone.Info -> c.infoSub
    Tone.Accent -> c.cyanSub
}

/** `.badge`: outlined uppercase mono tag. */
@Composable
fun NgBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Neutral,
    icon: ImageVector? = null,
) {
    val c = NgTheme.colors
    val fg = tone.foreground(c)
    Row(
        modifier = modifier
            .height(22.dp)
            .background(tone.background(c))
            .border(1.dp, fg)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text.uppercase(), style = NgTheme.type.tag, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

enum class SyncStatus { Connected, Syncing, Offline, Paused, Error }

/** `.st`: status word with the shape glyph, so status never depends on color alone. */
@Composable
fun NgStatusLabel(
    status: SyncStatus,
    text: String,
    modifier: Modifier = Modifier,
) {
    val c = NgTheme.colors
    val (icon, color) = status.glyph(c)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Text(text.uppercase(), style = NgTheme.type.tag, color = color, maxLines = 1)
    }
}

fun SyncStatus.glyph(c: NgColors): Pair<ImageVector, Color> = when (this) {
    SyncStatus.Connected -> NgIcons.StatusSquare to c.ok
    SyncStatus.Syncing -> NgIcons.StatusDiamond to c.cyan
    SyncStatus.Offline -> NgIcons.StatusTriangle to c.warn
    SyncStatus.Paused -> NgIcons.Pause to c.text2
    SyncStatus.Error -> NgIcons.Warning to c.err
}
