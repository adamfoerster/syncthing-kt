package com.adamfoerster.meshsync.feature.files.data

import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.files.domain.FileEntry
import com.adamfoerster.meshsync.feature.files.domain.FileManager
import com.adamfoerster.meshsync.feature.files.domain.isSameOrInside
import com.adamfoerster.meshsync.feature.files.domain.isValidFileName
import com.adamfoerster.meshsync.feature.files.domain.uniqueName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use

/**
 * [FileManager] over an Okio [FileSystem], so tests run on a fake one. Copy streams file contents;
 * move uses a rename and falls back to copy-then-delete (e.g. across volumes).
 */
class OkioFileManager(
    private val fileSystem: FileSystem,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : FileManager {

    override suspend fun list(directory: String): Result<List<FileEntry>, DataError.Local> = fileCall {
        val entries = fileSystem.list(directory.toPath()).mapNotNull { child ->
            val meta = runCatching { fileSystem.metadataOrNull(child) }.getOrNull() ?: return@mapNotNull null
            FileEntry(
                path = child.toString(),
                name = child.name,
                isDirectory = meta.isDirectory,
                sizeBytes = if (meta.isDirectory) 0 else meta.size ?: 0,
                modifiedMillis = meta.lastModifiedAtMillis,
            )
        }
        entries.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    override suspend fun delete(paths: List<String>): EmptyResult<DataError.Local> = fileCall {
        paths.forEach { fileSystem.deleteRecursively(it.toPath(), mustExist = false) }
    }

    override suspend fun rename(path: String, newName: String): EmptyResult<DataError.Local> = fileCall {
        val name = newName.trim()
        if (!isValidFileName(name)) throw InvalidName()
        val source = path.toPath()
        val parent = source.parent ?: throw InvalidName()
        val target = parent / name
        if (target == source) return@fileCall
        if (fileSystem.exists(target)) throw AlreadyExists()
        fileSystem.atomicMove(source, target)
    }

    override suspend fun paste(paths: List<String>, targetDirectory: String, move: Boolean): EmptyResult<DataError.Local> = fileCall {
        val target = targetDirectory.toPath()
        // Checked up front so a bad item cannot leave half of the selection transferred.
        paths.forEach { source ->
            if (isSameOrInside(targetDirectory, source) && fileSystem.metadataOrNull(source.toPath())?.isDirectory == true) throw IntoItself()
        }
        paths.forEach { source ->
            val from = source.toPath()
            // Moving an item into the folder it is already in changes nothing.
            if (move && from.parent == target) return@forEach
            val destination = target / uniqueName(from.name) { fileSystem.exists(target / it) }
            if (move) moveTo(from, destination) else copyTo(from, destination)
        }
    }

    override suspend fun createDirectory(parent: String, name: String): EmptyResult<DataError.Local> = fileCall {
        val clean = name.trim()
        if (!isValidFileName(clean)) throw InvalidName()
        val dir = parent.toPath() / clean
        if (fileSystem.exists(dir)) throw AlreadyExists()
        fileSystem.createDirectory(dir, mustCreate = true)
    }

    private fun moveTo(from: Path, to: Path) {
        try {
            fileSystem.atomicMove(from, to)
        } catch (_: IOException) {
            copyTo(from, to)
            fileSystem.deleteRecursively(from)
        }
    }

    private fun copyTo(from: Path, to: Path) {
        if (fileSystem.metadata(from).isDirectory) {
            fileSystem.createDirectory(to, mustCreate = true)
            fileSystem.list(from).forEach { copyTo(it, to / it.name) }
        } else {
            fileSystem.source(from).use { source -> fileSystem.sink(to, mustCreate = true).buffer().use { it.writeAll(source) } }
        }
    }

    private class InvalidName : IOException("invalid name")
    private class AlreadyExists : IOException("already exists")
    private class IntoItself : IOException("into itself")

    private suspend fun <T> fileCall(block: () -> T): Result<T, DataError.Local> = withContext(io) {
        try {
            Result.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (_: InvalidName) {
            Result.Failure(DataError.Local.INVALID_NAME)
        } catch (_: AlreadyExists) {
            Result.Failure(DataError.Local.ALREADY_EXISTS)
        } catch (_: IntoItself) {
            Result.Failure(DataError.Local.INTO_ITSELF)
        } catch (_: FileNotFoundException) {
            Result.Failure(DataError.Local.NOT_FOUND)
        } catch (e: IOException) {
            // Android reports a denied storage permission as an IOException with this text.
            val message = e.message.orEmpty()
            val denied = message.contains("permission denied", ignoreCase = true) || message.contains("EACCES")
            Result.Failure(if (denied) DataError.Local.PERMISSION_DENIED else DataError.Local.IO)
        } catch (_: Exception) {
            Result.Failure(DataError.Local.UNKNOWN)
        }
    }
}
