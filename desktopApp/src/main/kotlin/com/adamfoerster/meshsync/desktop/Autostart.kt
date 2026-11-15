package com.adamfoerster.meshsync.desktop

import java.io.File

/** Starting the app when the user signs in. One implementation per OS (Windows registry, macOS LaunchAgent). */
interface Autostart {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)

    companion object {
        /** Passed by the login start, so the app only shows its tray/menu bar icon. */
        const val MINIMIZED_FLAG = "--minimized"

        /**
         * The packaged launcher (`MeshSync.exe`, `MeshSync.app/Contents/MacOS/MeshSync`), or null when running
         * from Gradle/an IDE, where the process is `java` and registering it would start the wrong thing at login.
         */
        fun currentLauncher(): String? = ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { !it.substringAfterLast('\\').substringAfterLast('/').startsWith("java", ignoreCase = true) }

        /** The autostart for [os], or [NoAutostart] for development runs and unsupported systems. */
        fun forCurrentOs(os: HostOs = HostOs.current(), launcher: String? = currentLauncher()): Autostart {
            if (launcher == null) return NoAutostart
            return when (os) {
                HostOs.Windows -> WindowsAutostart(launcher)
                HostOs.MacOs -> MacAutostart(launcher)
                HostOs.Other -> NoAutostart
            }
        }
    }
}

/** Runs `reg.exe` with [args]; returns the exit code. A seam so tests don't touch the real registry. */
fun interface RegistryRunner {
    fun run(args: List<String>): Int
}

/**
 * Autostart through `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`: per user, no admin rights.
 * [launchCommand] is the quoted executable; the app is started with [Autostart.MINIMIZED_FLAG].
 */
class WindowsAutostart(
    private val launchCommand: String,
    private val registry: RegistryRunner = RegistryRunner { args ->
        ProcessBuilder(listOf("reg") + args).redirectErrorStream(true).start().also { it.inputStream.readBytes() }.waitFor()
    },
) : Autostart {
    override fun isEnabled(): Boolean = registry.run(listOf("query", RUN_KEY, "/v", VALUE_NAME)) == 0

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            registry.run(listOf("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"$launchCommand\" ${Autostart.MINIMIZED_FLAG}", "/f"))
        } else {
            registry.run(listOf("delete", RUN_KEY, "/v", VALUE_NAME, "/f"))
        }
    }

    private companion object {
        const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        const val VALUE_NAME = "MeshSync"
    }
}

/**
 * Autostart through a per-user LaunchAgent (`~/Library/LaunchAgents`): launchd reads it at the next login and
 * runs [launcher] with [Autostart.MINIMIZED_FLAG]. No admin rights, and deleting the file turns it off.
 */
class MacAutostart(
    private val launcher: String,
    private val launchAgentsDir: File = File(System.getProperty("user.home"), "Library/LaunchAgents"),
) : Autostart {
    private val plist: File get() = File(launchAgentsDir, "$LABEL.plist")

    override fun isEnabled(): Boolean = plist.exists()

    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            launchAgentsDir.mkdirs()
            plist.writeText(plistXml(launcher))
        } else {
            plist.delete()
        }
    }

    companion object {
        const val LABEL = "com.adamfoerster.meshsync"

        fun plistXml(launcher: String): String = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            |<plist version="1.0">
            |<dict>
            |    <key>Label</key>
            |    <string>$LABEL</string>
            |    <key>ProgramArguments</key>
            |    <array>
            |        <string>${launcher.escapeXml()}</string>
            |        <string>${Autostart.MINIMIZED_FLAG}</string>
            |    </array>
            |    <key>RunAtLoad</key>
            |    <true/>
            |    <key>ProcessType</key>
            |    <string>Interactive</string>
            |</dict>
            |</plist>
            |
        """.trimMargin()

        private fun String.escapeXml(): String =
            replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }
}

/** Used where autostart makes no sense (development runs). */
object NoAutostart : Autostart {
    override fun isEnabled(): Boolean = false
    override fun setEnabled(enabled: Boolean) = Unit
}
