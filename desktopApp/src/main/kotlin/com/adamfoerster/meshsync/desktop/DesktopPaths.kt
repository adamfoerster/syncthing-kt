package com.adamfoerster.meshsync.desktop

import java.io.File

/** Where the desktop app keeps its files and finds the bundled Syncthing. */
object DesktopPaths {
    /** `%LOCALAPPDATA%\MeshSync`: settings, the core's config/database and the instance lock. */
    fun dataDir(): File {
        val base = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(System.getProperty("user.home"), ".local/share")
        return File(base, "MeshSync")
    }

    /**
     * The Syncthing executable. Packaged builds and `gradle run` both set `compose.application.resources.dir`
     * to the OS folder of `desktopApp/resources` (see `:syncthing-native:buildWindows`); the fallback covers
     * running from an IDE.
     */
    fun syncthingBinary(): File {
        val name = if (System.getProperty("os.name").lowercase().contains("windows")) "syncthing.exe" else "syncthing"
        val resources = System.getProperty("compose.application.resources.dir")?.let(::File)
            ?: File("resources/windows-x64")
        return File(resources, name)
    }
}
