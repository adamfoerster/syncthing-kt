package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import java.io.File
import java.nio.file.Files
import kotlin.test.Test

class MacAutostartTest {
    private val dir: File = Files.createTempDirectory("launchagents").toFile().resolve("LaunchAgents")
    private val launcher = "/Applications/MeshSync.app/Contents/MacOS/MeshSync"
    private val autostart = MacAutostart(launcher, dir)
    private val plist = File(dir, "${MacAutostart.LABEL}.plist")

    @Test
    fun `enabling writes a LaunchAgent that starts the app minimized at login`() {
        autostart.setEnabled(true)

        val xml = plist.readText()
        assertThat(xml).contains("<string>$launcher</string>")
        assertThat(xml).contains("<string>--minimized</string>")
        assertThat(xml).contains("<key>RunAtLoad</key>")
        assertThat(autostart.isEnabled()).isTrue()
    }

    @Test
    fun `disabling removes the LaunchAgent`() {
        autostart.setEnabled(true)
        autostart.setEnabled(false)

        assertThat(plist.exists()).isFalse()
        assertThat(autostart.isEnabled()).isFalse()
    }

    @Test
    fun `special characters in the launcher path are escaped`() {
        assertThat(MacAutostart.plistXml("/Apps/A&B.app")).contains("/Apps/A&amp;B.app")
    }

    @Test
    fun `the OS picks the autostart, development runs get none`() {
        assertThat(Autostart.forCurrentOs(HostOs.MacOs, launcher)).isInstanceOf(MacAutostart::class)
        assertThat(Autostart.forCurrentOs(HostOs.Windows, "C:\\MeshSync.exe")).isInstanceOf(WindowsAutostart::class)
        assertThat(Autostart.forCurrentOs(HostOs.MacOs, null)).isSameInstanceAs(NoAutostart)
        assertThat(Autostart.forCurrentOs(HostOs.Other, "/usr/bin/meshsync")).isSameInstanceAs(NoAutostart)
    }
}
