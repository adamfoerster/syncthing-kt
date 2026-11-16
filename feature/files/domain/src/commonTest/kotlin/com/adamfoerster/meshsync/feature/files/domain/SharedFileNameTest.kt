package com.adamfoerster.meshsync.feature.files.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class SharedFileNameTest {
    @Test
    fun keepsAnOrdinaryName() = assertThat(sharedFileName("photo.jpg", "x")).isEqualTo("photo.jpg")

    @Test
    fun separatorsCannotEscapeTheTargetFolder() {
        assertThat(sharedFileName("../../etc/passwd", "x")).isEqualTo(".._.._etc_passwd")
        assertThat(sharedFileName("a\\b.txt", "x")).isEqualTo("a_b.txt")
    }

    @Test
    fun missingOrUnusableNamesFallBack() {
        assertThat(sharedFileName(null, "shared-file")).isEqualTo("shared-file")
        assertThat(sharedFileName("  ", "shared-file")).isEqualTo("shared-file")
        assertThat(sharedFileName("..", "shared-file")).isEqualTo("shared-file")
    }
}
