package com.adamfoerster.meshsync.core.domain.model

/**
 * An entry that a remote device still has to receive (or delete) to match this device's version of a folder.
 * [path] is relative to the folder root.
 */
data class PendingFile(
    val path: String,
    val size: Long,
    val isDirectory: Boolean,
    /** The entry was deleted here and the remote device has yet to remove it. */
    val isDeletion: Boolean,
)
