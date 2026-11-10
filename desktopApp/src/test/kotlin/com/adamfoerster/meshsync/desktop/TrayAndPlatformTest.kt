package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.model.CoreState
import java.io.File
import kotlin.test.Test

class TrayAndPlatformTest {
    @Test
    fun `core states map to tray statuses`() {
        assertThat(TrayStatus.of(CoreState.Running)).isEqualTo(TrayStatus.Running)
        assertThat(TrayStatus.of(CoreState.Starting)).isEqualTo(TrayStatus.Starting)
        assertThat(TrayStatus.of(CoreState.Stopped)).isEqualTo(TrayStatus.Paused)
        assertThat(TrayStatus.of(CoreState.BinaryMissing)).isEqualTo(TrayStatus.Missing)
        assertThat(TrayStatus.of(CoreState.Failed("boom"))).isEqualTo(TrayStatus.Failed)
    }

    @Test
    fun `pause is offered only while the core is on`() {
        assertThat(TrayStatus.Running.canPause).isTrue()
        assertThat(TrayStatus.Starting.canPause).isTrue()
        assertThat(TrayStatus.Paused.canPause).isFalse()
        assertThat(TrayStatus.Failed.canPause).isFalse()
    }

    @Test
    fun `one file is selected in Explorer`() {
        val file = File("C:\\docs\\a.txt").absolutePath
        assertThat(DesktopPlatformActions.explorerCommand(listOf("C:\\docs\\a.txt")))
            .isEqualTo(listOf("explorer.exe", "/select,$file"))
    }

    @Test
    fun `several files open their folder`() {
        val parent = File("C:\\docs\\a.txt").absoluteFile.parentFile.path
        assertThat(DesktopPlatformActions.explorerCommand(listOf("C:\\docs\\a.txt", "C:\\docs\\b.txt")))
            .isEqualTo(listOf("explorer.exe", parent))
    }

    @Test
    fun `nothing to share does nothing`() {
        assertThat(DesktopPlatformActions.explorerCommand(emptyList())).isNull()
    }

    @Test
    fun `the desktop never asks for storage access`() {
        val actions = DesktopPlatformActions()
        assertThat(actions.hasStorageAccess()).isTrue()
    }
}
