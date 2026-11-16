package com.adamfoerster.meshsync.desktop

import java.io.File

/** Where the desktop app keeps its files and finds the bundled Syncthing. */
object DesktopPaths {
    /**
     * Settings, the core's config/database and the instance lock: `%LOCALAPPDATA%\MeshSync` on Windows,
     * `~/Library/Application Support/MeshSync` on macOS and `~/.local/share/MeshSync` elsewhere.
     */
    fun dataDir(
        os: HostOs = HostOs.current(),
        env: (String) -> String? = System::getenv,
        home: File = File(System.getProperty("user.home")),
    ): File {
        val base = when (os) {
            HostOs.Windows -> env("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let(::File) ?: File(home, ".local/share")
            HostOs.MacOs -> File(home, "Library/Application Support")
            HostOs.Other -> File(home, ".local/share")
        }
        return File(base, "MeshSync")
    }

    /**
     * The Syncthing executable. Packaged builds and `gradle run` both set `compose.application.resources.dir`
     * to the OS folder of `desktopApp/resources` (see `:syncthing-native:buildWindows`/`buildMacos`); the
     * fallback covers running from an IDE.
     */
    fun syncthingBinary(
        os: HostOs = HostOs.current(),
        resourcesDir: String? = System.getProperty("compose.application.resources.dir"),
    ): File {
        val name = if (os == HostOs.Windows) "syncthing.exe" else "syncthing"
        val resources = resourcesDir?.let(::File)
            ?: File(if (os == HostOs.MacOs) "resources/macos-arm64" else "resources/windows-x64")
        return File(resources, name)
    }
}
