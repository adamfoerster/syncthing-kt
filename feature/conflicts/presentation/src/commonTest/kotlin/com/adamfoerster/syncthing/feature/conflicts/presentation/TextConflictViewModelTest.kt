package com.adamfoerster.syncthing.feature.conflicts.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.model.DeviceId
import com.adamfoerster.syncthing.core.domain.model.DeviceRef
import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.feature.conflicts.domain.BinaryChoice
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictList
import com.adamfoerster.syncthing.feature.conflicts.domain.ConflictRepository
import com.adamfoerster.syncthing.feature.conflicts.domain.TextVersions
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictKind
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictVersion
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.LineOrigin
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.Pick
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.ResolverPhase
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.TextConflictAction
import com.adamfoerster.syncthing.feature.conflicts.presentation.text.TextConflictViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TextConflictViewModelTest {

    private val me = DeviceRef(DeviceId("A".repeat(7)), "Pixel", isLocal = true)
    private val laptop = DeviceRef(DeviceId("B".repeat(7)), "Notebook", isLocal = false)

    // The phone's version is the conflict copy here, so it must still be shown first.
    private val conflict = Conflict(
        folderId = "docs", folderLabel = "Documentos", folderRoot = "/x", path = "a.txt", kind = ConflictKind.TEXT,
        current = ConflictVersion(VersionSide.CURRENT, "a.txt", 1, null, laptop),
        copy = ConflictVersion(VersionSide.COPY, "a.sync-conflict-20261019-143500-AAAAAAA.txt", 1, null, me),
        hunkCount = 2,
    )
    private val repository = FakeRepository(conflict, current = "x\nlaptop1\ny\nlaptop2\n", copy = "x\nphone1\ny\nphone2\n")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun localVersionComesFirstAndPicksMapToTheRightSide() = runTest {
        val vm = TextConflictViewModel(conflict.id, repository, SavedStateHandle())
        assertThat(vm.state.value.first?.isLocal).isEqualTo(true)
        assertThat(vm.state.value.canFinish).isFalse()

        vm.onAction(TextConflictAction.OnPick(0, Pick.FIRST))
        vm.onAction(TextConflictAction.OnPick(1, Pick.BOTH))
        assertThat(vm.state.value.result.filter { it.origin != LineOrigin.COMMON }.map { it.text })
            .isEqualTo(listOf("phone1", "phone2", "laptop2"))
        assertThat(vm.state.value.canFinish).isTrue()

        vm.onAction(TextConflictAction.OnFinish)
        assertThat(repository.written).isEqualTo("x\nphone1\ny\nphone2\nlaptop2\n")
        assertThat(vm.state.value.phase).isEqualTo(ResolverPhase.DONE)
    }

    @Test
    fun manualEditStartsWithMarkersAndBlocksUntilTheyAreGone() = runTest {
        val vm = TextConflictViewModel(conflict.id, repository, SavedStateHandle())
        vm.onAction(TextConflictAction.OnPick(0, Pick.SECOND))
        vm.onAction(TextConflictAction.OnStartEditing)
        assertThat(vm.state.value.editorText.lines()).isEqualTo(
            listOf("x", "laptop1", "y", "<<<<<<< Notebook", "laptop2", "=======", "phone2", ">>>>>>> Pixel", ""),
        )
        assertThat(vm.state.value.canFinish).isFalse()

        vm.onAction(TextConflictAction.OnEditorChange("x\nlaptop1\ny\nboth2\n"))
        assertThat(vm.state.value.canFinish).isTrue()
        vm.onAction(TextConflictAction.OnFinish)
        assertThat(repository.written).isNotNull().isEqualTo("x\nlaptop1\ny\nboth2\n")
    }

    @Test
    fun picksSurviveProcessDeath() = runTest {
        val handle = SavedStateHandle()
        TextConflictViewModel(conflict.id, repository, handle).onAction(TextConflictAction.OnPick(1, Pick.SECOND))
        val restored = TextConflictViewModel(conflict.id, repository, handle)
        assertThat(restored.state.value.resolvedCount).isEqualTo(1)
    }

    private class FakeRepository(conflict: Conflict, private val current: String, private val copy: String) : ConflictRepository {
        override val conflicts = MutableStateFlow(ConflictList(listOf(conflict), isLoaded = true))
        var written: String? = null

        override suspend fun rescan() = Unit
        override suspend fun loadText(conflict: Conflict): Result<TextVersions, DataError.Local> =
            Result.Success(TextVersions(current, copy, "fp"))
        override suspend fun loadBytes(conflict: Conflict, side: VersionSide): Result<ByteArray, DataError.Local> =
            Result.Failure(DataError.Local.NOT_FOUND)
        override suspend fun resolveText(conflict: Conflict, loaded: TextVersions, merged: String): EmptyResult<DataError> {
            written = merged
            return Result.Success(Unit)
        }
        override suspend fun resolveBinary(conflict: Conflict, choice: BinaryChoice, copySuffix: String): Result<String, DataError> =
            Result.Failure(DataError.Local.UNKNOWN)
    }
}
