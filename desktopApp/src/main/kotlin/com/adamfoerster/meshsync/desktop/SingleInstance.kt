package com.adamfoerster.meshsync.desktop

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileLock
import kotlin.concurrent.thread

/**
 * Keeps a single MeshSync running per user. The first instance holds a file lock and listens on a
 * loopback port (written next to the lock); a second launch connects to that port, which makes the
 * first one call [onShowRequested] (bring the window up), and then exits.
 */
class SingleInstance(private val dir: File, private val onShowRequested: () -> Unit) : AutoCloseable {
    private var lockFile: RandomAccessFile? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null

    /** True if this process is now the primary instance; false if another one is running (and was told to show). */
    fun acquire(): Boolean {
        dir.mkdirs()
        val file = RandomAccessFile(File(dir, "instance.lock"), "rw")
        val acquired = try {
            file.channel.tryLock()
        } catch (e: java.nio.channels.OverlappingFileLockException) {
            null
        }
        if (acquired == null) {
            file.close()
            signalExisting()
            return false
        }
        lockFile = file
        lock = acquired
        val socket = ServerSocket(0, 8, InetAddress.getLoopbackAddress())
        server = socket
        File(dir, "instance.port").writeText(socket.localPort.toString())
        thread(isDaemon = true, name = "single-instance") {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { onShowRequested() }
                } catch (e: java.io.IOException) {
                    // Closed on shutdown.
                }
            }
        }
        return true
    }

    private fun signalExisting() {
        val port = File(dir, "instance.port").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: return
        runCatching { Socket(InetAddress.getLoopbackAddress(), port).close() }
    }

    override fun close() {
        runCatching { server?.close() }
        runCatching { lock?.release() }
        runCatching { lockFile?.close() }
        File(dir, "instance.port").delete()
    }
}
