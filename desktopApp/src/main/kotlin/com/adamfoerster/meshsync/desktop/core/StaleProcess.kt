package com.adamfoerster.meshsync.desktop.core

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * If the app was killed (crash, Task Manager) its core keeps running and holds Syncthing's database
 * lock, so the next start would fail. The core's PID is therefore kept in [pidFile] and the leftover
 * process is ended before a new one starts. Only a process whose executable name contains
 * [expectedName] is touched, because Windows reuses PIDs.
 */
object StaleProcess {
    fun write(pidFile: File, pid: Long) {
        pidFile.writeText(pid.toString())
    }

    fun clear(pidFile: File) {
        pidFile.delete()
    }

    /** Returns true if a leftover core was found and ended. */
    fun terminate(pidFile: File, expectedName: String = "syncthing"): Boolean {
        val pid = pidFile.takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull()
        pidFile.delete()
        val handle = pid?.let { ProcessHandle.of(it).orElse(null) } ?: return false
        val command = handle.info().command().orElse("")
        if (!command.contains(expectedName, ignoreCase = true)) return false
        handle.destroy()
        val exited = runCatching { handle.onExit().get(STOP_WAIT_SECONDS, TimeUnit.SECONDS) }.isSuccess
        if (!exited) handle.destroyForcibly()
        return true
    }

    private const val STOP_WAIT_SECONDS = 5L
}
