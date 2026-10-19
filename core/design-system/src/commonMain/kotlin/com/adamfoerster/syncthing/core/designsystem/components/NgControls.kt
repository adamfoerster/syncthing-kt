package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

/** `.bar`: 6dp outlined progress bar. [fraction] in 0..1. */
@Composable
fun NgProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val c = NgTheme.colors
    val value = fraction.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(c.bg)
            .border(1.dp, c.lineStrong)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
                if (label != null) contentDescription = label
            },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(value).background(c.cyan))
    }
}

/**
 * `.seg`: toggle segment; selected shows the check and the [tone] colors. A [tone] other than
 * [Tone.Accent] also tints the unselected label, tying the segment to content shown in that color.
 */
@Composable
fun RowScope.NgSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Accent,
) {
    val c = NgTheme.colors
    val accent = tone.foreground(c)
    Row(
        modifier = modifier
            .weight(1f)
            .defaultMinSize(minHeight = 48.dp)
            .background(if (selected) tone.background(c) else c.panel)
            .border(1.dp, if (selected) accent else c.lineStrong)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (selected || tone != Tone.Accent) accent else c.text
        if (selected) Icon(NgIcons.Check, null, tint = fg, modifier = Modifier.size(14.dp))
        Text(text, style = NgTheme.type.segment, color = fg, textAlign = TextAlign.Center)
    }
}

@Composable
fun NgSegmentedRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), content = content)
}
