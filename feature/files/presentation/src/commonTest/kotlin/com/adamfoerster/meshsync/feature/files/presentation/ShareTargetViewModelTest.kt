package com.adamfoerster.meshsync.feature.files.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.FileInfo
import com.adamfoerster.meshsync.core.domain.model.Folder
import com.adamfoerster.meshsync.core.domain.model.FolderStatus
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.model.PendingFile
import com.adamfoerster.meshsync.core.domain.model.SyncOverview
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.files.domain.IncomingShare
import com.adamfoerster.meshsync.feature.files.domain.IncomingShareInbox
import com.adamfoerster.meshsync.feature.files.domain.ShareImporter
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
class ShareTargetViewModelTest {
    private val inbox = IncomingShareInbox().apply { offer(IncomingShare(uris = listOf("content://a/1", "content://a/2"))) }
    private val importer = FakeImporter()
    private val repository = FakeRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ShareTargetViewModel(inbox, repository, importer)

    @Test
    fun listsSyncedFoldersExceptEncryptedOnes() = runTest {
        val vm = viewModel()

        assertThat(vm.state.value.itemCount).isEqualTo(2)
        assertThat(vm.state.value.folders.map { it.id }).containsExactly("photos", "docs")
    }

    @Test
    fun choosingAFolderCopiesTheShareThereAndRescansIt() = runTest {
        val vm = viewModel()

        vm.onAction(ShareTargetAction.OnFolderClick("docs"))

        assertThat(importer.target).isEqualTo("/storage/Docs/")
        assertThat(importer.received?.uris).isEqualTo(listOf("content://a/1", "content://a/2"))
        assertThat(repository.scanned).containsExactly("docs")
        assertThat(vm.state.value.done).isEqualTo(ShareDone(copied = 2, folderLabel = "Docs"))
        assertThat(inbox.pending.value).isNull()
    }

    @Test
    fun aFailedCopyKeepsTheShareSoTheUserCanTryAnotherFolder() = runTest {
        importer.result = Result.Failure(DataError.Local.PERMISSION_DENIED)
        val vm = viewModel()

        vm.onAction(ShareTargetAction.OnFolderClick("docs"))

        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.isImporting).isFalse()
        assertThat(vm.state.value.done).isNull()
        assertThat(inbox.pending.value).isNotNull()
        assertThat(repository.scanned).isEqualTo(emptyList())
    }

    @Test
    fun cancellingDropsTheShareAndLeaves() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(ShareTargetAction.OnCancel)

            assertThat(awaitItem()).isEqualTo(ShareTargetEvent.Exit)
        }
        assertThat(inbox.pending.value).isNull()
    }

    @Test
    fun emptySharesAreIgnoredByTheInbox() {
        val empty = IncomingShareInbox()
        empty.offer(IncomingShare())
        empty.offer(IncomingShare(text = ""))

        assertThat(empty.pending.value).isNull()
    }

    private class FakeImporter : ShareImporter {
        var result: Result<Int, DataError.Local>? = null
        var target: String? = null
        var received: IncomingShare? = null

        override suspend fun import(share: IncomingShare, targetDirectory: String): Result<Int, DataError.Local> {
            target = targetDirectory
            received = share
            return result ?: Result.Success(share.itemCount)
        }
    }

    private class FakeRepository : SyncthingRepository {
        val scanned = mutableListOf<String>()

        private fun folder(id: String, label: String, type: FolderType) =
            Folder(id, label, "/storage/$label/", type, false, emptyList(), FolderStatus.Unknown)

        override val overview: StateFlow<SyncOverview?> = MutableStateFlow(
            SyncOverview(
                myId = DeviceId("BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB"),
                myName = "Phone",
                version = "v2",
                devices = emptyList(),
                folders = listOf(
                    folder("photos", "Photos", FolderType.SEND_RECEIVE),
                    folder("docs", "Docs", FolderType.SEND_ONLY),
                    folder("vault", "Vault", FolderType.RECEIVE_ENCRYPTED),
                ),
                pendingDevices = emptyList(),
                pendingFolders = emptyList(),
                lastSync = null,
            ),
        )

        override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> {
            scanned += folderId
            return Result.Success(Unit)
        }

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
        override suspend fun pendingFiles(device: DeviceId, folderId: String): Result<List<PendingFile>, DataError.Remote> = unused()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = unused()

        private fun unused(): Nothing = error("not used by these tests")
    }
}
