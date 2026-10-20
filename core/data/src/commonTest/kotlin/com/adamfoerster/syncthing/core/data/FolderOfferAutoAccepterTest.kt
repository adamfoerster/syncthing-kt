package com.adamfoerster.syncthing.core.data

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.adamfoerster.syncthing.core.data.repository.FolderOfferAutoAccepter
import com.adamfoerster.syncthing.core.domain.model.Device
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceStatus
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.Folder
import com.adamfoerster.syncthing.core.domain.model.FolderStatus
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.PendingFolder
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import com.adamfoerster.syncthing.core.domain.preferences.ThemeMode
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class FolderOfferAutoAccepterTest {
    private val me = DeviceId("ME")
    private val device2 = DeviceId("D2")
    private val device3 = DeviceId("D3")

    private fun device(id: DeviceId) = Device(id, id.value, false, DeviceStatus.CONNECTED, null, null, emptyList())
    private val docs = Folder("docs", "Docs", "/docs", FolderType.SEND_RECEIVE, false, listOf(me, device2), FolderStatus.Unknown)
    private val offer = PendingFolder("docs", "Docs", device3, "D3", null)

    private fun overview(offers: List<PendingFolder>) = SyncOverview(
        me, "me", "", listOf(device(device2), device(device3)), listOf(docs), emptyList(), offers, null,
    )

    private class FakeRepository(initial: SyncOverview, var fail: Boolean = false) : SyncthingRepository {
        val state = MutableStateFlow<SyncOverview?>(initial)
        override val overview: StateFlow<SyncOverview?> = state
        val calls = mutableListOf<Pair<String, List<DeviceId>>>()

        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote> {
            calls += folderId to devices
            return if (fail) Result.Failure(DataError.Remote.CORE_UNAVAILABLE) else Result.Success(Unit)
        }

        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote> = TODO()
        override suspend fun dismissPendingDevice(id: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean): EmptyResult<DataError.Remote> = TODO()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setMyName(name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun renameDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> = TODO()
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> = TODO()
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> = TODO()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = TODO()
    }

    private class FakePreferences(enabled: Boolean) : AppPreferences {
        override val autoAcceptKnownFolders = MutableStateFlow(enabled)
        override val languageTag = MutableStateFlow<String?>(null)
        override fun setAutoAcceptKnownFolders(enabled: Boolean) { autoAcceptKnownFolders.value = enabled }
        override val themeMode = MutableStateFlow(ThemeMode.System)
        override fun setLanguageTag(tag: String?) { languageTag.value = tag }
        override fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }
    }

    private fun TestScope.accepter(repository: FakeRepository, preferences: AppPreferences) =
        FolderOfferAutoAccepter(repository, preferences, backgroundScope + UnconfinedTestDispatcher(testScheduler)).also { it.start() }

    @Test
    fun addsTheOfferingDeviceToTheFolderKeepingTheCurrentMembers() = runTest {
        val repository = FakeRepository(overview(listOf(offer)))
        accepter(repository, FakePreferences(true))
        assertThat(repository.calls).containsExactly("docs" to listOf(me, device2, device3))
    }

    @Test
    fun announcesWhatItAccepted() = runTest {
        val repository = FakeRepository(overview(emptyList()))
        val accepter = accepter(repository, FakePreferences(true))
        accepter.accepted.test {
            repository.state.value = overview(listOf(offer))
            assertThat(awaitItem().devices.map { it.id }).containsExactly(device3)
        }
    }

    @Test
    fun doesNothingWhileTheSettingIsOff() = runTest {
        val repository = FakeRepository(overview(listOf(offer)))
        accepter(repository, FakePreferences(false))
        assertThat(repository.calls).isEmpty()
    }

    @Test
    fun turningTheSettingOnAnswersOffersThatAreAlreadyWaiting() = runTest {
        val repository = FakeRepository(overview(listOf(offer)))
        val preferences = FakePreferences(false)
        accepter(repository, preferences)
        preferences.setAutoAcceptKnownFolders(true)
        assertThat(repository.calls.size).isEqualTo(1)
    }

    @Test
    fun theSameOfferIsNotAnsweredTwiceWhileItStaysPending() = runTest {
        val repository = FakeRepository(overview(listOf(offer)))
        accepter(repository, FakePreferences(true))
        repository.state.value = overview(listOf(offer)).copy(myName = "renamed")
        assertThat(repository.calls.size).isEqualTo(1)
    }

    @Test
    fun aFailedAttemptIsRetriedOnTheNextSnapshot() = runTest {
        val repository = FakeRepository(overview(listOf(offer)), fail = true)
        accepter(repository, FakePreferences(true))
        repository.fail = false
        repository.state.value = overview(listOf(offer)).copy(myName = "renamed")
        assertThat(repository.calls.size).isEqualTo(2)
    }
}
