package com.adamfoerster.meshsync.feature.folders.presentation

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.adamfoerster.meshsync.core.domain.model.CoreState
import com.adamfoerster.meshsync.core.domain.model.DeclinedFolderOffer
import com.adamfoerster.meshsync.core.domain.model.DeviceId
import com.adamfoerster.meshsync.core.domain.model.FileInfo
import com.adamfoerster.meshsync.core.domain.model.FolderType
import com.adamfoerster.meshsync.core.domain.model.PendingFolder
import com.adamfoerster.meshsync.core.domain.model.SyncOverview
import com.adamfoerster.meshsync.core.domain.repository.ConflictSummary
import com.adamfoerster.meshsync.core.domain.repository.SyncthingCore
import com.adamfoerster.meshsync.core.domain.repository.SyncthingRepository
import com.adamfoerster.meshsync.core.domain.util.DataError
import com.adamfoerster.meshsync.core.domain.util.EmptyResult
import com.adamfoerster.meshsync.core.domain.util.Result
import com.adamfoerster.meshsync.feature.folders.presentation.list.FolderListAction
import com.adamfoerster.meshsync.feature.folders.presentation.list.FolderListViewModel
import com.adamfoerster.meshsync.feature.folders.presentation.list.FolderOfferUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
class FolderListViewModelTest {

    private val laptop = DeviceId("AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA")
    private val repository = FakeRepository()
    private lateinit var viewModel: FolderListViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = FolderListViewModel(repository, FakeCore(), FakeConflicts())
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.collect() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun decliningAnOfferMovesItToTheDeclinedList() = runTest {
        collect()
        val offer = viewModel.state.value.offers.single()

        viewModel.onAction(FolderListAction.OnDismissOffer(offer))

        assertThat(repository.declined).containsExactly(Triple("fotos", "Fotos", laptop))
        assertThat(viewModel.state.value.offers).isEmpty()
        assertThat(viewModel.state.value.declinedOffers).containsExactly(FolderOfferUi("fotos", "Fotos", laptop.value, "Notebook"))
    }

    @Test
    fun aDeclinedOfferCanBeShownAgainAndAccepted() = runTest {
        collect()
        viewModel.onAction(FolderListAction.OnDismissOffer(viewModel.state.value.offers.single()))
        val declined = viewModel.state.value.declinedOffers.single()

        viewModel.onAction(FolderListAction.OnAcceptDeclinedOffer(declined))

        assertThat(repository.restored).containsExactly("fotos" to laptop)
        assertThat(viewModel.state.value.declinedOffers).isEmpty()
    }

    @Test
    fun offersDeclinedInEarlierSessionsAreListed() = runTest {
        repository.overview.update { it?.copy(declinedFolders = listOf(DeclinedFolderOffer("docs", "Docs", laptop, "Notebook", null))) }
        collect()

        assertThat(viewModel.state.value.declinedOffers).containsExactly(FolderOfferUi("docs", "Docs", laptop.value, "Notebook"))
    }

    private class FakeCore : SyncthingCore {
        override val state: StateFlow<CoreState> = MutableStateFlow(CoreState.Running)
        override fun start() = Unit
        override fun stop() = Unit
        override fun restart() = Unit
    }

    private class FakeConflicts : ConflictSummary {
        override val countsByFolder: Flow<Map<String, Int>> = flowOf(emptyMap())
    }

    private class FakeRepository : SyncthingRepository {
        val declined = mutableListOf<Triple<String, String, DeviceId>>()
        val restored = mutableListOf<Pair<String, DeviceId>>()

        override val overview = MutableStateFlow<SyncOverview?>(
            SyncOverview(
                myId = DeviceId("BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB-BBBBBBB"),
                myName = "Phone",
                version = "v2",
                devices = emptyList(),
                folders = emptyList(),
                pendingDevices = emptyList(),
                pendingFolders = listOf(PendingFolder("fotos", "Fotos", DeviceId("AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA-AAAAAAA"), "Notebook", null)),
                lastSync = null,
            ),
        )

        override suspend fun declineFolderOffer(folderId: String, label: String, device: DeviceId): EmptyResult<DataError.Remote> {
            declined += Triple(folderId, label, device)
            // Like the real core: ignoring an offer takes it off the pending list.
            overview.update { o ->
                o?.copy(
                    pendingFolders = o.pendingFolders.filterNot { it.folderId == folderId && it.offeredBy == device },
                    declinedFolders = o.declinedFolders + DeclinedFolderOffer(folderId, label, device, "Notebook", null),
                )
            }
            return Result.Success(Unit)
        }

        override suspend fun restoreDeclinedFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote> {
            restored += folderId to device
            overview.update { o -> o?.copy(declinedFolders = o.declinedFolders.filterNot { it.folderId == folderId && it.offeredBy == device }) }
            return Result.Success(Unit)
        }

        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String) = unused()
        override suspend fun removeDevice(id: DeviceId) = unused()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean) = unused()
        override suspend fun renameDevice(id: DeviceId, name: String) = unused()
        override suspend fun dismissPendingDevice(id: DeviceId) = unused()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>) = unused()
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>) = unused()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean) = unused()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId) = unused()
        override suspend fun setMyName(name: String) = unused()
        override suspend fun scan(folderId: String, subPath: String?) = unused()
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> = unused()
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>) = unused()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = unused()

        private fun unused(): Nothing = error("not used by these tests")
    }
}
