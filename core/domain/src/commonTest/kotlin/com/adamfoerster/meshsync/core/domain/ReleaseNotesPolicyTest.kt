package com.adamfoerster.meshsync.core.domain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.preferences.ReleaseNotesPolicy
import kotlin.test.Test

class ReleaseNotesPolicyTest {

    @Test
    fun showsOnceAfterAnUpdate() {
        assertThat(ReleaseNotesPolicy.shouldAutoShow("1.18.0", lastSeen = "1.17.0", onboardingCompleted = true, hasNotes = true)).isTrue()
        assertThat(ReleaseNotesPolicy.shouldAutoShow("1.18.0", lastSeen = "1.18.0", onboardingCompleted = true, hasNotes = true)).isFalse()
    }

    @Test
    fun showsForAnExistingUserWhoNeverSawAny() {
        assertThat(ReleaseNotesPolicy.shouldAutoShow("1.18.0", lastSeen = null, onboardingCompleted = true, hasNotes = true)).isTrue()
    }

    @Test
    fun staysClosedWithoutNotesOrWithoutAKnownVersion() {
        assertThat(ReleaseNotesPolicy.shouldAutoShow("1.18.0", null, onboardingCompleted = true, hasNotes = false)).isFalse()
        assertThat(ReleaseNotesPolicy.shouldAutoShow("", null, onboardingCompleted = true, hasNotes = true)).isFalse()
    }

    @Test
    fun aFreshInstallDoesNotShowAndRecordsTheVersion() {
        assertThat(ReleaseNotesPolicy.shouldAutoShow("1.18.0", null, onboardingCompleted = false, hasNotes = true)).isFalse()
        assertThat(ReleaseNotesPolicy.shouldMarkSeenSilently("1.18.0", null, onboardingCompleted = false)).isTrue()
        assertThat(ReleaseNotesPolicy.shouldMarkSeenSilently("1.18.0", "1.18.0", onboardingCompleted = false)).isFalse()
        assertThat(ReleaseNotesPolicy.shouldMarkSeenSilently("1.18.0", null, onboardingCompleted = true)).isFalse()
    }
}
