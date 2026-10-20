package com.adamfoerster.syncthing.feature.conflicts.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.feature.conflicts.domain.BinaryChoice
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Instant

class FileSystemConflictRepositoryTest {

    private val me = DeviceId("AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA")
    private val laptop = DeviceId("BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB")
    private val root = "/sdcard/Documentos".toPath()

    private lateinit var fs: FakeFileSystem
    private lateinit var syncthing: FakeSyncthing

    @BeforeTest
    fun setUp() {
        fs = FakeFileSystem()
        fs.createDirectories(root / "listas")
        fs.createDirectories(root / ".stversions")
        syncthing = FakeSyncthing(overview(me, laptop, root.toString()))
    }

    private fun TestScope.repository() = FileSystemConflictRepository(
        fileSystem = fs,
        syncthing = syncthing,
        scope = backgroundScope,
        archive = VersionArchive(fs, FixedClock, TimeZone.UTC),
        io = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun findsTextConflictWithAuthorsAndHunks() = runTest {
        fs.write(root / "listas" / "compras.txt") { writeUtf8("a\nb\nc\n") }
        fs.write(root / "listas" / "compras.sync-conflict-20261019-143500-BBBBBBB.txt") { writeUtf8("a\nB\nc\n") }
        // Syncthing's own folders are never scanned.
        fs.write(root / ".stversions" / "x.sync-conflict-20261019-143500-BBBBBBB.txt") { writeUtf8("x") }
        syncthing.modifiedBy = me.short

        val repo = repository()
        repo.rescan()

        val conflict = repo.conflicts.value.items.single()
        assertThat(conflict.path).isEqualTo("listas/compras.txt")
        assertThat(conflict.kind).isEqualTo(ConflictKind.TEXT)
        assertThat(conflict.hunkCount).isEqualTo(1)
        assertThat(conflict.current.author?.isLocal).isEqualTo(true)
        assertThat(conflict.copy.author?.name).isEqualTo("Notebook da Ana")
    }

    @Test
    fun resolvingTextWritesMergeAndArchivesBothVersions() = runTest {
        val current = root / "listas" / "compras.txt"
        val copy = root / "listas" / "compras.sync-conflict-20261019-143500-BBBBBBB.txt"
        fs.write(current) { writeUtf8("a\nb\n") }
        fs.write(copy) { writeUtf8("a\nB\n") }
        val repo = repository()
        repo.rescan()
        val conflict = repo.conflicts.value.items.single()
        val loaded = (repo.loadText(conflict) as Result.Success).data

        val result = repo.resolveText(conflict, loaded, "a\nb\nB\n")

        assertThat(result).isInstanceOf(Result.Success::class)
        assertThat(fs.read(current) { readUtf8() }).isEqualTo("a\nb\nB\n")
        assertThat(fs.exists(copy)).isFalse()
        val archived = fs.list(root / ".stversions" / "listas").map { it.name }
        assertThat(archived.sorted()).containsExactly(
            "compras.sync-conflict-20261019-143500-BBBBBBB~20261019-174500.txt",
            "compras~20261019-174500.txt",
        )
        assertThat(repo.conflicts.value.items).hasSize(0)
        assertThat(syncthing.scans).containsExactly("doc" to "listas")
    }

    @Test
    fun refusesToOverwriteWhenTheFileChangedMeanwhile() = runTest {
        val current = root / "notas.txt"
        fs.write(current) { writeUtf8("um\n") }
        fs.write(root / "notas.sync-conflict-20261019-143500-BBBBBBB.txt") { writeUtf8("dois\n") }
        val repo = repository()
        repo.rescan()
        val conflict = repo.conflicts.value.items.single()
        val loaded = (repo.loadText(conflict) as Result.Success).data
        fs.write(current) { writeUtf8("três\n") }

        val result = repo.resolveText(conflict, loaded, "um\n")

        assertThat(result).isEqualTo(Result.Failure(DataError.Local.FILE_CHANGED))
        assertThat(fs.read(current) { readUtf8() }).isEqualTo("três\n")
    }

    @Test
    fun keepBothRenamesTheCopy() = runTest {
        fs.write(root / "convite.png") { write(byteArrayOf(1, 2, 3)) }
        fs.write(root / "convite (cópia).png") { write(byteArrayOf(9)) }
        fs.write(root / "convite.sync-conflict-20261019-143500-BBBBBBB.png") { write(byteArrayOf(4, 5)) }
        val repo = repository()
        repo.rescan()
        val conflict = repo.conflicts.value.items.single()
        assertThat(conflict.kind).isEqualTo(ConflictKind.IMAGE)

        val result = repo.resolveBinary(conflict, BinaryChoice.KEEP_BOTH, "cópia")

        assertThat(result).isEqualTo(Result.Success("convite (cópia 2).png"))
        assertThat(fs.read(root / "convite (cópia 2).png") { readByteArray() }.toList()).containsExactly(4.toByte(), 5.toByte())
    }

    @Test
    fun keepCopyReplacesCurrentAndArchivesIt() = runTest {
        fs.write(root / "plan.bin") { write(byteArrayOf(0, 1)) }
        fs.write(root / "plan.sync-conflict-20261019-143500-BBBBBBB.bin") { write(byteArrayOf(0, 2)) }
        val repo = repository()
        repo.rescan()

        repo.resolveBinary(repo.conflicts.value.items.single(), BinaryChoice.KEEP_COPY, "cópia")

        assertThat(fs.read(root / "plan.bin") { readByteArray() }.toList()).containsExactly(0.toByte(), 2.toByte())
        assertThat(fs.exists(root / ".stversions" / "plan~20261019-174500.bin")).isTrue()
    }

    private object FixedClock : Clock {
        override fun now(): Instant = Instant.parse("2026-10-19T17:45:00Z")
    }

    private fun overview(me: DeviceId, laptop: DeviceId, path: String) = SyncOverview(
        myId = me,
        myName = "Pixel",
        version = "v2.1.6",
        devices = listOf(
            com.adamfoerster.syncthing.core.domain.model.Device(
                id = laptop, name = "Notebook da Ana", paused = false,
                status = com.adamfoerster.syncthing.core.domain.model.DeviceStatus.CONNECTED,
                completion = 100.0, lastSeen = null, folderIds = listOf("doc"),
            ),
        ),
        folders = listOf(
            Folder("doc", "Documentos", path, FolderType.SEND_RECEIVE, false, listOf(laptop), FolderStatus.Unknown),
        ),
        pendingDevices = emptyList(),
        pendingFolders = emptyList(),
        lastSync = null,
    )

    private class FakeSyncthing(initial: SyncOverview) : SyncthingRepository {
        override val overview = MutableStateFlow<SyncOverview?>(initial)
        var modifiedBy: String? = null
        val scans = mutableListOf<Pair<String, String?>>()

        override suspend fun refresh() = Unit
        override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> {
            scans += folderId to subPath
            return Result.Success(Unit)
        }
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> =
            Result.Success(FileInfo(path, 0, null, modifiedBy))

        private val unsupported: EmptyResult<DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)
        override suspend fun addDevice(id: DeviceId, name: String) = unsupported
        override suspend fun removeDevice(id: DeviceId) = unsupported
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean) = unsupported
        override suspend fun dismissPendingDevice(id: DeviceId) = unsupported
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>) = unsupported
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>) = unsupported
        override suspend fun setFolderPaused(folderId: String, paused: Boolean) = unsupported
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId) = unsupported
        override suspend fun setMyName(name: String) = unsupported
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> =
            Result.Failure(DataError.Remote.UNKNOWN)
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>) = unsupported
    }
}
