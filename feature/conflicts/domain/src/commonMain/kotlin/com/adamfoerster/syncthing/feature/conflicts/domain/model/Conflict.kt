package com.adamfoerster.syncthing.feature.conflicts.domain.model

import com.adamfoerster.syncthing.core.domain.model.DeviceRef
import kotlin.time.Instant

/**
 * A file that Syncthing could not merge: the file at [path] (the version that won) and a
 * `.sync-conflict-…` copy next to it (the version that lost). Nothing is lost until the user chooses.
 */
data class Conflict(
    val folderId: String,
    val folderLabel: String,
    /** Absolute path of the folder root on this device. */
    val folderRoot: String,
    /** Path of the conflicted file, relative to the folder root, `/`-separated. */
    val path: String,
    val kind: ConflictKind,
    val current: ConflictVersion,
    val copy: ConflictVersion,
    /** Number of differing passages, for text conflicts. 0 means the contents are equivalent. */
    val hunkCount: Int?,
) {
    /** Stable across rescans; there is one conflict per conflict copy. */
    val id: String get() = "$folderId:${copy.path}"
    val fileName: String get() = path.substringAfterLast('/')
}

data class ConflictVersion(
    val side: VersionSide,
    /** Relative to the folder root, `/`-separated. */
    val path: String,
    val size: Long,
    val modified: Instant?,
    val author: DeviceRef?,
)

/** [CURRENT] is the file at the original name; [COPY] is the `.sync-conflict-…` file. */
enum class VersionSide { CURRENT, COPY }

enum class ConflictKind {
    /** UTF-8 text small enough to merge passage by passage. */
    TEXT,

    /** Binary, but can be previewed. */
    IMAGE,

    /** Anything else: the user keeps one version, or both. */
    BINARY,
}
