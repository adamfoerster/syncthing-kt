package com.adamfoerster.meshsync.core.domain.preferences

/** Decides when the "What’s new" popup opens by itself; Settings can always reopen it by hand. */
object ReleaseNotesPolicy {
    /**
     * Opens once per version: only after the introduction (a fresh install has nothing "new" to announce),
     * only when this version has notes, and not again once [lastSeen] equals [current].
     */
    fun shouldAutoShow(current: String, lastSeen: String?, onboardingCompleted: Boolean, hasNotes: Boolean): Boolean =
        onboardingCompleted && hasNotes && current.isNotEmpty() && lastSeen != current

    /** A fresh install (introduction not finished) records the current version so its first upgrade announces only what is new. */
    fun shouldMarkSeenSilently(current: String, lastSeen: String?, onboardingCompleted: Boolean): Boolean =
        !onboardingCompleted && current.isNotEmpty() && lastSeen != current
}
