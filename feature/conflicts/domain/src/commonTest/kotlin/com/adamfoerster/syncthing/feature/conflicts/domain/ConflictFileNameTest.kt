package com.adamfoerster.syncthing.feature.conflicts.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictFileName
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ContentSniffer
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import kotlin.test.Test

class ConflictFileNameTest {

    @Test
    fun parsesNameWithExtension() {
        val parsed = ConflictFileName.parse("lista-de-compras.sync-conflict-20261019-143500-ABCDEF2.txt")
        assertThat(parsed).isEqualTo(ConflictFileName("lista-de-compras.txt", "20261019-143500", "ABCDEF2"))
    }

    @Test
    fun parsesNameWithoutExtension() {
        assertThat(ConflictFileName.parse("Makefile.sync-conflict-20261019-143500-ABCDEF2")?.originalName)
            .isEqualTo("Makefile")
    }

    @Test
    fun keepsInnerExtensionsInTheBase() {
        // Syncthing only moves the last extension past the marker.
        assertThat(ConflictFileName.parse("backup.tar.sync-conflict-20261019-143500-ABCDEF2.gz")?.originalName)
            .isEqualTo("backup.tar.gz")
    }

    @Test
    fun rejectsOrdinaryFiles() {
        assertThat(ConflictFileName.parse("notes.txt")).isNull()
        assertThat(ConflictFileName.parse(".sync-conflict-20261019-143500-ABCDEF2.txt")).isNull()
        assertThat(ConflictFileName.parse("a.sync-conflict-2026-143500-ABCDEF2.txt")).isNull()
    }

    @Test
    fun detectsTextAndBinary() {
        assertThat(ContentSniffer.looksLikeText("Olá, mundo — ✓".encodeToByteArray())).isTrue()
        assertThat(ContentSniffer.looksLikeText(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00))).isFalse()
        assertThat(ContentSniffer.looksLikeText(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x41))).isFalse()
        // A multi-byte character cut by the sample boundary is still text.
        val cut = "ação".encodeToByteArray().let { it.copyOf(it.size - 1) }
        assertThat(ContentSniffer.looksLikeText(cut)).isTrue()
    }

    @Test
    fun imagesAreRecognizedByExtension() {
        val kind = ContentSniffer.kindOf("convite.PNG", listOf(byteArrayOf(0)), listOf(10L))
        assertThat(kind).isEqualTo(ConflictKind.IMAGE)
    }

    @Test
    fun largeTextIsTreatedAsBinary() {
        val kind = ContentSniffer.kindOf(
            "dump.sql",
            listOf("text".encodeToByteArray()),
            listOf(ContentSniffer.MAX_TEXT_BYTES + 1),
        )
        assertThat(kind).isEqualTo(ConflictKind.BINARY)
    }
}
