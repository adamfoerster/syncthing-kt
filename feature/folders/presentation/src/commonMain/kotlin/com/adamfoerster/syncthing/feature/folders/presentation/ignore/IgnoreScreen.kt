package com.adamfoerster.syncthing.feature.folders.presentation.ignore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adamfoerster.syncthing.core.designsystem.components.NgActionBar
import com.adamfoerster.syncthing.core.designsystem.components.NgButton
import com.adamfoerster.syncthing.core.designsystem.components.NgButtonStyle
import com.adamfoerster.syncthing.core.designsystem.components.NgCallout
import com.adamfoerster.syncthing.core.designsystem.components.NgIconButton
import com.adamfoerster.syncthing.core.designsystem.components.NgPanel
import com.adamfoerster.syncthing.core.designsystem.components.NgScreen
import com.adamfoerster.syncthing.core.designsystem.components.NgSectionLabel
import com.adamfoerster.syncthing.core.designsystem.components.NgTextField
import com.adamfoerster.syncthing.core.designsystem.components.NgTopBar
import com.adamfoerster.syncthing.core.designsystem.components.PanelTone
import com.adamfoerster.syncthing.core.designsystem.icons.NgIcons
import com.adamfoerster.syncthing.core.designsystem.theme.NgTheme
import com.adamfoerster.syncthing.core.designsystem.theme.SyncthingTheme
import com.adamfoerster.syncthing.core.domain.model.IgnorePreset
import com.adamfoerster.syncthing.core.domain.model.IgnoreRule
import com.adamfoerster.syncthing.core.presentation.ObserveAsEvents
import com.adamfoerster.syncthing.feature.folders.presentation.resources.Res
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_add_button
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_add_hint
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_add_label
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_add_placeholder
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_add_section
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_advanced_button
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_advanced_editor
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_advanced_intro
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_for_folder
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_braces
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_case
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_comment
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_delete
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_doublestar
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_not
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_question
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_root
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_star
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_help_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_intro
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_kind_advanced
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_kind_file_type
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_kind_name
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_list_empty
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_list_section
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_loading
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_android_body
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_android_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_programming_body
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_programming_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_system_body
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_system_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_temporary_body
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_preset_temporary_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_quick
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_remove
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_retry
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_save
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_simple_button
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_title
import com.adamfoerster.syncthing.feature.folders.presentation.resources.ignore_unsaved
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun IgnoreRoot(
    folderId: String,
    onDone: () -> Unit,
    viewModel: IgnoreViewModel = koinViewModel(key = folderId) { parametersOf(folderId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            IgnoreEvent.Saved -> onDone()
        }
    }
    IgnoreScreen(
        state = state,
        onAction = { action -> if (action == IgnoreAction.OnBackClick) onDone() else viewModel.onAction(action) },
    )
}

