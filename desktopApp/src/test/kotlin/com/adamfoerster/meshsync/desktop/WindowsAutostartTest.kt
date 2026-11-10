package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class WindowsAutostartTest {
    private val calls = mutableListOf<List<String>>()
    private var exitCode = 0
    private val autostart = WindowsAutostart("""C:\Users\u\AppData\Local\MeshSync\MeshSync.exe""") { args ->
        calls += args
        exitCode
    }

    @Test
    fun `enabling writes the quoted launcher with the minimized flag`() {
        autostart.setEnabled(true)

        val args = calls.single()
        assertThat(args.first()).isEqualTo("add")
        assertThat(args).contains("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run")
        assertThat(args).contains("\"C:\\Users\\u\\AppData\\Local\\MeshSync\\MeshSync.exe\" --minimized")
        assertThat(args).contains("/f")
    }

    @Test
    fun `disabling deletes the value`() {
        autostart.setEnabled(false)
        assertThat(calls.single().first()).isEqualTo("delete")
    }

    @Test
    fun `is enabled when the value exists`() {
        exitCode = 0
        assertThat(autostart.isEnabled()).isTrue()
        exitCode = 1
        assertThat(autostart.isEnabled()).isFalse()
    }

    @Test
    fun `a development run through java is not registered`() {
        // Under Gradle the current process is java.exe; registering that would break the login start.
        val launcher = WindowsAutostart.currentLauncher()
        assertThat(launcher == null || !launcher.substringAfterLast('\\').startsWith("java", ignoreCase = true)).isTrue()
    }
}
