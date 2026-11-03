package com.adamfoerster.meshsync.feature.files.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class FileNamesTest {
    @Test
    fun aNameMustBeASingleRealSegment() {
        assertThat(isValidFileName("notes.txt")).isTrue()
        assertThat(isValidFileName("  ")).isFalse()
        assertThat(isValidFileName("..")).isFalse()
        assertThat(isValidFileName("a/b")).isFalse()
    }

    @Test
    fun aTakenNameGetsTheNextFreeSuffixBeforeTheExtension() {
        val taken = setOf("photo.jpg", "photo (1).jpg")
        assertThat(uniqueName("photo.jpg") { it in taken }).isEqualTo("photo (2).jpg")
        assertThat(uniqueName("new.txt") { it in taken }).isEqualTo("new.txt")
    }

    @Test
    fun foldersAndDotfilesGetTheSuffixAtTheEnd() {
        assertThat(uniqueName("Docs") { it == "Docs" }).isEqualTo("Docs (1)")
        assertThat(uniqueName(".nomedia") { it == ".nomedia" }).isEqualTo(".nomedia (1)")
    }

    @Test
    fun parentAndContainment() {
        assertThat(parentOf("/storage/emulated/0/Docs/")).isEqualTo("/storage/emulated/0")
        assertThat(parentOf("/storage")).isNull()
        assertThat(isSameOrInside("/a/b/c", "/a/b")).isTrue()
        assertThat(isSameOrInside("/a/bc", "/a/b")).isFalse()
        assertThat(isSameOrInside("/a/b", "/a/b/")).isTrue()
    }
}
