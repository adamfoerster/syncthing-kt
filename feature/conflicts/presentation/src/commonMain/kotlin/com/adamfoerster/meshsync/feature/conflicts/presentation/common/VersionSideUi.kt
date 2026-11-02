package com.adamfoerster.meshsync.feature.conflicts.presentation.common

import com.adamfoerster.meshsync.core.presentation.UiText
import com.adamfoerster.meshsync.core.presentation.displayName
import com.adamfoerster.meshsync.core.presentation.formatBytes
import com.adamfoerster.meshsync.core.presentation.formatClock
import com.adamfoerster.meshsync.core.presentation.initialOf
import com.adamfoerster.meshsync.core.presentation.toDayAndTimeUiText
import com.adamfoerster.meshsync.feature.conflicts.domain.merge.TextDifference
import com.adamfoerster.meshsync.feature.conflicts.domain.model.Conflict
import com.adamfoerster.meshsync.feature.conflicts.domain.model.ConflictVersion
import com.adamfoerster.meshsync.feature.conflicts.domain.model.VersionSide
import com.adamfoerster.meshsync.feature.conflicts.presentation.resources.Res
import com.adamfoerster.meshsync.feature.conflicts.presentation.resources.difference_final_newline
import com.adamfoerster.meshsync.feature.conflicts.presentation.resources.difference_line_endings
import com.adamfoerster.meshsync.feature.conflicts.presentation.resources.difference_several

/** Why two texts with no differing passages are still different files; null when there is nothing to say. */
fun Set<TextDifference>.toUiText(): UiText? = when {
    isEmpty() -> null
    size > 1 -> UiText.Resource(Res.string.difference_several)
    else -> UiText.Resource(
        when (first()) {
            TextDifference.LINE_ENDINGS -> Res.string.difference_line_endings
            TextDifference.FINAL_NEWLINE -> Res.string.difference_final_newline
        },
    )
}

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
