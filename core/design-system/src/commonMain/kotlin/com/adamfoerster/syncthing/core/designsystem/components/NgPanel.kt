package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.theme.NgColors
import com.adamfoerster.syncthing.core.designsystem.theme.NgShapes
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

enum class PanelTone { Default, Selected, Warn, Ok, Info }

/** `.panel`: chamfered card with a 1px border whose color carries the tone. */
@Composable
fun NgPanel(
    modifier: Modifier = Modifier,
    tone: PanelTone = PanelTone.Default,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = NgTheme.colors
    val (border, fill) = tone.colorsIn(colors)
    Column(
        modifier = modifier
            .clip(NgShapes.panel)
            .background(fill, NgShapes.panel)
            .border(1.dp, border, NgShapes.panel)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

private fun PanelTone.colorsIn(c: NgColors): Pair<Color, Color> = when (this) {
    PanelTone.Default -> c.line to c.panel
    PanelTone.Selected -> c.cyanEdge to c.cyanSub
    PanelTone.Warn -> c.warn to c.panel
    PanelTone.Ok -> c.ok to c.panel
    PanelTone.Info -> c.info to c.panel
}
