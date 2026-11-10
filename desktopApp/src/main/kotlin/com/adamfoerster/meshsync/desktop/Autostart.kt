package com.adamfoerster.meshsync.desktop

/** Starting the app when the user signs in. One implementation per OS (Windows registry now, macOS later). */
interface Autostart {
    fun isEnabled(): Boolean
    fun setEnabled(enabled: Boolean)
}

/** Runs `reg.exe` with [args]; returns the exit code. A seam so tests don't touch the real registry. */
fun interface RegistryRunner {
    fun run(args: List<String>): Int
}

/**
 * Autostart through `HKCU\Software\Microsoft\Windows\CurrentVersion\Run`: per user, no admin rights.
 * [launchCommand] is the quoted executable; the app is started with [MINIMIZED_FLAG] so it only shows
 * the tray icon.
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
            registry.run(listOf("add", RUN_KEY, "/v", VALUE_NAME, "/t", "REG_SZ", "/d", "\"$launchCommand\" $MINIMIZED_FLAG", "/f"))
        } else {
            registry.run(listOf("delete", RUN_KEY, "/v", VALUE_NAME, "/f"))
        }
    }

    companion object {
        const val MINIMIZED_FLAG = "--minimized"
        private const val RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
        private const val VALUE_NAME = "MeshSync"

        /**
         * The packaged launcher (`MeshSync.exe`), or null when running from Gradle/an IDE, where the
         * process is `java.exe` and registering it would start the wrong thing at login.
         */
        fun currentLauncher(): String? = ProcessHandle.current().info().command().orElse(null)
            ?.takeIf { !it.substringAfterLast('\\').substringAfterLast('/').startsWith("java", ignoreCase = true) }
    }
}

/** Used where autostart makes no sense (development runs). */
object NoAutostart : Autostart {
    override fun isEnabled(): Boolean = false
    override fun setEnabled(enabled: Boolean) = Unit
}
