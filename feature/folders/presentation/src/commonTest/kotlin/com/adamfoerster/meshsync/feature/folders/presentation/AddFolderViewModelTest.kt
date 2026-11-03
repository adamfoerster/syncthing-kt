package com.adamfoerster.meshsync.feature.folders.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.FileInfo
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.model.SyncOverview
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.folders.presentation.add.AddFolderAction
import com.adamfoerster.meshsync.feature.folders.presentation.add.AddFolderViewModel
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
class AddFolderViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        AddFolderViewModel(null, "/storage/emulated/0", FakeRepository(), handle)

    @Test
    fun aPickedFolderSetsThePathAndNamesTheShareWhenNoNameWasTyped() = runTest {
        val vm = viewModel()
        vm.onAction(AddFolderAction.OnFolderPicked("/storage/emulated/0/Documents/Fotos/"))

        assertThat(vm.state.value.path).isEqualTo("/storage/emulated/0/Documents/Fotos/")
        assertThat(vm.state.value.label).isEqualTo("Fotos")
        assertThat(vm.state.value.isValid).isEqualTo(true)
    }

    @Test
    fun aPickedFolderKeepsTheNameTheUserTypedAndStopsThePathFollowingIt() = runTest {
        val vm = viewModel()
        vm.onAction(AddFolderAction.OnLabelChange("Trabalho"))
        vm.onAction(AddFolderAction.OnFolderPicked("/storage/emulated/0/Docs"))
        vm.onAction(AddFolderAction.OnLabelChange("Trabalho 2"))

        assertThat(vm.state.value.label).isEqualTo("Trabalho 2")
        assertThat(vm.state.value.path).isEqualTo("/storage/emulated/0/Docs")
    }

    @Test
    fun theChoiceSurvivesProcessDeath() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).onAction(AddFolderAction.OnFolderPicked("/storage/emulated/0/Docs"))

        val restored = viewModel(handle)
        assertThat(restored.state.value.path).isEqualTo("/storage/emulated/0/Docs")
        assertThat(restored.state.value.label).isEqualTo("Docs")
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
