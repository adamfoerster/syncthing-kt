package com.adamfoerster.meshsync.core.presentation

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

class ReleaseNotesTest {

    @Test
    fun everyEntryHasAtLeastOneItemAndAUniqueVersion() {
        val versions = ReleaseNotes.entries.map { it.version }
        assertThat(versions.distinct()).isEqualTo(versions)
        assertThat(ReleaseNotes.entries.filter { it.items.isEmpty() }.map { it.version }).isEmpty()
    }

    @Test
    fun looksUpNotesByExactVersion() {
        val newest = ReleaseNotes.entries.first()
        assertThat(ReleaseNotes.forVersion(newest.version)).isNotNull()
        assertThat(ReleaseNotes.forVersion("0.0.0")).isNull()
    }
}
