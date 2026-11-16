package com.adamfoerster.meshsync.core.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamfoerster.meshsync.core.designsystem.components.NgButton
import com.adamfoerster.meshsync.core.designsystem.components.NgButtonStyle
import com.adamfoerster.meshsync.core.designsystem.theme.NgShapes
import com.adamfoerster.meshsync.core.designsystem.theme.NgTheme
import com.adamfoerster.meshsync.core.presentation.resources.Res
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_1_18_0_fixed_progress
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_1_18_0_new_pending
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_1_18_0_new_popup
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_1_19_0_new_share
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_close
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_kind_changed
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_kind_fixed
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_kind_new
import com.adamfoerster.meshsync.core.presentation.resources.release_notes_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

enum class ReleaseNoteKind(val label: StringResource) {
    New(Res.string.release_notes_kind_new),
    Changed(Res.string.release_notes_kind_changed),
    Fixed(Res.string.release_notes_kind_fixed),
}

data class ReleaseNoteItem(val kind: ReleaseNoteKind, val text: StringResource)

/** The short, user-facing summary of one version (the full technical history stays in CHANGELOG.md). */
data class ReleaseNote(val version: String, val items: List<ReleaseNoteItem>)

/**
 * Release notes per version, newest first. Every new `app.version` adds an entry here (see CLAUDE.md);
 * a version without an entry simply shows no popup and no Settings button.
 */
object ReleaseNotes {
    val entries: List<ReleaseNote> = listOf(
        ReleaseNote(
            "1.19.0",
            listOf(ReleaseNoteItem(ReleaseNoteKind.New, Res.string.release_notes_1_19_0_new_share)),
        ),
        ReleaseNote(
            "1.18.0",
            listOf(
                ReleaseNoteItem(ReleaseNoteKind.New, Res.string.release_notes_1_18_0_new_popup),
                ReleaseNoteItem(ReleaseNoteKind.New, Res.string.release_notes_1_18_0_new_pending),
                ReleaseNoteItem(ReleaseNoteKind.Fixed, Res.string.release_notes_1_18_0_fixed_progress),
            ),
        ),
    )

    fun forVersion(version: String): ReleaseNote? = entries.firstOrNull { it.version == version }
}

/** "What’s new in X" popup; [onDismiss] runs for the button and for tapping outside. */
@Composable
fun ReleaseNotesDialog(note: ReleaseNote, onDismiss: () -> Unit) {
    val c = NgTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = NgShapes.panel,
        containerColor = c.panel,
        title = { Text(stringResource(Res.string.release_notes_title, note.version), style = NgTheme.type.h2) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                note.items.forEach { item ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // The label, not a colour, tells the kinds apart.
                        Text(stringResource(item.kind.label), style = NgTheme.type.monoSmall, color = c.text2)
                        Text(stringResource(item.text), style = NgTheme.type.body, modifier = Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton = { NgButton(stringResource(Res.string.release_notes_close), onDismiss, style = NgButtonStyle.Primary) },
    )
}
