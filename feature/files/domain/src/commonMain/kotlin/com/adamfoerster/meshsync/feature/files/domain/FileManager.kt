package com.adamfoerster.meshsync.feature.files.domain

import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result

/** One row of a directory listing. [path] is absolute. */
data class FileEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedMillis: Long?,
)

/**
 * Basic file operations on the device's storage, for the file manager. Paths are absolute.
 * Every operation reports failures as [DataError.Local] instead of throwing.
 */
interface FileManager {
    /** Directories first, then files, each group sorted by name ignoring case. Hidden files are included. */
    suspend fun list(directory: String): Result<List<FileEntry>, DataError.Local>

    /** Deletes files and folders (recursively). */
    suspend fun delete(paths: List<String>): EmptyResult<DataError.Local>

    /** Renames in place; [newName] is a bare name, never a path. */
    suspend fun rename(path: String, newName: String): EmptyResult<DataError.Local>

    /**
     * Copies or moves [paths] into [targetDirectory]. A name already taken there is kept: the newcomer
     * becomes `name (1).ext`, so pasting never overwrites anything.
     */
    suspend fun paste(paths: List<String>, targetDirectory: String, move: Boolean): EmptyResult<DataError.Local>

    suspend fun createDirectory(parent: String, name: String): EmptyResult<DataError.Local>
}

/** A name is valid when it is a single, real path segment. */
fun isValidFileName(name: String): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && trimmed != "." && trimmed != ".." && '/' !in trimmed && '\u0000' !in trimmed
}

/** `photo.jpg` taken -> `photo (1).jpg`, then `photo (2).jpg`; folders and dotfiles get the suffix at the end. */
fun uniqueName(name: String, isTaken: (String) -> Boolean): String {
    if (!isTaken(name)) return name
    val dot = name.lastIndexOf('.')
    val stem = if (dot > 0) name.substring(0, dot) else name
    val extension = if (dot > 0) name.substring(dot) else ""
    var n = 1
    while (true) {
        val candidate = "$stem ($n)$extension"
        if (!isTaken(candidate)) return candidate
        n++
    }
}

/** Parent directory of an absolute [path], or null at the root. */
fun parentOf(path: String): String? {
    val trimmed = path.trimEnd('/')
    val cut = trimmed.lastIndexOf('/')
    return if (cut <= 0) null else trimmed.substring(0, cut)
}

/** True when [path] is [ancestor] itself or lies inside it. */
fun isSameOrInside(path: String, ancestor: String): Boolean {
    val a = ancestor.trimEnd('/')
    val p = path.trimEnd('/')
    return p == a || p.startsWith("$a/")
}
