package com.adamfoerster.syncthing.feature.conflicts.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeDocument
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.TextDifference
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.TextDifference.FINAL_NEWLINE
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.TextDifference.LINE_ENDINGS
import kotlin.test.Test

class TextDifferenceTest {

    @Test
    fun equalTextsHaveNoDifference() {
        assertThat(TextDifference.between("a\nb\n", "a\nb\n")).isEmpty()
    }

    @Test
    fun detectsLineEndings() {
        assertThat(TextDifference.between("a\r\nb\r\n", "a\nb\n").toList()).containsExactly(LINE_ENDINGS)
    }

    @Test
    fun detectsMissingFinalNewline() {
        assertThat(TextDifference.between("a\nb\n", "a\nb").toList()).containsExactly(FINAL_NEWLINE)
    }

    @Test
    fun reportsSeveralDifferencesAtOnce() {
        assertThat(TextDifference.between("a\r\nb\r\n", "a\nb").toList()).containsExactly(LINE_ENDINGS, FINAL_NEWLINE)
    }

    @Test
    fun theseDifferencesAreInvisibleToTheLineDiff() {
        val hunks = MergeDocument.of("a\r\nb\r\n", "a\nb").hunks
        assertThat(hunks).isEmpty()
    }
}
