package com.adamfoerster.syncthing.feature.conflicts.data

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.Path
import kotlin.time.Clock

/**
 * Puts replaced versions where Syncthing's own file versioning keeps them:
 * `<folder>/.stversions/<dir>/<name>~<YYYYMMDD-HHMMSS><ext>`. That directory is internal to
 * Syncthing (never synced), and the naming lets its "restore version" tools find them.
 */
class VersionArchive(
    private val fileSystem: FileSystem,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) {
    /** Moves [file] (inside [root]) into the archive and returns where it went. */
    fun move(root: Path, file: Path): Path {
        val target = targetFor(root, file)
        fileSystem.atomicMove(file, target)
        return target
    }

    /** Copies [file] into the archive, leaving it in place (it is about to be overwritten). */
    fun copy(root: Path, file: Path): Path {
        val target = targetFor(root, file)
        fileSystem.copy(file, target)
        return target
    }

    private fun targetFor(root: Path, file: Path): Path {
        val relativeDir = file.parent?.relativeTo(root)
        var dir = root / VERSIONS_DIR
        relativeDir?.segments?.filter { it.isNotEmpty() && it != "." }?.forEach { dir = dir / it }
        fileSystem.createDirectories(dir)
        val (base, ext) = file.name.splitExtension()
        val stamp = clock.now().toLocalDateTime(timeZone).stamp()
        var candidate = dir / "$base~$stamp$ext"
        var n = 1
        while (fileSystem.exists(candidate)) candidate = dir / "$base~$stamp-${n++}$ext"
        return candidate
    }

    private fun LocalDateTime.stamp(): String {
        fun Int.two() = toString().padStart(2, '0')
        return "$year${month.ordinal.plus(1).two()}${day.two()}-${hour.two()}${minute.two()}${second.two()}"
    }

    companion object {
        const val VERSIONS_DIR = ".stversions"
    }
}

/** `convite.png` → (`convite`, `.png`); dotfiles keep their name as base. */
internal fun String.splitExtension(): Pair<String, String> {
    val dot = lastIndexOf('.')
    return if (dot <= 0) this to "" else substring(0, dot) to substring(dot)
}
