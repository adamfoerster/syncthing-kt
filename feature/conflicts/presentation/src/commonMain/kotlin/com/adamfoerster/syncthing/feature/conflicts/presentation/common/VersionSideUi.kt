package com.adamfoerster.syncthing.feature.conflicts.presentation.common

import com.adamfoerster.syncthing.core.presentation.UiText
import com.adamfoerster.syncthing.core.presentation.displayName
import com.adamfoerster.syncthing.core.presentation.formatBytes
import com.adamfoerster.syncthing.core.presentation.formatClock
import com.adamfoerster.syncthing.core.presentation.initialOf
import com.adamfoerster.syncthing.core.presentation.toDayAndTimeUiText
import com.adamfoerster.syncthing.feature.conflicts.domain.model.Conflict
import com.adamfoerster.syncthing.feature.conflicts.domain.model.ConflictVersion
import com.adamfoerster.syncthing.feature.conflicts.domain.model.VersionSide

/** One version as the screens show it: who made it and when. */
data class VersionUi(
    val side: VersionSide,
    val author: UiText,
    /** "N" for "Notebook da Ana"; null for this phone, which shows the phone glyph instead. */
    val initial: String,
    val isLocal: Boolean,
    val clock: String?,
    val dayAndTime: UiText?,
    val size: String,
)

/**
 * The resolvers always put this phone's version first (green) and the other device's second (blue),
 * whichever of them Syncthing left at the original name.
 */
fun Conflict.orderedVersions(): Pair<ConflictVersion, ConflictVersion> =
    if (copy.author?.isLocal == true && current.author?.isLocal != true) copy to current else current to copy

fun ConflictVersion.toUi(): VersionUi = VersionUi(
    side = side,
    author = author.displayName(),
    initial = initialOf(author?.name ?: "?"),
    isLocal = author?.isLocal == true,
    clock = modified?.formatClock(),
    dayAndTime = modified?.toDayAndTimeUiText(),
    size = formatBytes(size),
)
