package com.adamfoerster.syncthing.feature.conflicts.presentation.binary

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
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
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_confirm
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_helper_pending
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_helper_ready
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_intro_file
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_intro_image
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_both
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_both_hint
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_mine
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_mine_hint
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_one
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_keep_one_hint
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_preview_unavailable
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.binary_question
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_folder
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_loading
import com.adamfoerster.syncthing.feature.conflicts.presentation.resources.resolver_not_found
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.ResolverPhase
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BinaryConflictRoot(
    conflictId: String,
    onBack: () -> Unit,
    viewModel: BinaryConflictViewModel = koinViewModel(key = conflictId) { parametersOf(conflictId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BinaryConflictScreen(
        state = state,
        onAction = { action ->
            if (action == BinaryConflictAction.OnBackClick) onBack() else viewModel.onAction(action)
        },
    )
}

@Composable
fun BinaryConflictScreen(
    state: BinaryConflictState,
    onAction: (BinaryConflictAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = {
            NgTopBar(
                title = state.fileName,
                subtitle = state.folderLabel.takeIf { it.isNotEmpty() }?.let { stringResource(Res.string.resolver_folder, it) },
                titleStyle = NgTheme.type.mono14.copy(fontSize = 15.sp),
                onBack = { onAction(BinaryConflictAction.OnBackClick) },
            )
        },
        bottomBar = {
            when (state.phase) {
                ResolverPhase.READY -> ConfirmBar(state, onAction)
                ResolverPhase.DONE, ResolverPhase.NOT_FOUND -> BackToListBar { onAction(BinaryConflictAction.OnBackClick) }
                ResolverPhase.LOADING -> Unit
            }
        },
        bodyArrangement = if (state.phase == ResolverPhase.DONE) Arrangement.Center else Arrangement.spacedBy(16.dp),
    ) {
        when (state.phase) {
            ResolverPhase.LOADING -> Text(stringResource(Res.string.resolver_loading), style = NgTheme.type.sub, color = c.text2)
            ResolverPhase.NOT_FOUND -> NgCallout(body = stringResource(Res.string.resolver_not_found), tone = PanelTone.Warn)
            ResolverPhase.DONE -> ResolvedPanel(state.doneMessage?.asString().orEmpty())
            ResolverPhase.READY -> {
                val first = state.first ?: return@NgScreen
                val second = state.second ?: return@NgScreen
                Text(
                    stringResource(if (state.isImage) Res.string.binary_intro_image else Res.string.binary_intro_file),
                    style = NgTheme.type.sub,
                    color = c.text2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VersionCard(first, state.isImage, selected = state.pick == BinaryPick.FIRST || state.pick == BinaryPick.BOTH, Modifier.weight(1f))
                    VersionCard(second, state.isImage, selected = state.pick == BinaryPick.SECOND || state.pick == BinaryPick.BOTH, Modifier.weight(1f))
                }
                Options(state, first.version, second.version, onAction)
            }
        }
    }
}

@Composable
private fun VersionCard(item: BinaryVersionUi, isImage: Boolean, selected: Boolean, modifier: Modifier) {
    val c = NgTheme.colors
    NgPanel(
        modifier = modifier,
        tone = if (selected) PanelTone.Selected else PanelTone.Default,
        contentPadding = PaddingValues(10.dp),
        spacing = 8.dp,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(c.panel)
                .stripes()
                .border(1.dp, c.lineStrong),
            contentAlignment = Alignment.Center,
        ) {
            val preview = item.preview
            if (preview != null) {
                Image(
                    preview.bitmap,
                    contentDescription = item.version.author.asString(),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().height(148.dp),
                )
            } else {
                Column(
                    Modifier.background(c.panel).padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(if (isImage) NgIcons.Image else NgIcons.File, null, tint = c.text2, modifier = Modifier.size(24.dp))
                    Text(stringResource(Res.string.binary_preview_unavailable), style = NgTheme.type.monoSmall.copy(fontSize = 11.sp), color = c.text2)
                }
            }
        }
        Text(item.version.author.asString(), style = NgTheme.type.bodyStrong.copy(fontSize = 13.sp, lineHeight = 16.sp), color = c.text)
        val dims = item.preview?.let { "${it.width}×${it.height}" }
        Text(
            listOfNotNull(item.version.dayAndTime?.asString(), listOfNotNull(item.version.size, dims).joinToString(" · ")).joinToString("\n"),
            style = NgTheme.type.monoSmall.copy(fontSize = 11.sp),
            color = c.text2,
        )
    }
}

/** The striped "no image" backdrop from the mockup. */
@Composable
private fun Modifier.stripes(): Modifier {
    val stripe = NgTheme.colors.raised
    return drawBehind {
        val step = 16.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(stripe, Offset(x, size.height), Offset(x + size.height, 0f), strokeWidth = 8.dp.toPx() / 1.41f, cap = StrokeCap.Butt)
            x += step
        }
    }
}

