package com.adamfoerster.meshsync.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adamfoerster.meshsync.core.designsystem.icons.NgIcons
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme

/**
 * A folder glyph. When [shared] (the folder is synced by MeshSync) it turns accent-colored and carries the
 * app mark inside, so shared folders stand out in a plain listing without relying on color alone.
 * [contentDescription] should name the state, e.g. "Folder, synced"; pass null when the row's text already says it.
 */
@Composable
fun NgFolderIcon(
    shared: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    contentDescription: String? = null,
) {
    val c = NgTheme.colors
    val tint = if (shared) c.cyan else c.text2
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Icon(NgIcons.Folder, contentDescription, tint = tint, modifier = Modifier.size(size))
        if (shared) {
            // The folder body spans y 8..20 of the 24-unit glyph, so its center sits 2 units below the middle.
            Icon(NgIcons.MeshSync, null, tint = tint, modifier = Modifier.size(size * 0.42f).offset(y = size * (2f / 24f)))
        }
    }
}