@Composable
fun IgnoreScreen(
    state: IgnoreState,
    onAction: (IgnoreAction) -> Unit,
) {
    val c = NgTheme.colors
    NgScreen(
        topBar = { NgTopBar(stringResource(Res.string.ignore_title), onBack = { onAction(IgnoreAction.OnBackClick) }) },
        bottomBar = if (state.phase == IgnorePhase.READY) {
            {
                NgActionBar {
                    val error = state.error
                    when {
                        error != null -> Text(error.asString(), style = NgTheme.type.sub, color = c.err, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        state.isDirty -> Text(stringResource(Res.string.ignore_unsaved), style = NgTheme.type.sub, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    NgButton(
                        stringResource(Res.string.ignore_save),
                        onClick = { onAction(IgnoreAction.OnSave) },
                        style = NgButtonStyle.Primary,
                        icon = NgIcons.Check,
                        enabled = state.isDirty && !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            null
        },
    ) {
        when (state.phase) {
            IgnorePhase.LOADING -> Text(stringResource(Res.string.ignore_loading), style = NgTheme.type.sub, color = c.text2)
            IgnorePhase.LOAD_ERROR -> NgCallout(
                body = state.error?.asString().orEmpty(),
                tone = PanelTone.Warn,
            ) {
                NgButton(
                    stringResource(Res.string.ignore_retry),
                    onClick = { onAction(IgnoreAction.OnRetry) },
                    icon = NgIcons.Restart,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            IgnorePhase.READY -> {
                Text(stringResource(Res.string.ignore_for_folder, state.folderLabel), style = NgTheme.type.h3, color = c.text)
                if (state.advanced) AdvancedEditor(state, onAction) else SimpleEditor(state, onAction)
            }
        }
    }
}

@Composable
private fun SimpleEditor(state: IgnoreState, onAction: (IgnoreAction) -> Unit) {
    val c = NgTheme.colors
    NgCallout(body = stringResource(Res.string.ignore_intro))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NgSectionLabel(stringResource(Res.string.ignore_quick))
        state.presets.forEach { (preset, active) ->
            NgPanel(
                tone = if (active) PanelTone.Selected else PanelTone.Default,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(active, role = Role.Checkbox, onValueChange = { onAction(IgnoreAction.OnTogglePreset(preset)) }),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(preset.title()), style = NgTheme.type.body, color = c.text)
                        Text(stringResource(preset.body()), style = NgTheme.type.sub, color = c.text2)
                    }
                    Checkbox(
                        checked = active,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = c.cyan, checkmarkColor = c.bg, uncheckedColor = c.lineStrong),
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NgSectionLabel(stringResource(Res.string.ignore_add_section))
        NgTextField(
            value = state.input,
            onValueChange = { onAction(IgnoreAction.OnInputChange(it)) },
            label = stringResource(Res.string.ignore_add_label),
            placeholder = stringResource(Res.string.ignore_add_placeholder),
            supportingText = state.inputError?.asString() ?: stringResource(Res.string.ignore_add_hint),
            isError = state.inputError != null,
            textStyle = NgTheme.type.mono14,
        )
        NgButton(
            stringResource(Res.string.ignore_add_button),
            onClick = { onAction(IgnoreAction.OnAddClick) },
            icon = NgIcons.Plus,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NgSectionLabel(stringResource(Res.string.ignore_list_section))
        if (state.rules.isEmpty()) {
            Text(stringResource(Res.string.ignore_list_empty), style = NgTheme.type.sub, color = c.text2)
        }
        state.rules.forEach { rule -> RuleRow(rule, onRemove = { onAction(IgnoreAction.OnRemove(rule.line)) }) }
    }

    NgButton(
        stringResource(Res.string.ignore_advanced_button),
        onClick = { onAction(IgnoreAction.OnAdvancedChange(true)) },
        icon = NgIcons.Edit,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RuleRow(rule: IgnoreRule, onRemove: () -> Unit) {
    val c = NgTheme.colors
    NgPanel(contentPadding = PaddingValues(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.line, style = NgTheme.type.mono14, color = c.text)
                Text(
                    when (rule) {
                        is IgnoreRule.FileType -> stringResource(Res.string.ignore_kind_file_type, rule.extension)
                        is IgnoreRule.Name -> stringResource(Res.string.ignore_kind_name)
                        is IgnoreRule.Comment, is IgnoreRule.Advanced -> stringResource(Res.string.ignore_kind_advanced)
                    },
                    style = NgTheme.type.sub,
                    color = c.text2,
                )
            }
            NgIconButton(NgIcons.Close, stringResource(Res.string.ignore_remove, rule.line), onRemove)
        }
    }
}

@Composable
private fun AdvancedEditor(state: IgnoreState, onAction: (IgnoreAction) -> Unit) {
    val c = NgTheme.colors
    NgCallout(body = stringResource(Res.string.ignore_advanced_intro))
    NgTextField(
        value = state.text,
        onValueChange = { onAction(IgnoreAction.OnTextChange(it)) },
        label = stringResource(Res.string.ignore_advanced_editor),
        singleLine = false,
        textStyle = NgTheme.type.mono14,
    )
    NgPanel(Modifier.fillMaxWidth()) {
        NgSectionLabel(stringResource(Res.string.ignore_help_title))
        helpItems.forEach { (code, description) ->
            Column {
                Text(code, style = NgTheme.type.mono14, color = c.text)
                Text(stringResource(description), style = NgTheme.type.sub, color = c.text2)
            }
        }
    }
    NgButton(
        stringResource(Res.string.ignore_simple_button),
        onClick = { onAction(IgnoreAction.OnAdvancedChange(false)) },
        icon = NgIcons.Back,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The syntax itself is not translated; only its explanation is. */
private val helpItems: List<Pair<String, StringResource>> = listOf(
    "*.log" to Res.string.ignore_help_star,
    "**/cache" to Res.string.ignore_help_doublestar,
    "photo?.jpg" to Res.string.ignore_help_question,
    "*.{jpg,png}" to Res.string.ignore_help_braces,
    "/Downloads" to Res.string.ignore_help_root,
    "!keep.txt" to Res.string.ignore_help_not,
    "(?i)readme*" to Res.string.ignore_help_case,
    "(?d)Thumbs.db" to Res.string.ignore_help_delete,
    "// note" to Res.string.ignore_help_comment,
)

private fun IgnorePreset.title(): StringResource = when (this) {
    IgnorePreset.TEMPORARY_FILES -> Res.string.ignore_preset_temporary_title
    IgnorePreset.SYSTEM_FILES -> Res.string.ignore_preset_system_title
    IgnorePreset.ANDROID_LEFTOVERS -> Res.string.ignore_preset_android_title
    IgnorePreset.PROGRAMMING_FOLDERS -> Res.string.ignore_preset_programming_title
}

private fun IgnorePreset.body(): StringResource = when (this) {
    IgnorePreset.TEMPORARY_FILES -> Res.string.ignore_preset_temporary_body
    IgnorePreset.SYSTEM_FILES -> Res.string.ignore_preset_system_body
    IgnorePreset.ANDROID_LEFTOVERS -> Res.string.ignore_preset_android_body
    IgnorePreset.PROGRAMMING_FOLDERS -> Res.string.ignore_preset_programming_body
}

@Preview
@Composable
private fun IgnoreScreenPreview() {
    SyncthingTheme {
        IgnoreScreen(
            state = IgnoreState(
                phase = IgnorePhase.READY,
                folderLabel = "Documentos",
                text = "*.tmp\n*.temp\n~$*\nrascunhos\n(?i)backup*",
                savedText = "*.tmp",
            ),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun IgnoreScreenAdvancedPreview() {
    SyncthingTheme {
        IgnoreScreen(
            state = IgnoreState(
                phase = IgnorePhase.READY,
                folderLabel = "Documentos",
                advanced = true,
                text = "// fotos\n*.tmp\n!importante.tmp\n(?d).DS_Store",
                savedText = "*.tmp",
            ),
            onAction = {},
        )
    }
}
