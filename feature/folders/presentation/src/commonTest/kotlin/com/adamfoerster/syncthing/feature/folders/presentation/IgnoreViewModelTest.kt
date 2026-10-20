package com.adamfoerster.syncthing.feature.folders.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.FileInfo
import com.adamfoerster.syncthing.core.domain.model.FolderType
import com.adamfoerster.syncthing.core.domain.model.IgnorePreset
import com.adamfoerster.syncthing.core.domain.model.IgnoreRule
import com.adamfoerster.syncthing.core.domain.model.SyncOverview
import com.adamfoerster.syncthing.core.domain.repository.SyncthingRepository
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.feature.folders.presentation.ignore.IgnoreAction
import com.adamfoerster.syncthing.feature.folders.presentation.ignore.IgnoreEvent
import com.adamfoerster.syncthing.feature.folders.presentation.ignore.IgnorePhase
import com.adamfoerster.syncthing.feature.folders.presentation.ignore.IgnoreViewModel
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
class IgnoreViewModelTest {

    private val repository = FakeRepository(listOf("*.tmp", "// note"))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = IgnoreViewModel("docs", repository, handle)

    @Test
    fun loadsTheCurrentRulesAndHidesCommentsFromTheSimpleList() = runTest {
        val vm = viewModel()
        assertThat(vm.state.value.phase).isEqualTo(IgnorePhase.READY)
        assertThat(vm.state.value.rules).containsExactly(IgnoreRule.FileType("tmp"))
        assertThat(vm.state.value.isDirty).isFalse()
    }

    @Test
    fun aFailedLoadOffersRetryAndRecovers() = runTest {
        repository.loadFails = true
        val vm = viewModel()
        assertThat(vm.state.value.phase).isEqualTo(IgnorePhase.LOAD_ERROR)
        assertThat(vm.state.value.error).isNotNull()

        repository.loadFails = false
        vm.onAction(IgnoreAction.OnRetry)
        assertThat(vm.state.value.phase).isEqualTo(IgnorePhase.READY)
    }

