package com.adamfoerster.meshsync.feature.files.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.FileInfo
import com.adamfoerster.meshsync.core.domain.model.Folder
import com.adamfoerster.meshsync.core.domain.model.FolderStatus
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.model.SyncOverview
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.files.domain.FileEntry
import com.adamfoerster.meshsync.feature.files.domain.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelTest {
    private val files = FakeFileManager()
    private val repository = FakeRepository(sharedPaths = listOf("/root/Photos"))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = FilesViewModel("/root", repository, files, SavedStateHandle())

    @Test
    fun listsTheRootAndMarksFoldersMeshSyncShares() = runTest {
        val vm = viewModel()

        assertThat(vm.state.value.entries.map { it.name }).containsExactly("Photos", "Docs", "a.txt")
        assertThat(vm.state.value.entries.first { it.name == "Photos" }.isShared).isTrue()
        assertThat(vm.state.value.entries.first { it.name == "Docs" }.isShared).isFalse()
        assertThat(vm.state.value.entries.first { it.name == "a.txt" }.isShared).isFalse()
    }

    @Test
    fun tappingAFolderOpensItAndBackGoesUp() = runTest {
        val vm = viewModel()

        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))
        assertThat(vm.state.value.directory).isEqualTo("/root/Docs")
        assertThat(vm.state.value.entries.map { it.name }).containsExactly("b.txt")

        vm.onAction(FilesAction.OnBackClick)
        assertThat(vm.state.value.directory).isEqualTo("/root")
    }

    @Test
    fun backAtTheRootLeavesTheScreen() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(FilesAction.OnBackClick)
            assertThat(awaitItem()).isEqualTo(FilesEvent.Exit)
        }
    }

    @Test
    fun backFirstClearsTheSelection() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        assertThat(vm.state.value.isSelecting).isTrue()

        vm.onAction(FilesAction.OnBackClick)

        assertThat(vm.state.value.isSelecting).isFalse()
        assertThat(vm.state.value.directory).isEqualTo("/root")
    }

    @Test
    fun whileSelectingATapTogglesInsteadOfOpening() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))

        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))

        assertThat(vm.state.value.directory).isEqualTo("/root")
        assertThat(vm.state.value.selection.map { it.name }).containsExactly("Docs", "a.txt")
    }

    @Test
    fun copyThenPasteInAnotherFolder() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        vm.onAction(FilesAction.OnCopy)
        assertThat(vm.state.value.clipboard).isEqualTo(FileClipboard(listOf("/root/a.txt"), move = false))
        assertThat(vm.state.value.isSelecting).isFalse()

        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))
        vm.onAction(FilesAction.OnPaste)

        assertThat(files.pastes).containsExactly(Paste(listOf("/root/a.txt"), "/root/Docs", move = false))
        // A copy can be pasted again; a move cannot.
        assertThat(vm.state.value.clipboard).isNotNull()
    }

    @Test
    fun aMoveEmptiesTheClipboardOnceItWorked() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        vm.onAction(FilesAction.OnCut)
        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))

        vm.onAction(FilesAction.OnPaste)

        assertThat(files.pastes.single().move).isTrue()
        assertThat(vm.state.value.clipboard).isNull()
    }

    @Test
    fun deleteAsksForConfirmationFirst() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))

        vm.onAction(FilesAction.OnDeleteClick)
        assertThat(files.deleted).isEmpty()
        assertThat(vm.state.value.dialog).isEqualTo(FilesDialog.Delete(listOf("/root/a.txt")))

        vm.onAction(FilesAction.OnConfirmDialog)
        assertThat(files.deleted).containsExactly("/root/a.txt")
        assertThat(vm.state.value.dialog).isNull()
        assertThat(vm.state.value.isSelecting).isFalse()
    }

    @Test
    fun renameKeepsThePromptOpenWhenTheNameIsRefused() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        vm.onAction(FilesAction.OnRenameClick)
        vm.onAction(FilesAction.OnDialogTextChange("b.txt"))
        files.failure = DataError.Local.ALREADY_EXISTS

        vm.onAction(FilesAction.OnConfirmDialog)

        assertThat(vm.state.value.dialog).isEqualTo(FilesDialog.Rename("/root/a.txt", "b.txt"))
        assertThat(vm.state.value.error).isNotNull()

        files.failure = null
        vm.onAction(FilesAction.OnConfirmDialog)
        assertThat(files.renames).containsExactly("/root/a.txt" to "b.txt")
        assertThat(vm.state.value.dialog).isNull()
    }

    @Test
    fun newFolderIsCreatedInTheCurrentDirectory() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))
        vm.onAction(FilesAction.OnNewFolderClick)
        vm.onAction(FilesAction.OnDialogTextChange("Receipts"))

        vm.onAction(FilesAction.OnConfirmDialog)

        assertThat(files.created).containsExactly("/root/Docs" to "Receipts")
    }

    @Test
    fun onlyFilesCanBeShared() = runTest {
        val vm = viewModel()
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        assertThat(vm.state.value.canShare).isTrue()
        vm.events.test {
            vm.onAction(FilesAction.OnShare)
            assertThat(awaitItem()).isEqualTo(FilesEvent.Share(listOf("/root/a.txt")))
        }

        vm.onAction(FilesAction.OnEntryLongClick("/root/Docs"))
        assertThat(vm.state.value.canShare).isFalse()
    }

    @Test
    fun syncIsOfferedOnlyForASingleFolderNotYetShared() = runTest {
        val vm = viewModel()

        vm.onAction(FilesAction.OnEntryLongClick("/root/Photos"))
        assertThat(vm.state.value.canSync).isFalse()

        vm.onAction(FilesAction.OnClearSelection)
        vm.onAction(FilesAction.OnEntryLongClick("/root/a.txt"))
        assertThat(vm.state.value.canSync).isFalse()

        vm.onAction(FilesAction.OnClearSelection)
        vm.onAction(FilesAction.OnEntryLongClick("/root/Docs"))
        assertThat(vm.state.value.canSync).isTrue()
        vm.events.test {
            vm.onAction(FilesAction.OnSync)
            assertThat(awaitItem()).isEqualTo(FilesEvent.Sync("/root/Docs"))
        }
    }

    @Test
    fun aFailedListingKeepsTheCurrentFolderAndShowsTheError() = runTest {
        val vm = viewModel()
        files.failure = DataError.Local.PERMISSION_DENIED

        vm.onAction(FilesAction.OnEntryClick("/root/Docs"))

        assertThat(vm.state.value.directory).isEqualTo("/root")
        assertThat(vm.state.value.error).isNotNull()
    }

    private data class Paste(val paths: List<String>, val target: String, val move: Boolean)

    private class FakeFileManager : FileManager {
        var failure: DataError.Local? = null
        val deleted = mutableListOf<String>()
        val renames = mutableListOf<Pair<String, String>>()
        val created = mutableListOf<Pair<String, String>>()
        val pastes = mutableListOf<Paste>()

        private val tree = mapOf(
            "/root" to listOf(
                FileEntry("/root/Photos", "Photos", true, 0, null),
                FileEntry("/root/Docs", "Docs", true, 0, null),
                FileEntry("/root/a.txt", "a.txt", false, 10, null),
            ),
            "/root/Docs" to listOf(FileEntry("/root/Docs/b.txt", "b.txt", false, 5, null)),
        )

        override suspend fun list(directory: String): Result<List<FileEntry>, DataError.Local> =
            failure?.let { Result.Failure(it) } ?: Result.Success(tree[directory].orEmpty())

        private fun outcome(): EmptyResult<DataError.Local> = failure?.let { Result.Failure(it) } ?: Result.Success(Unit)

        override suspend fun delete(paths: List<String>): EmptyResult<DataError.Local> = outcome().also { if (failure == null) deleted += paths }
        override suspend fun rename(path: String, newName: String): EmptyResult<DataError.Local> = outcome().also { if (failure == null) renames += path to newName }
        override suspend fun createDirectory(parent: String, name: String): EmptyResult<DataError.Local> = outcome().also { if (failure == null) created += parent to name }
        override suspend fun paste(paths: List<String>, targetDirectory: String, move: Boolean): EmptyResult<DataError.Local> =
            outcome().also { if (failure == null) pastes += Paste(paths, targetDirectory, move) }
    }

    private class FakeRepository(sharedPaths: List<String>) : SyncthingRepository {
        override val overview: StateFlow<SyncOverview?> = MutableStateFlow(
            SyncOverview(
                myId = DeviceId("BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB"),
                myName = "Phone",
                version = "v2",
                devices = emptyList(),
                folders = sharedPaths.map { Folder(it.substringAfterLast('/'), it.substringAfterLast('/'), "$it/", FolderType.SEND_RECEIVE, false, emptyList(), FolderStatus.Unknown) },
                pendingDevices = emptyList(),
                pendingFolders = emptyList(),
                lastSync = null,
            ),
        )
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> = unused()
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>) = unused()
        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String) = unused()
        override suspend fun removeDevice(id: DeviceId) = unused()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean) = unused()
        override suspend fun dismissPendingDevice(id: DeviceId) = unused()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>) = unused()
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>) = unused()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean) = unused()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId) = unused()
        override suspend fun declineFolderOffer(folderId: String, label: String, device: DeviceId) = unused()
        override suspend fun restoreDeclinedFolder(folderId: String, device: DeviceId) = unused()
        override suspend fun setMyName(name: String) = unused()
        override suspend fun renameDevice(id: DeviceId, name: String) = unused()
        override suspend fun scan(folderId: String, subPath: String?) = unused()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = unused()

        private fun unused(): Nothing = error("not used by these tests")
    }
}
