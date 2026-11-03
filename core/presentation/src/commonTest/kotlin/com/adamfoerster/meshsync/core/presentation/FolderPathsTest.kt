package com.adamfoerster.meshsync.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class FolderPathsTest {
    private val root = "/storage/emulated/0"

    @Test
    fun primaryVolumeMapsToTheInternalStorageRoot() {
        assertThat(documentIdToPath("primary:Documents/Fotos", root)).isEqualTo("/storage/emulated/0/Documents/Fotos")
        assertThat(documentIdToPath("primary:", root)).isEqualTo(root)
    }

    @Test
    fun removableVolumesMapToTheirStorageMount() {
        assertThat(documentIdToPath("1A2B-3C4D:Backup/", root)).isEqualTo("/storage/1A2B-3C4D/Backup")
        assertThat(documentIdToPath("1A2B-3C4D:", root)).isEqualTo("/storage/1A2B-3C4D")
    }

    @Test
    fun providersWithoutAFileSystemPathGiveNull() {
        assertThat(documentIdToPath("downloads", root)).isNull()
        assertThat(documentIdToPath("home:Notes", root)).isNull()
        assertThat(documentIdToPath(":x", root)).isNull()
    }
}