    @Test
    fun addingARuleAppendsItClearsTheFieldAndMarksTheScreenDirty() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnInputChange("  node_modules "))
        vm.onAction(IgnoreAction.OnAddClick)

        assertThat(vm.state.value.lines).containsExactly("*.tmp", "// note", "node_modules")
        assertThat(vm.state.value.input).isEqualTo("")
        assertThat(vm.state.value.isDirty).isTrue()
    }

    @Test
    fun addingNothingShowsAnErrorAndTypingClearsIt() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnInputChange("   "))
        vm.onAction(IgnoreAction.OnAddClick)
        assertThat(vm.state.value.inputError).isNotNull()
        assertThat(vm.state.value.isDirty).isFalse()

        vm.onAction(IgnoreAction.OnInputChange("a"))
        assertThat(vm.state.value.inputError).isNull()
    }

    @Test
    fun removingAndTogglingPresetsEditTheSameRules() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnTogglePreset(IgnorePreset.SYSTEM_FILES))
        assertThat(vm.state.value.presets.first { it.preset == IgnorePreset.SYSTEM_FILES }.active).isTrue()

        vm.onAction(IgnoreAction.OnRemove("*.tmp"))
        assertThat(vm.state.value.lines).containsExactly("// note", ".DS_Store", "Thumbs.db", "desktop.ini")

        vm.onAction(IgnoreAction.OnTogglePreset(IgnorePreset.SYSTEM_FILES))
        assertThat(vm.state.value.lines).containsExactly("// note")
    }

    @Test
    fun advancedAndSimpleEditorsShareOneTextSoNothingIsLostWhenSwitching() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnAdvancedChange(true))
        vm.onAction(IgnoreAction.OnTextChange("*.tmp\n!keep.tmp\n(?i)Backup*"))
        vm.onAction(IgnoreAction.OnAdvancedChange(false))

        assertThat(vm.state.value.advanced).isFalse()
        assertThat(vm.state.value.rules.map { it.line }).containsExactly("*.tmp", "!keep.tmp", "(?i)Backup*")
        assertThat(vm.state.value.rules[1]).isEqualTo(IgnoreRule.Advanced("!keep.tmp"))
    }

    @Test
    fun saveSendsTheLinesAndClearsTheDirtyFlag() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnInputChange("cache"))
        vm.onAction(IgnoreAction.OnAddClick)

        vm.onAction(IgnoreAction.OnSave)

        assertThat(repository.saved).isEqualTo("docs" to listOf("*.tmp", "// note", "cache"))
        assertThat(vm.state.value.isDirty).isFalse()
    }

    @Test
    fun emitsSavedAfterASuccessfulSave() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnInputChange("cache"))
        vm.onAction(IgnoreAction.OnAddClick)

        val events = mutableListOf<IgnoreEvent>()
        val job = launch(Dispatchers.Unconfined) { vm.events.collect { events += it } }
        vm.onAction(IgnoreAction.OnSave)
        job.cancel()

        assertThat(events).containsExactly(IgnoreEvent.Saved)
    }

    @Test
    fun aRejectedSaveKeepsTheEditsAndShowsTheError() = runTest {
        repository.saveFails = true
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnInputChange("cache"))
        vm.onAction(IgnoreAction.OnAddClick)

        vm.onAction(IgnoreAction.OnSave)

        assertThat(vm.state.value.error).isNotNull()
        assertThat(vm.state.value.isDirty).isTrue()
        assertThat(vm.state.value.isSaving).isFalse()
    }

    @Test
    fun saveDoesNothingWhenNothingChanged() = runTest {
        val vm = viewModel()
        vm.onAction(IgnoreAction.OnSave)
        assertThat(repository.saved).isNull()
    }

    @Test
    fun unsavedEditsSurviveProcessDeath() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onAction(IgnoreAction.OnAdvancedChange(true))
            onAction(IgnoreAction.OnTextChange("*.tmp\nbig-folder"))
        }

        val restored = viewModel(handle)
        assertThat(restored.state.value.advanced).isTrue()
        assertThat(restored.state.value.lines).containsExactly("*.tmp", "big-folder")
        assertThat(restored.state.value.isDirty).isTrue()
    }

    private class FakeRepository(private val stored: List<String>) : SyncthingRepository {
        var loadFails = false
        var saveFails = false
        var saved: Pair<String, List<String>>? = null

        override val overview: StateFlow<SyncOverview?> = MutableStateFlow(null)

        override suspend fun ignorePatterns(folderId: String): Result<List<String>, DataError.Remote> =
            if (loadFails) Result.Failure(DataError.Remote.CORE_UNAVAILABLE) else Result.Success(stored)

        override suspend fun setIgnorePatterns(folderId: String, lines: List<String>): EmptyResult<DataError.Remote> {
            if (saveFails) return Result.Failure(DataError.Remote.BAD_REQUEST)
            saved = folderId to lines
            return Result.Success(Unit)
        }

        override suspend fun refresh() = Unit
        override suspend fun addDevice(id: DeviceId, name: String) = unused()
        override suspend fun removeDevice(id: DeviceId) = unused()
        override suspend fun setDevicePaused(id: DeviceId, paused: Boolean) = unused()
        override suspend fun dismissPendingDevice(id: DeviceId) = unused()
        override suspend fun addFolder(id: String, label: String, path: String, type: FolderType, devices: List<DeviceId>) = unused()
        override suspend fun setFolderDevices(folderId: String, devices: List<DeviceId>) = unused()
        override suspend fun setFolderPaused(folderId: String, paused: Boolean) = unused()
        override suspend fun dismissPendingFolder(folderId: String, device: DeviceId) = unused()
        override suspend fun setMyName(name: String) = unused()
        override suspend fun scan(folderId: String, subPath: String?) = unused()
        override suspend fun fileInfo(folderId: String, path: String): Result<FileInfo, DataError.Remote> = unused()

        private fun unused(): Nothing = error("not used by these tests")
    }
}
