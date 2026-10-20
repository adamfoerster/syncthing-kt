package com.adamfoerster.syncthing.feature.settings.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.model.CoreState
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import com.adamfoerster.syncthing.core.domain.preferences.ThemeMode
import com.adamfoerster.syncthing.core.domain.repository.SyncthingCore
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val preferences = FakePreferences()
    private lateinit var viewModel: SettingsViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = SettingsViewModel(FakeRepository(), FakeCore(), preferences)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun autoAcceptStartsOnAndFollowsTheSwitch() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertThat(viewModel.state.value.autoAcceptKnownFolders).isTrue()

        viewModel.onAction(SettingsAction.OnAutoAcceptChange(false))

        assertThat(preferences.autoAcceptKnownFolders.value).isFalse()
        assertThat(viewModel.state.value.autoAcceptKnownFolders).isFalse()
    }

    @Test
    fun languageOverrideIsStoredAndCanBeClearedToFollowTheDevice() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertThat(viewModel.state.value.languageTag).isNull()

        viewModel.onAction(SettingsAction.OnLanguageChange("de"))
        assertThat(viewModel.state.value.languageTag).isEqualTo("de")

        viewModel.onAction(SettingsAction.OnLanguageChange(null))
        assertThat(viewModel.state.value.languageTag).isNull()
    }

    @Test
    fun aLanguageWeDoNotTranslateIsIgnored() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        viewModel.onAction(SettingsAction.OnLanguageChange("ja"))
        assertThat(viewModel.state.value.languageTag).isNull()
    }

    @Test
    fun themeStartsFollowingTheDeviceAndCanBeOverridden() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertThat(viewModel.state.value.themeMode).isEqualTo(ThemeMode.System)

        viewModel.onAction(SettingsAction.OnThemeChange(ThemeMode.Light))
        assertThat(preferences.themeMode.value).isEqualTo(ThemeMode.Light)
        assertThat(viewModel.state.value.themeMode).isEqualTo(ThemeMode.Light)

        viewModel.onAction(SettingsAction.OnThemeChange(ThemeMode.System))
        assertThat(viewModel.state.value.themeMode).isEqualTo(ThemeMode.System)
    }

    private class FakePreferences : AppPreferences {
        override val autoAcceptKnownFolders = MutableStateFlow(AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS)
        override val languageTag = MutableStateFlow<String?>(null)
        override fun setAutoAcceptKnownFolders(enabled: Boolean) { autoAcceptKnownFolders.value = enabled }
        override val themeMode = MutableStateFlow(ThemeMode.System)
        override fun setLanguageTag(tag: String?) { languageTag.value = tag }
        override fun setThemeMode(mode: ThemeMode) { themeMode.value = mode }
    }

    private class FakeCore : SyncthingCore {
        override val state: StateFlow<CoreState> = MutableStateFlow(CoreState.Running)
        override fun start() = Unit
        override fun stop() = Unit
        override fun restart() = Unit
    }

    private class FakeRepository : SyncthingRepository {
        override val overview: StateFlow<SyncOverview?> = MutableStateFlow(null)
        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun removeDevice(id: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean): EmptyResult<DataError.Remote> = TODO()
        override suspend fun dismissPendingDevice(id: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean): EmptyResult<DataError.Remote> = TODO()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId): EmptyResult<DataError.Remote> = TODO()
        override suspend fun setMyName(name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun renameDevice(id: DeviceId, name: String): EmptyResult<DataError.Remote> = TODO()
        override suspend fun scan(folderId: String, subPath: String?): EmptyResult<DataError.Remote> = TODO()
        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> = TODO()
        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> = TODO()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = TODO()
    }
}
