package com.adamfoerster.syncthing.feature.conflicts.data

import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.model.deviceRef
import com.adamfoerster.syncthing.core.domain.repository.ConflictSummary
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.core.domain.util.getOrNull
import com.adamfoerster.syncthing.feature.conflicts.domain.BinaryChoice
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictList
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.TextVersions
import com.adamfoerster.syncthing.feature.conflicts.domain.merge.MergeDocument
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictVersion
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ContentSniffer
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import kotlin.time.Instant

/**
 * Finds conflicts by walking the shared folders on this device and resolves them with plain file
 * operations; the core picks the result up with a targeted rescan and syncs it everywhere.
 */
@OptIn(FlowPreview::class)
class FileSystemConflictRepository(
    private val fileSystem: FileSystem,
    private val syncthing: SyncthingRepository,
    scope: CoroutineScope,
    private val archive: VersionArchive = VersionArchive(fileSystem),
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ConflictRepository, ConflictSummary {

    private val scanner = ConflictScanner(fileSystem)
    private val scanLock = Mutex()
    private val _conflicts = MutableStateFlow(ConflictList())
    override val conflicts: StateFlow<ConflictList> = _conflicts.asStateFlow()

    override val countsByFolder: Flow<Map<String, Int>> =
        conflicts.map { list -> list.items.groupingBy { it.folderId }.eachCount() }.distinctUntilChanged()

    init {
        // Rescan whenever a folder's local index moves (new files, finished syncs) or the folder set changes.
        scope.launch {
            syncthing.overview
                .filterNotNull()
                .map { overview -> overview.folders.map { Triple(it.id, it.path, it.status.sequence) } }
                .distinctUntilChanged()
                .debounce(RESCAN_DEBOUNCE_MS)
                .collect { rescan() }
        }
    }

    override suspend fun rescan() {
        val overview = syncthing.overview.value ?: return
        scanLock.withLock {
            _conflicts.update { it.copy(isScanning = true) }
            val items = try {
                withContext(io) { scanner.scan(overview.folders) }.mapNotNull { toConflict(it, overview) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _conflicts.value.items
            }
            _conflicts.value = ConflictList(
                items = items.sortedWith(compareBy({ it.folderLabel.lowercase() }, { it.path.lowercase() })),
                isScanning = false,
                isLoaded = true,
            )
        }
    }

    private suspend fun toConflict(found: FoundConflict, overview: SyncOverview): Conflict? {
        val currentFile = found.root.resolveSlashPath(found.originalPath)
        val copyFile = found.root.resolveSlashPath(found.copyPath)
        val disk = withContext(io) { inspect(found.originalPath, currentFile, copyFile) } ?: return null
        // The core's index knows who made the current version; the copy's name says who made it.
        val currentAuthor = syncthing.fileInfo(found.folder.id, found.originalPath).getOrNull()?.modifiedBy
        return Conflict(
            folderId = found.folder.id,
            folderLabel = found.folder.label,
            folderRoot = found.root.toString(),
            path = found.originalPath,
            kind = disk.kind,
            current = ConflictVersion(
                side = VersionSide.CURRENT,
                path = found.originalPath,
                size = disk.current.size ?: 0,
                modified = disk.current.lastModifiedAtMillis?.let(Instant::fromEpochMilliseconds),
                author = overview.deviceRef(currentAuthor),
            ),
            copy = ConflictVersion(
                side = VersionSide.COPY,
                path = found.copyPath,
                size = disk.copy.size ?: 0,
                modified = disk.copy.lastModifiedAtMillis?.let(Instant::fromEpochMilliseconds),
                author = overview.deviceRef(found.copyName.shortDeviceId),
            ),
            hunkCount = disk.hunkCount,
        )
    }

    private class DiskState(
        val current: FileMetadata,
        val copy: FileMetadata,
        val kind: ConflictKind,
        val hunkCount: Int?,
    )

    private fun inspect(originalPath: String, currentFile: Path, copyFile: Path): DiskState? = try {
        val currentMeta = fileSystem.metadataOrNull(currentFile)
        val copyMeta = fileSystem.metadataOrNull(copyFile)
        if (currentMeta == null || copyMeta == null) {
            null
        } else {
            val kind = ContentSniffer.kindOf(
                fileName = originalPath.substringAfterLast('/'),
                samples = listOf(sample(currentFile), sample(copyFile)),
                sizes = listOf(currentMeta.size ?: 0, copyMeta.size ?: 0),
            )
            val hunks = if (kind == ConflictKind.TEXT) {
                MergeDocument.of(readText(currentFile), readText(copyFile)).hunks.size
            } else {
                null
            }
            DiskState(currentMeta, copyMeta, kind, hunks)
        }
    } catch (e: IOException) {
        null
    }

    override suspend fun loadText(conflict: Conflict): Result<TextVersions, DataError.Local> = fileCall {
        val current = readText(conflict.file(VersionSide.CURRENT))
        val copy = readText(conflict.file(VersionSide.COPY))
        TextVersions(current, copy, fingerprint(conflict, current, copy))
    }

    override suspend fun loadBytes(conflict: Conflict, side: VersionSide): Result<ByteArray, DataError.Local> =
        fileCall { fileSystem.read(conflict.file(side)) { readByteArray() } }

    override suspend fun resolveText(conflict: Conflict, loaded: TextVersions, merged: String): EmptyResult<DataError> {
        val root = conflict.root()
        val currentFile = conflict.file(VersionSide.CURRENT)
        val copyFile = conflict.file(VersionSide.COPY)
        val result = fileCall {
            val onDisk = fingerprint(conflict, readText(currentFile), readText(copyFile))
            if (onDisk != loaded.fingerprint) throw FileChangedException()
            archive.copy(root, currentFile)
            writeAtomically(currentFile, merged)
            archive.move(root, copyFile)
        }
        return result.afterResolve(conflict) { }
    }

    override suspend fun resolveBinary(
        conflict: Conflict,
        choice: BinaryChoice,
        copySuffix: String,
    ): Result<String, DataError> {
        val root = conflict.root()
        val currentFile = conflict.file(VersionSide.CURRENT)
        val copyFile = conflict.file(VersionSide.COPY)
        val result = fileCall {
            when (choice) {
                BinaryChoice.KEEP_CURRENT -> {
                    archive.move(root, copyFile)
                    currentFile.name
                }
                BinaryChoice.KEEP_COPY -> {
                    archive.move(root, currentFile)
                    fileSystem.atomicMove(copyFile, currentFile)
                    currentFile.name
                }
                BinaryChoice.KEEP_BOTH -> {
                    val target = uniqueSibling(currentFile, copySuffix)
                    fileSystem.atomicMove(copyFile, target)
                    target.name
                }
            }
        }
        return result.afterResolve(conflict) { it }
    }

    private suspend fun <T, R> Result<T, DataError.Local>.afterResolve(
        conflict: Conflict,
        transform: (T) -> R,
    ): Result<R, DataError> = when (this) {
        is Result.Failure -> this
        is Result.Success -> {
            _conflicts.update { list -> list.copy(items = list.items.filterNot { it.id == conflict.id }) }
            val dir = conflict.path.substringBeforeLast('/', missingDelimiterValue = "").ifEmpty { null }
            syncthing.scan(conflict.folderId, dir)
            Result.Success(transform(data))
        }
    }

    /** `convite.png` → `convite (cópia).png`, then `convite (cópia 2).png`, … */
    private fun uniqueSibling(file: Path, suffix: String): Path {
        val parent = file.parent ?: error("file without parent: $file")
        val (base, ext) = file.name.splitExtension()
        var candidate = parent / "$base ($suffix)$ext"
        var n = 2
        while (fileSystem.exists(candidate)) candidate = parent / "$base ($suffix $n)$ext"
        return candidate
    }

    /** Syncthing ignores `.syncthing.*.tmp`, so a half-written file is never synced. */
    private fun writeAtomically(target: Path, text: String) {
        val temp = (target.parent ?: error("no parent")) / ".syncthing.${target.name}.tmp"
        fileSystem.write(temp) { writeUtf8(text) }
        fileSystem.atomicMove(temp, target)
    }

    private fun sample(file: Path): ByteArray = fileSystem.source(file).buffer().use { source ->
        val sink = okio.Buffer()
        source.read(sink, ContentSniffer.SAMPLE_BYTES.toLong())
        sink.readByteArray()
    }

    private fun readText(file: Path): String = fileSystem.read(file) { readUtf8() }

    private fun fingerprint(conflict: Conflict, current: String, copy: String): String =
        "${conflict.id}|${current.length}:${current.hashCode()}|${copy.length}:${copy.hashCode()}"

    private fun Conflict.root(): Path = folderRoot.toPath()

    private fun Conflict.file(side: VersionSide): Path =
        root().resolveSlashPath(if (side == VersionSide.CURRENT) current.path else copy.path)

    private suspend fun <T> fileCall(block: () -> T): Result<T, DataError.Local> = withContext(io) {
        try {
            Result.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: FileChangedException) {
            Result.Failure(DataError.Local.FILE_CHANGED)
        } catch (e: FileNotFoundException) {
            Result.Failure(DataError.Local.NOT_FOUND)
        } catch (e: IOException) {
            val denied = e.message.orEmpty().contains("denied", ignoreCase = true)
            Result.Failure(if (denied) DataError.Local.PERMISSION_DENIED else DataError.Local.IO)
        } catch (e: Exception) {
            Result.Failure(DataError.Local.UNKNOWN)
        }
    }

    private class FileChangedException : Exception()

    private companion object {
        const val RESCAN_DEBOUNCE_MS = 1_500L
    }
}
