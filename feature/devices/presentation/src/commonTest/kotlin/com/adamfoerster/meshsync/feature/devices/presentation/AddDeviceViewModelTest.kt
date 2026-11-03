package com.adamfoerster.meshsync.feature.devices.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.FileInfo
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.model.SyncOverview
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.devices.presentation.add.AddDeviceAction
import com.adamfoerster.meshsync.feature.devices.presentation.add.AddDeviceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddDeviceViewModelTest {
    private val core = FakeCore()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AddDeviceViewModel(null, null, FakeRepository(), core, SavedStateHandle())

    @Test
    fun theFlowFollowsTheCoreState() {
        core.state.value = CoreState.Starting
        val vm = viewModel()
        assertThat(vm.state.value.coreReady).isFalse()

        core.state.value = CoreState.Running
        assertThat(vm.state.value.coreReady).isTrue()
    }

    @Test
    fun startingTheCoreFromTheFlowAsksTheCore() {
        viewModel().onAction(AddDeviceAction.OnStartCore)

        assertThat(core.starts).isEqualTo(1)
    }

    private class FakeCore : SyncthingCore {
        override val state = MutableStateFlow<CoreState>(CoreState.Running)
        var starts = 0
        override fun start() { starts++ }
        override fun stop() = Unit
        override fun restart() = Unit
    }

    private class FakeRepository : SyncthingRepository {
        override val overview: StateFlow<SyncOverview?> = MutableStateFlow(null)
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
