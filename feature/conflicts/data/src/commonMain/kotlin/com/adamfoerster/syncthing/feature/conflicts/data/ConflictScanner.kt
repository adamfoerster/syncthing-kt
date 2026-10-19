package com.adamfoerster.syncthing.feature.conflicts.data

import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictFileName
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath

/** A conflict copy found on disk, before authors and contents are looked up. */
data class FoundConflict(
    val folder: Folder,
    val root: Path,
    /** Both relative to [root], `/`-separated. */
    val originalPath: String,
    val copyPath: String,
    val copyName: ConflictFileName,
)

/** Walks folder trees looking for `.sync-conflict-` copies whose original still exists. */
class ConflictScanner(private val fileSystem: FileSystem) {

    fun scan(folders: List<Folder>): List<FoundConflict> = folders.flatMap { folder ->
        val root = folder.path.takeIf { it.isNotBlank() && !it.startsWith("~") }?.toPath() ?: return@flatMap emptyList()
        if (!fileSystem.exists(root)) return@flatMap emptyList()
        val found = ArrayList<FoundConflict>()
        walk(root, root, folder, found)
        found
    }

    private fun walk(root: Path, dir: Path, folder: Folder, out: MutableList<FoundConflict>) {
        val children = try {
            fileSystem.list(dir)
        } catch (e: IOException) {
            return // unreadable directory: skip it rather than failing the whole scan
        }
        for (child in children) {
            val name = child.name
            // Syncthing's own bookkeeping (.stfolder, .stversions, .stignore) is never user data.
            if (name.startsWith(".st")) continue
            val metadata = try {
                fileSystem.metadataOrNull(child)
            } catch (e: IOException) {
                null
            } ?: continue
            if (metadata.symlinkTarget != null) continue
            when {
                metadata.isDirectory -> walk(root, child, folder, out)
                metadata.isRegularFile -> {
                    val parsed = ConflictFileName.parse(name) ?: continue
                    val original = dir / parsed.originalName
                    if (!fileSystem.exists(original)) continue
                    out += FoundConflict(
                        folder = folder,
                        root = root,
                        originalPath = original.relativeTo(root).toSlashString(),
                        copyPath = child.relativeTo(root).toSlashString(),
                        copyName = parsed,
                    )
                }
            }
        }
    }
}

internal fun Path.toSlashString(): String = segments.joinToString("/")

internal fun Path.resolveSlashPath(relative: String): Path =
    relative.split('/').filter { it.isNotEmpty() }.fold(this) { acc, segment -> acc / segment }
