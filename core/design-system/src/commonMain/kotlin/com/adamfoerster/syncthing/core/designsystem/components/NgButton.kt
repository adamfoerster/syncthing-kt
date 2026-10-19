package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.theme.NgShapes
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

enum class NgButtonStyle { Primary, Secondary, Danger, Tertiary }

/** `.btn` and its `.pri`, `.dng`, `.ter` variants. */
@Composable
fun NgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: NgButtonStyle = NgButtonStyle.Secondary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 48.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    val c = NgTheme.colors
    val (border, fill, content) = when {
        !enabled -> Triple(c.line, c.disabledBg, c.disabledText)
        style == NgButtonStyle.Primary -> Triple(c.cyan, c.cyan, c.bg)
        style == NgButtonStyle.Danger -> Triple(c.err, c.bg, c.err)
        style == NgButtonStyle.Tertiary -> Triple(Color.Transparent, Color.Transparent, c.cyan)
        else -> Triple(c.lineStrong, c.bg, c.text)
    }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .clip(NgShapes.button)
            .background(fill, NgShapes.button)
            .border(1.dp, border, NgShapes.button)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(
            text = text.uppercase(),
            style = NgTheme.type.button.copy(fontSize = fontSize.takeIf { it != TextUnit.Unspecified } ?: NgTheme.type.button.fontSize),
            color = content,
            textAlign = TextAlign.Center,
        )
    }
}
