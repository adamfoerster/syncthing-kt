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
        assertThat(DesktopPlatformActions.revealCommand(listOf("C:\\docs\\a.txt"), HostOs.Windows))
            .isEqualTo(listOf("explorer.exe", "/select,$file"))
    }

    @Test
    fun `several files open their folder`() {
        val parent = File("C:\\docs\\a.txt").absoluteFile.parentFile.path
        assertThat(DesktopPlatformActions.revealCommand(listOf("C:\\docs\\a.txt", "C:\\docs\\b.txt"), HostOs.Windows))
            .isEqualTo(listOf("explorer.exe", parent))
    }

    @Test
    fun `nothing to share does nothing`() {
        assertThat(DesktopPlatformActions.revealCommand(emptyList(), HostOs.Windows)).isNull()
        assertThat(DesktopPlatformActions.revealCommand(emptyList(), HostOs.MacOs)).isNull()
    }

    @Test
    fun `one file is revealed in Finder`() {
        val file = File("/Users/u/docs/a.txt").absolutePath
        assertThat(DesktopPlatformActions.revealCommand(listOf("/Users/u/docs/a.txt"), HostOs.MacOs))
            .isEqualTo(listOf("open", "-R", file))
    }

    @Test
    fun `several files open their folder in Finder`() {
        val parent = File("/Users/u/docs/a.txt").absoluteFile.parentFile.path
        assertThat(DesktopPlatformActions.revealCommand(listOf("/Users/u/docs/a.txt", "/Users/u/docs/b.txt"), HostOs.MacOs))
            .isEqualTo(listOf("open", parent))
    }

    @Test
    fun `the desktop never asks for storage access`() {
        val actions = DesktopPlatformActions()
        assertThat(actions.hasStorageAccess()).isTrue()
    }
}
