package com.adamfoerster.syncthing.feature.conflicts.presentation.text

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgBadge
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgProgressBar
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgSegment
import com.adamfoerster.syncthing.core.designsystem.components.NgSegmentedRow
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.components.Tone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.BackToListBar
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.HelperText
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.ResolvedPanel
import com.adamfoerster.syncthing.feature.conflicts.presentation.common.VersionUi
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.Res
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_folder
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_loading
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_not_found
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_all_from
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_all_mine
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_badge_pending
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_badge_ready
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_done_body
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_edit_active
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_edit_discard
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_edit_hint
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_edit_manually
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_empty_side
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_finish
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_helper_markers
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_helper_pending
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_helper_ready
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_hunk_resolved
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_hunk_title
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_hunk_undecided
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_pick_both
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_result_pending
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_same_lines
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_side_header
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_summary
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_summary_same
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_view_compare
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.text_view_result
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TextConflictRoot(
    conflictId: String,
    onBack: () -> Unit,
    viewModel: TextConflictViewModel = koinViewModel(key = conflictId) { parametersOf(conflictId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TextConflictScreen(
        state = state,
        onAction = { action ->
            if (action == TextConflictAction.OnBackClick) onBack() else viewModel.onAction(action)
        },
    )
}

@Composable
fun TextConflictScreen(
    state: TextConflictState,
    onAction: (TextConflictAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                title = state.fileName,
                subtitle = state.folderLabel.takeIf { it.isNotEmpty() }?.let { stringResource(Res.string.resolver_folder, it) },
                titleStyle = NgTheme.type.mono14.copy(fontSize = 15.sp),
                onBack = { onAction(TextConflictAction.OnBackClick) },
            )
        },
        bottomBar = {
            when (state.phase) {
                ResolverPhase.READY -> FinishBar(state, onAction)
                ResolverPhase.DONE, ResolverPhase.NOT_FOUND -> BackToListBar { onAction(TextConflictAction.OnBackClick) }
                ResolverPhase.LOADING -> Unit
            }
        },
        scrollable = state.phase != ResolverPhase.READY,
        bodyArrangement = if (state.phase == ResolverPhase.DONE) Arrangement.Center else Arrangement.spacedBy(16.dp),
    ) {
        when (state.phase) {
            ResolverPhase.LOADING -> Text(stringResource(Res.string.resolver_loading), style = NgTheme.type.sub, color = c.text2)
            ResolverPhase.NOT_FOUND -> NgCallout(
                body = state.error?.asString() ?: stringResource(Res.string.resolver_not_found),
                tone = PanelTone.Warn,
            )
            ResolverPhase.DONE -> ResolvedPanel(stringResource(Res.string.text_done_body, state.fileName))
            ResolverPhase.READY -> ResolverBody(state, onAction, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResolverBody(state: TextConflictState, onAction: (TextConflictAction) -> Unit, modifier: Modifier) {
    val first = state.first ?: return
    val second = state.second ?: return
    // NgScreen pads the body by 16dp; the lazy list scrolls inside that.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        item(key = "progress") { ProgressPanel(state, first, second, onAction) }
        item(key = "views") {
            NgSegmentedRow(Modifier.padding(vertical = 16.dp)) {
                NgSegment(
                    stringResource(Res.string.text_view_compare),
                    selected = state.view == TextView.COMPARE,
                    onClick = { onAction(TextConflictAction.OnViewChange(TextView.COMPARE)) },
                )
                NgSegment(
                    stringResource(Res.string.text_view_result),
                    selected = state.view == TextView.RESULT,
                    onClick = { onAction(TextConflictAction.OnViewChange(TextView.RESULT)) },
                )
            }
        }
        when {
            state.view == TextView.COMPARE -> compareItems(state, first, second, onAction)
            state.isEditing -> item(key = "editor") { Editor(state, onAction) }
            else -> resultItems(state, onAction)
        }
    }
}

@Composable
private fun ProgressPanel(
    state: TextConflictState,
    first: VersionUi,
    second: VersionUi,
    onAction: (TextConflictAction) -> Unit,
) {
    val ready = state.resolvedCount == state.hunkTotal
    NgPanel(Modifier.fillMaxWidth(), spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (state.hunkTotal == 0) {
                    stringResource(Res.string.text_summary_same)
                } else {
                    stringResource(Res.string.text_summary, state.resolvedCount, state.hunkTotal)
                },
                style = NgTheme.type.h3,
                color = NgTheme.colors.text,
                modifier = Modifier.weight(1f),
            )
            NgBadge(
                stringResource(if (ready) Res.string.text_badge_ready else Res.string.text_badge_pending),
                tone = if (ready) Tone.Ok else Tone.Warn,
            )
        }
        if (state.hunkTotal in 1..MAX_SEGMENTED_BARS) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                state.blocks.filterIsInstance<DiffBlockUi.Hunk>().forEach { hunk ->
                    NgProgressBar(if (hunk.pick != null) 1f else 0f, Modifier.weight(1f))
                }
            }
        } else if (state.hunkTotal > 0) {
            NgProgressBar(state.resolvedCount.toFloat() / state.hunkTotal)
        }
        if (state.hunkTotal > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NgButton(
                    text = allFrom(first),
                    onClick = { onAction(TextConflictAction.OnPickAll(Pick.FIRST)) },
                    style = NgButtonStyle.Tertiary,
                    minHeight = 44.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                NgButton(
                    text = allFrom(second),
                    onClick = { onAction(TextConflictAction.OnPickAll(Pick.SECOND)) },
                    style = NgButtonStyle.Tertiary,
                    minHeight = 44.dp,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun LazyListScope.compareItems(
    state: TextConflictState,
    first: VersionUi,
    second: VersionUi,
    onAction: (TextConflictAction) -> Unit,
) {
    val last = state.blocks.lastIndex
    state.blocks.forEachIndexed { index, block ->
        val isTop = index == 0
        val isBottom = index == last
        when (block) {
            is DiffBlockUi.Context -> item(key = "ctx-${block.blockIndex}") {
                ContextBlock(
                    block,
                    Modifier.diffFrame(isTop, isBottom),
                    onExpand = { onAction(TextConflictAction.OnExpandContext(block.blockIndex)) },
                )
            }
            is DiffBlockUi.Hunk -> item(key = "hunk-${block.hunkIndex}") {
                HunkBlock(block, state.hunkTotal, first, second, Modifier.diffFrame(isTop, isBottom), onAction)
            }
        }
    }
}

/** The diff reads as one bordered box even though each block is its own lazy item. */
@Composable
private fun Modifier.diffFrame(isTop: Boolean, isBottom: Boolean): Modifier {
    val c = NgTheme.colors
    return this
        .fillMaxWidth()
        .background(c.bg)
        .drawBehind {
            val w = 1.dp.toPx()
            drawLine(c.lineStrong, Offset(w / 2, 0f), Offset(w / 2, size.height), w)
            drawLine(c.lineStrong, Offset(size.width - w / 2, 0f), Offset(size.width - w / 2, size.height), w)
            if (isTop) drawLine(c.lineStrong, Offset(0f, w / 2), Offset(size.width, w / 2), w)
            if (isBottom) drawLine(c.lineStrong, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
        }
}

@Composable
private fun ContextBlock(block: DiffBlockUi.Context, modifier: Modifier, onExpand: () -> Unit) {
    val c = NgTheme.colors
    Column(modifier.padding(vertical = 6.dp)) {
        block.head.forEach { CodeLine(it.number, it.text) }
        if (block.hiddenCount > 0) {
            Text(
                stringResource(Res.string.text_same_lines, block.hiddenCount),
                style = NgTheme.type.monoSmall,
                color = c.cyan,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onExpand)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        block.tail.forEach { CodeLine(it.number, it.text) }
    }
}

@Composable
private fun HunkBlock(
    hunk: DiffBlockUi.Hunk,
    total: Int,
    first: VersionUi,
    second: VersionUi,
    modifier: Modifier,
    onAction: (TextConflictAction) -> Unit,
) {
    val c = NgTheme.colors
    val dimFirst = hunk.pick == Pick.SECOND
    val dimSecond = hunk.pick == Pick.FIRST
    Column(
        modifier
            .background(c.panel)
            .drawBehind {
                val w = 1.dp.toPx()
                drawLine(c.lineStrong, Offset(0f, w / 2), Offset(size.width, w / 2), w)
                drawLine(c.lineStrong, Offset(0f, size.height - w / 2), Offset(size.width, size.height - w / 2), w)
            }
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            NgSectionLabel(stringResource(Res.string.text_hunk_title, hunk.number, total), Modifier.weight(1f))
            NgBadge(
                stringResource(if (hunk.pick != null) Res.string.text_hunk_resolved else Res.string.text_hunk_undecided),
                tone = if (hunk.pick != null) Tone.Ok else Tone.Warn,
                icon = if (hunk.pick != null) NgIcons.Check else NgIcons.Warning,
            )
        }
        NgSegmentedRow(Modifier.padding(horizontal = 12.dp)) {
            NgSegment(first.author.asString(), hunk.pick == Pick.FIRST, { onAction(TextConflictAction.OnPick(hunk.hunkIndex, Pick.FIRST)) })
            NgSegment(second.author.asString(), hunk.pick == Pick.SECOND, { onAction(TextConflictAction.OnPick(hunk.hunkIndex, Pick.SECOND)) })
            NgSegment(stringResource(Res.string.text_pick_both), hunk.pick == Pick.BOTH, { onAction(TextConflictAction.OnPick(hunk.hunkIndex, Pick.BOTH)) })
        }
        HunkSide(first, hunk.firstLines, isFirst = true, dimmed = dimFirst)
        HunkSide(second, hunk.secondLines, isFirst = false, dimmed = dimSecond)
    }
}

@Composable
private fun HunkSide(version: VersionUi, lines: List<HunkLineUi>, isFirst: Boolean, dimmed: Boolean) {
    val c = NgTheme.colors
    val fg = if (isFirst) c.ok else c.info
    val headerBg = if (isFirst) c.okSub else c.infoSub
    val lineBg = if (isFirst) c.oursLine else c.theirsLine
    val highlightBg = if (isFirst) c.oursHighlight else c.theirsHighlight
    Column(Modifier.alpha(if (dimmed) 0.45f else 1f)) {
        Row(
            Modifier.fillMaxWidth().background(headerBg).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (version.isLocal) NgIcons.Phone else NgIcons.Devices, null, tint = fg, modifier = Modifier.size(14.dp))
            val who = version.author.asString()
            Text(
                (version.clock?.let { stringResource(Res.string.text_side_header, who, it) } ?: who).uppercase(),
                style = NgTheme.type.tag.copy(lineHeight = 16.sp),
                color = fg,
            )
        }
        if (lines.isEmpty()) {
            Text(
                stringResource(Res.string.text_empty_side),
                style = NgTheme.type.code,
                color = c.text2,
                modifier = Modifier.fillMaxWidth().background(lineBg).padding(horizontal = 36.dp),
            )
        }
        lines.forEach { line ->
            CodeLine(
                number = line.number,
                text = line.text,
                background = lineBg,
                highlight = line.highlight,
                highlightColor = highlightBg,
                strike = dimmed,
            )
        }
    }
}

@Composable
private fun CodeLine(
    number: Int,
    text: String,
    background: Color = Color.Transparent,
    foreground: Color = NgTheme.colors.text,
    highlight: IntRange? = null,
    highlightColor: Color = Color.Transparent,
    strike: Boolean = false,
) {
    val c = NgTheme.colors
    Row(
        Modifier.fillMaxWidth().background(background).padding(end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            number.toString(),
            style = NgTheme.type.lineNumber,
            color = c.text2,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp).padding(end = 4.dp),
        )
        val annotated = buildAnnotatedString {
            if (highlight == null || highlight.first > text.length) {
                append(text)
            } else {
                append(text.substring(0, highlight.first))
                withStyle(SpanStyle(background = highlightColor)) {
                    append(text.substring(highlight.first, (highlight.last + 1).coerceAtMost(text.length)))
                }
                append(text.substring((highlight.last + 1).coerceAtMost(text.length)))
            }
        }
        Text(
            annotated,
            style = NgTheme.type.code.copy(textDecoration = if (strike) TextDecoration.LineThrough else null),
            color = foreground,
            modifier = Modifier.weight(1f).heightIn(min = 20.dp),
        )
    }
}

private fun LazyListScope.resultItems(state: TextConflictState, onAction: (TextConflictAction) -> Unit) {
    item(key = "result-top") { Box(Modifier.diffFrame(isTop = true, isBottom = false).padding(top = 6.dp)) }
    items(state.result, key = { "r-${it.number}" }) { line ->
        val c = NgTheme.colors
        Box(Modifier.diffFrame(isTop = false, isBottom = false)) {
            when (line.origin) {
                LineOrigin.PENDING -> CodeLine(
                    line.number,
                    stringResource(Res.string.text_result_pending, line.text.toInt()),
                    background = c.warnSub,
                    foreground = c.warn,
                )
                LineOrigin.FIRST -> CodeLine(line.number, line.text, background = c.oursLine)
                LineOrigin.SECOND -> CodeLine(line.number, line.text, background = c.theirsLine)
                LineOrigin.COMMON -> CodeLine(line.number, line.text)
            }
        }
    }
    item(key = "result-bottom") { Box(Modifier.diffFrame(isTop = false, isBottom = true).padding(bottom = 6.dp)) }
    item(key = "edit") {
        NgButton(
            text = stringResource(Res.string.text_edit_manually),
            onClick = { onAction(TextConflictAction.OnStartEditing) },
            icon = NgIcons.Edit,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
    }
}

@Composable
private fun Editor(state: TextConflictState, onAction: (TextConflictAction) -> Unit) {
    val c = NgTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        NgCallout(body = stringResource(Res.string.text_edit_hint))
        BasicTextField(
            value = state.editorText,
            onValueChange = { onAction(TextConflictAction.OnEditorChange(it)) },
            textStyle = NgTheme.type.code.copy(color = c.text),
            cursorBrush = SolidColor(c.cyan),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 240.dp)
                .background(c.bg)
                .border(1.dp, if (state.editorHasMarkers) c.warn else c.lineStrong)
                .padding(12.dp),
        )
        Text(stringResource(Res.string.text_edit_active), style = NgTheme.type.sub, color = c.text2)
        NgButton(
            text = stringResource(Res.string.text_edit_discard),
            onClick = { onAction(TextConflictAction.OnDiscardEdit) },
            style = NgButtonStyle.Danger,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FinishBar(state: TextConflictState, onAction: (TextConflictAction) -> Unit) {
    NgActionBar {
        val error = state.error
        if (error != null) {
            Text(error.asString(), style = NgTheme.type.sub, color = NgTheme.colors.err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            HelperText(
                stringResource(
                    when {
                        state.isEditing && state.editorHasMarkers -> Res.string.text_helper_markers
                        state.canFinish -> Res.string.text_helper_ready
                        else -> Res.string.text_helper_pending
                    },
                ),
            )
        }
        NgButton(
            text = stringResource(Res.string.text_finish),
            onClick = { onAction(TextConflictAction.OnFinish) },
            style = NgButtonStyle.Primary,
            icon = NgIcons.Check,
            enabled = state.canFinish,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val MAX_SEGMENTED_BARS = 8

@Preview
@Composable
private fun TextConflictScreenPreview() {
    val mine = VersionUi(VersionSide.CURRENT, UiText.DynamicString("Este celular"), "E", true, "14:32", null, "1 KB")
    val theirs = VersionUi(VersionSide.COPY, UiText.DynamicString("Notebook da Ana"), "N", false, "14:35", null, "1 KB")
    SyncthingTheme {
        TextConflictScreen(
            state = TextConflictState(
                phase = ResolverPhase.READY,
                fileName = "lista-de-compras.txt",
                folderLabel = "Documentos",
                first = mine,
                second = theirs,
                hunkTotal = 1,
                blocks = listOf(
                    DiffBlockUi.Context(0, listOf(NumberedLine(1, "Frutas"), NumberedLine(2, "- Maçã")), 0, emptyList()),
                    DiffBlockUi.Hunk(
                        hunkIndex = 0,
                        number = 1,
                        pick = Pick.FIRST,
                        firstLines = listOf(HunkLineUi(3, "- Leite integral (2 caixas)", 8..15)),
                        secondLines = listOf(HunkLineUi(3, "- Leite desnatado (3 caixas)", 8..16)),
                    ),
                ),
                resolvedCount = 1,
            ),
            onAction = {},
        )
    }
}

@Composable
private fun allFrom(version: VersionUi): String =
    if (version.isLocal) stringResource(Res.string.text_all_mine) else stringResource(Res.string.text_all_from, version.author.asString())
