package com.adamfoerster.syncthing.core.domain.model

import kotlin.time.Instant

/** What the core's index knows about a file (`/rest/db/file`). */
data class FileInfo(
    val path: String,
    val size: Long,
    val modified: Instant?,
    /** Short ID of the device that made the last change. */
    val modifiedBy: String?,
)