@Composable
private fun Options(
    state: BinaryConflictState,
    first: VersionUi,
    second: VersionUi,
    onAction: (BinaryConflictAction) -> Unit,
) {
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NgSectionLabel(stringResource(Res.string.binary_question))
        OptionRow(
            title = keepTitle(first),
            hint = keepHint(other = second),
            selected = state.pick == BinaryPick.FIRST,
            onClick = { onAction(BinaryConflictAction.OnPick(BinaryPick.FIRST)) },
        )
        OptionRow(
            title = keepTitle(second),
            hint = keepHint(other = first),
            selected = state.pick == BinaryPick.SECOND,
            onClick = { onAction(BinaryConflictAction.OnPick(BinaryPick.SECOND)) },
        )
        OptionRow(
            title = stringResource(Res.string.binary_keep_both),
            hint = stringResource(Res.string.binary_keep_both_hint, state.keepBothName),
            selected = state.pick == BinaryPick.BOTH,
            onClick = { onAction(BinaryConflictAction.OnPick(BinaryPick.BOTH)) },
        )
    }
}

@Composable
private fun OptionRow(title: String, hint: String, selected: Boolean, onClick: () -> Unit) {
    val c = NgTheme.colors
    NgPanel(
        tone = if (selected) PanelTone.Selected else PanelTone.Default,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = c.cyan, unselectedColor = c.lineStrong),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = NgTheme.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.text)
                Text(hint, style = NgTheme.type.sub, color = c.text2)
            }
        }
    }
}

@Composable
private fun ConfirmBar(state: BinaryConflictState, onAction: (BinaryConflictAction) -> Unit) {
    NgActionBar {
        val error = state.error
        if (error != null) {
            Text(error.asString(), style = NgTheme.type.sub, color = NgTheme.colors.err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            HelperText(stringResource(if (state.pick != null) Res.string.binary_helper_ready else Res.string.binary_helper_pending))
        }
        NgButton(
            text = stringResource(Res.string.binary_confirm),
            onClick = { onAction(BinaryConflictAction.OnConfirm) },
            style = NgButtonStyle.Primary,
            icon = NgIcons.Check,
            enabled = state.canConfirm,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun BinaryConflictScreenPreview() {
    SyncthingTheme {
        BinaryConflictScreen(
            state = BinaryConflictState(
                phase = ResolverPhase.READY,
                fileName = "convite.png",
                folderLabel = "Documentos",
                isImage = true,
                first = BinaryVersionUi(VersionUi(VersionSide.CURRENT, UiText.DynamicString("Este celular"), "E", true, "14:32", UiText.DynamicString("Hoje, 14:32"), "2,1 MB"), null),
                second = BinaryVersionUi(VersionUi(VersionSide.COPY, UiText.DynamicString("PC escritório"), "P", false, "14:41", UiText.DynamicString("Hoje, 14:41"), "3,4 MB"), null),
                pick = BinaryPick.SECOND,
                keepBothName = "convite (cópia).png",
            ),
            onAction = {},
        )
    }
}

@Composable
private fun keepTitle(version: VersionUi): String =
    if (version.isLocal) stringResource(Res.string.binary_keep_mine) else stringResource(Res.string.binary_keep_one, version.author.asString())

@Composable
private fun keepHint(other: VersionUi): String =
    if (other.isLocal) stringResource(Res.string.binary_keep_mine_hint) else stringResource(Res.string.binary_keep_one_hint, other.author.asString())
