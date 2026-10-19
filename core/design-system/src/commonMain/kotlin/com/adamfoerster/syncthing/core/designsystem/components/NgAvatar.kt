package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adamfoerster.syncthing.core.designsystem.theme.NgShapes
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

/** `.av`: chamfered initial (or icon) for a device. [isYou] marks this phone. */
@Composable
fun NgAvatar(
    modifier: Modifier = Modifier,
    initial: String? = null,
    icon: ImageVector? = null,
    size: Dp = 36.dp,
    isYou: Boolean = false,
) {
    val c = NgTheme.colors
    Box(
        modifier = modifier
            .size(size)
            .clip(NgShapes.avatar)
            .background(if (isYou) c.cyanSub else c.raised),
        contentAlignment = Alignment.Center,
    ) {
        val fg = if (isYou) c.cyan else c.text
        when {
            icon != null -> Icon(icon, null, tint = fg, modifier = Modifier.size(size * 0.56f))
            initial != null -> Text(
                initial.take(1).uppercase(),
                style = NgTheme.type.button.copy(fontSize = (size.value * 0.39f).sp, letterSpacing = 0.sp),
                color = fg,
            )
        }
    }
}

/** `.stack`: overlapping small avatars. */
@Composable
fun NgAvatarStack(initials: List<String>, modifier: Modifier = Modifier) {
    val c = NgTheme.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        initials.forEach { initial ->
            NgAvatar(
                initial = initial,
                size = 28.dp,
                modifier = Modifier.border(2.dp, c.panel, NgShapes.avatar),
            )
        }
    }
}

/** `.tile`: 40dp chamfered icon square in accent color. */
@Composable
fun NgTile(icon: ImageVector, modifier: Modifier = Modifier) {
    val c = NgTheme.colors
    Box(
        modifier = modifier.size(40.dp).clip(NgShapes.avatar).background(c.raised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = c.cyan, modifier = Modifier.size(24.dp))
    }
}

/** `.chip` */
@Composable
fun NgChip(
    text: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = NgTheme.colors
    Row(
        modifier = modifier
            .height(24.dp)
            .background(c.raised)
            .border(1.dp, c.line)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, style = NgTheme.type.chip, color = c.text, maxLines = 1)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NgChips(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
