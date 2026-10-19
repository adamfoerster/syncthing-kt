package com.adamfoerster.syncthing.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.resources.Res
import com.adamfoerster.syncthing.core.designsystem.resources.ng_back
import org.jetbrains.compose.resources.stringResource
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme

/**
 * `.screen`: top bar, a scrolling body (16dp padding, 16dp gaps) and an optional bottom bar
 * (tab bar or action bar). Handles system-bar and keyboard insets.
 */
@Composable
fun NgScreen(
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: (@Composable () -> Unit)? = null,
    scrollable: Boolean = true,
    bodyArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = NgTheme.colors
    Column(modifier.fillMaxSize().background(c.bg)) {
        topBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(16.dp),
            verticalArrangement = bodyArrangement,
            content = content,
        )
        if (bottomBar != null) bottomBar() else Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
    }
}

/** `.top` — [onBack] turns it into the `.top.back` variant. */
@Composable
fun NgTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    titleStyle: TextStyle = NgTheme.type.title,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = NgTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.panel)
            .drawBehind {
                drawLine(c.line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
            }
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = if (onBack != null) 4.dp else 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) NgIconButton(NgIcons.Back, contentDescription = stringResource(Res.string.ng_back), onClick = onBack)
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = titleStyle,
                    color = c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                if (subtitle != null) {
                    Text(subtitle, style = NgTheme.type.sub.copy(fontSize = titleStyle.fontSize * 0.8f), color = c.text2, maxLines = 1)
                }
            }
            actions()
        }
    }
}

/** `.ibtn`, optionally bordered (`.ibtn.bd`). */
@Composable
fun NgIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bordered: Boolean = false,
) {
    val c = NgTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .then(if (bordered) Modifier.border(1.dp, c.lineStrong) else Modifier)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.text, modifier = Modifier.size(20.dp))
    }
}

data class NgTab(val label: String, val icon: ImageVector)

/** `.tabbar`: selected tab gets accent text, tinted fill and a 2dp top rule. */
@Composable
fun NgTabBar(
    tabs: List<NgTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = NgTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .background(c.panel)
            .drawBehind { drawLine(c.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Row(Modifier.fillMaxWidth().height(68.dp)) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val fg = if (selected) c.cyan else c.text2
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (selected) c.cyanSub else c.panel)
                        .drawBehind {
                            if (selected) drawLine(c.cyan, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx())
                        }
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                ) {
                    Icon(tab.icon, null, tint = fg, modifier = Modifier.size(20.dp))
                    Text(tab.label.uppercase(), style = NgTheme.type.tab, color = fg, maxLines = 1)
                }
            }
        }
    }
}

/** `.actionbar`: sticky bottom area for the primary action of a flow. */
@Composable
fun NgActionBar(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = NgTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(c.panel)
            .drawBehind { drawLine(c.line, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** `.lbl` section heading. */
@Composable
fun NgSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = NgTheme.type.label,
        color = NgTheme.colors.text2,
        modifier = modifier.semantics { heading() },
    )
}

/** Row of an icon and text, used for inline callouts. */
@Composable
fun NgIconText(icon: ImageVector, text: String, tint: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(text, style = NgTheme.type.sub.copy(fontSize = NgTheme.type.chip.fontSize), color = NgTheme.colors.text2)
    }
}
