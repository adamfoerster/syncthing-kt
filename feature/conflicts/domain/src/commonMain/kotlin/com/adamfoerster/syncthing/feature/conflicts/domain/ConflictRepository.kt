package com.adamfoerster.syncthing.feature.conflicts.domain

import com.adamfoerster.syncthing.core.domain.util.DataError
import com.adamfoerster.syncthing.core.domain.util.EmptyResult
import com.adamfoerster.syncthing.core.domain.util.Result
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide
import kotlinx.coroutines.flow.StateFlow

interface ConflictRepository {
    val conflicts: StateFlow<ConflictList>

    /** Walks every folder again. Also happens on its own when a folder's index changes. */
    suspend fun rescan()

    fun find(conflictId: String): Conflict? = conflicts.value.items.firstOrNull { it.id == conflictId }

    /** Both versions decoded as UTF-8, plus a fingerprint used to refuse stale writes. */
    suspend fun loadText(conflict: Conflict): Result<TextVersions, DataError.Local>

    suspend fun loadBytes(conflict: Conflict, side: VersionSide): Result<ByteArray, DataError.Local>

    /**
     * Writes [merged] over the current file. Both previous versions go to `.stversions`, so the
     * user can still restore either of them from the history.
     */
    suspend fun resolveText(conflict: Conflict, loaded: TextVersions, merged: String): EmptyResult<DataError>

    /** See [BinaryChoice]. [copySuffix] names the second file when keeping both, e.g. "cópia". */
    suspend fun resolveBinary(conflict: Conflict, choice: BinaryChoice, copySuffix: String): Result<String, DataError>
}

data class ConflictList(
    val items: List<Conflict> = emptyList(),
    val isScanning: Boolean = false,
    /** False until the first scan finished. */
    val isLoaded: Boolean = false,
)

data class TextVersions(
    val current: String,
    val copy: String,
    val fingerprint: String,
)

enum class BinaryChoice {
    /** Keep the file at the original name; the copy is archived. */
    KEEP_CURRENT,

    /** The copy replaces the file at the original name; the old one is archived. */
    KEEP_COPY,

    /** The copy is renamed to `name (suffix).ext` and kept next to the original. */
    KEEP_BOTH,
}
