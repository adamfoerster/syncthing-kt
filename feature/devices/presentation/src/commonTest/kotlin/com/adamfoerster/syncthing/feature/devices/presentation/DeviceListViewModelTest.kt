package com.adamfoerster.syncthing.feature.devices.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.feature.devices.presentation.list.DeviceListAction
import com.adamfoerster.syncthing.feature.devices.presentation.list.DeviceListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModelTest {

    private val laptop = DeviceId("AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA")
    private val repository = FakeRepository()
    private lateinit var viewModel: DeviceListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = DeviceListViewModel(repository, FakeCore())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    private fun openRename() {
        viewModel.onAction(DeviceListAction.OnDeviceClick(viewModel.state.value.devices.single()))
        viewModel.onAction(DeviceListAction.OnRenameRequest(viewModel.state.value.devices.single()))
    }

    @Test
    fun renameDialogStartsWithTheCurrentName() = runTest {
        collect()
        openRename()
        assertThat(viewModel.state.value.rename?.input).isEqualTo("Notebook")
        assertThat(viewModel.state.value.menuFor).isNull()
    }

    @Test
    fun confirmingSendsTheTrimmedNameAndClosesTheDialog() = runTest {
        collect()
        openRename()
        viewModel.onAction(DeviceListAction.OnRenameInputChange("  Notebook da Ana "))
        viewModel.onAction(DeviceListAction.OnRenameConfirm)

        assertThat(repository.renamed).isEqualTo(laptop to "Notebook da Ana")
        assertThat(viewModel.state.value.rename).isNull()
        assertThat(viewModel.state.value.devices.single().name).isEqualTo("Notebook da Ana")
    }

    @Test
    fun aBlankNameCannotBeSaved() = runTest {
        collect()
        openRename()
        viewModel.onAction(DeviceListAction.OnRenameInputChange("   "))
        assertThat(viewModel.state.value.rename?.canSave).isEqualTo(false)

        viewModel.onAction(DeviceListAction.OnRenameConfirm)

        assertThat(repository.renamed).isNull()
        assertThat(viewModel.state.value.rename).isNotNull()
    }

    @Test
    fun keepingTheSameNameDoesNotCallTheCore() = runTest {
        collect()
        openRename()
        viewModel.onAction(DeviceListAction.OnRenameConfirm)

        assertThat(repository.renamed).isNull()
        assertThat(viewModel.state.value.rename).isNull()
    }

    @Test
    fun aFailureIsShownAndTheNameStaysTheSame() = runTest {
        collect()
        repository.renameFails = true
        openRename()
        viewModel.onAction(DeviceListAction.OnRenameInputChange("Outro"))
        viewModel.onAction(DeviceListAction.OnRenameConfirm)

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.devices.single().name).isEqualTo("Notebook")
    }

    @Test
    fun cancellingClosesTheDialogWithoutSaving() = runTest {
        collect()
        openRename()
        viewModel.onAction(DeviceListAction.OnRenameInputChange("Outro"))
        viewModel.onAction(DeviceListAction.OnDismissMenu)

        assertThat(viewModel.state.value.rename).isNull()
        assertThat(repository.renamed).isNull()
    }

    private class FakeCore : SyncthingCore {
        override val state: StateFlow<CoreState> = MutableStateFlow(CoreState.Running)
        override fun start() = Unit
        override fun stop() = Unit
        override fun restart() = Unit
    }

    private inner class FakeRepository : SyncthingRepository {
        var renamed: Pair<DeviceId, String>? = null
        var renameFails = false

        override val overview = MutableStateFlow<SyncOverview?>(
            SyncOverview(
                myId = DeviceId("BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB"),
                myName = "Phone",
                version = "v2",
                devices = listOf(Device(laptop, "Notebook", false, DeviceStatus.CONNECTED, null, null, emptyList())),
                folders = emptyList(),
                pendingDevices = emptyList(),
                pendingFolders = emptyList(),
                lastSync = null,
            ),
        )

        override suspend fun renameDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> {
            if (renameFails) return Result.Failure(DataError.Remote.CORE_UNAVAILABLE)
            renamed = id to name
            overview.update { o -> o?.copy(devices = o.devices.map { if (it.id == id) it.copy(name = name) else it }) }
            return Result.Success(Unit)
        }

        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> = unused()
        override suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote> = unused()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote> = unused()
        override suspend fun dismissPendingDevice(id: DeviceId): EmptyResult<DataError.Remote> = unused()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>): EmptyResult<DataError.Remote> = unused()
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote> = unused()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean): EmptyResult<DataError.Remote> = unused()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote> = unused()
        override suspend fun setMyName(name: String): EmptyResult<DataError.Remote> = unused()
        override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> = unused()
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> = unused()
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> = unused()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = unused()

        private fun unused(): Nothing = error("not used by these tests")
    }
}
