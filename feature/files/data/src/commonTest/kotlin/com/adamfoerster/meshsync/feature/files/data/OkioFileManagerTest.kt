package com.adamfoerster.meshsync.feature.files.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OkioFileManagerTest {
    private val fs = FakeFileSystem()
    private val manager = OkioFileManager(fs, UnconfinedTestDispatcher())
    private val root = "/storage".toPath()

    init {
        fs.createDirectories(root / "Docs")
        fs.createDirectories(root / "Photos")
        fs.write(root / "Docs" / "a.txt") { writeUtf8("A") }
        fs.write(root / "notes.txt") { writeUtf8("notes") }
        fs.write(root / "Zebra.txt") { writeUtf8("z") }
    }

    private fun text(path: String) = fs.read(path.toPath()) { readUtf8() }

    @Test
    fun listsFoldersFirstThenFilesIgnoringCase() = runTest {
        val result = manager.list("/storage") as Result.Success

        assertThat(result.data.map { it.name }).containsExactly("Docs", "Photos", "notes.txt", "Zebra.txt")
        assertThat(result.data.first().isDirectory).isTrue()
    }

    @Test
    fun listingAMissingFolderIsNotFound() = runTest {
        assertThat(manager.list("/nope")).isEqualTo(Result.Failure(DataError.Local.NOT_FOUND))
    }

    @Test
    fun deleteRemovesFoldersWithTheirContents() = runTest {
        manager.delete(listOf("/storage/Docs", "/storage/notes.txt"))

        assertThat(fs.exists(root / "Docs")).isFalse()
        assertThat(fs.exists(root / "notes.txt")).isFalse()
        assertThat(fs.exists(root / "Photos")).isTrue()
    }

    @Test
    fun renameChangesTheNameInPlace() = runTest {
        val result = manager.rename("/storage/notes.txt", "  todo.txt ")

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(text("/storage/todo.txt")).isEqualTo("notes")
        assertThat(fs.exists(root / "notes.txt")).isFalse()
    }

    @Test
    fun renameRefusesABadNameOrOneAlreadyTaken() = runTest {
        assertThat(manager.rename("/storage/notes.txt", "a/b")).isEqualTo(Result.Failure(DataError.Local.INVALID_NAME))
        assertThat(manager.rename("/storage/notes.txt", "Zebra.txt")).isEqualTo(Result.Failure(DataError.Local.ALREADY_EXISTS))
        assertThat(text("/storage/Zebra.txt")).isEqualTo("z")
    }

    @Test
    fun copyKeepsTheOriginalAndNeverOverwrites() = runTest {
        manager.paste(listOf("/storage/notes.txt"), "/storage/Docs", move = false)
        manager.paste(listOf("/storage/notes.txt"), "/storage/Docs", move = false)

        assertThat(fs.exists(root / "notes.txt")).isTrue()
        assertThat(text("/storage/Docs/notes.txt")).isEqualTo("notes")
        assertThat(text("/storage/Docs/notes (1).txt")).isEqualTo("notes")
    }

    @Test
    fun copyingAFolderCopiesItsContents() = runTest {
        manager.paste(listOf("/storage/Docs"), "/storage/Photos", move = false)

        assertThat(text("/storage/Photos/Docs/a.txt")).isEqualTo("A")
        assertThat(fs.exists(root / "Docs" / "a.txt")).isTrue()
    }

    @Test
    fun moveRemovesTheOriginal() = runTest {
        manager.paste(listOf("/storage/notes.txt"), "/storage/Photos", move = true)

        assertThat(fs.exists(root / "notes.txt")).isFalse()
        assertThat(text("/storage/Photos/notes.txt")).isEqualTo("notes")
    }

    @Test
    fun movingIntoTheSameFolderChangesNothing() = runTest {
        manager.paste(listOf("/storage/notes.txt"), "/storage", move = true)

        assertThat(text("/storage/notes.txt")).isEqualTo("notes")
        assertThat(fs.exists(root / "notes (1).txt")).isFalse()
    }

    @Test
    fun aFolderCannotGoIntoItself() = runTest {
        fs.createDirectories(root / "Docs" / "Sub")

        val result = manager.paste(listOf("/storage/Docs"), "/storage/Docs/Sub", move = false)

        assertThat(result).isEqualTo(Result.Failure(DataError.Local.INTO_ITSELF))
        assertThat(fs.exists(root / "Docs" / "Sub" / "Docs")).isFalse()
    }

    @Test
    fun createDirectoryRejectsDuplicatesAndBadNames() = runTest {
        assertThat(manager.createDirectory("/storage", "New")).isEqualTo(Result.Success(Unit))
        assertThat(fs.metadata(root / "New").isDirectory).isTrue()
        assertThat(manager.createDirectory("/storage", "New")).isEqualTo(Result.Failure(DataError.Local.ALREADY_EXISTS))
        assertThat(manager.createDirectory("/storage", " ")).isEqualTo(Result.Failure(DataError.Local.INVALID_NAME))
    }
}
