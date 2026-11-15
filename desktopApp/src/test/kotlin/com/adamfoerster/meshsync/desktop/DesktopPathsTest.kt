package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlin.test.Test

class DesktopPathsTest {
    private val home = File("/home/u")

    @Test
    fun `os names are recognised`() {
        assertThat(HostOs.of("Windows 11")).isEqualTo(HostOs.Windows)
        assertThat(HostOs.of("Mac OS X")).isEqualTo(HostOs.MacOs)
        assertThat(HostOs.of("Linux")).isEqualTo(HostOs.Other)
    }

    @Test
    fun `windows keeps data under LOCALAPPDATA`() {
        val dir = DesktopPaths.dataDir(HostOs.Windows, { if (it == "LOCALAPPDATA") "C:\\Users\\u\\AppData\\Local" else null }, home)
        assertThat(dir).isEqualTo(File("C:\\Users\\u\\AppData\\Local", "MeshSync"))
    }

    @Test
    fun `macOS keeps data under Application Support`() {
        val dir = DesktopPaths.dataDir(HostOs.MacOs, { "ignored" }, home)
        assertThat(dir).isEqualTo(File(home, "Library/Application Support/MeshSync"))
    }

    @Test
    fun `the binary name and fallback folder follow the OS`() {
        assertThat(DesktopPaths.syncthingBinary(HostOs.Windows, null)).isEqualTo(File("resources/windows-x64/syncthing.exe"))
        assertThat(DesktopPaths.syncthingBinary(HostOs.MacOs, null)).isEqualTo(File("resources/macos-arm64/syncthing"))
        assertThat(DesktopPaths.syncthingBinary(HostOs.MacOs, "/App/res")).isEqualTo(File("/App/res/syncthing"))
    }
}
