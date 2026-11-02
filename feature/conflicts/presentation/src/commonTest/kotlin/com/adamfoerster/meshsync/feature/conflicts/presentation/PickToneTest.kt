package com.adamfoerster.meshsync.feature.conflicts.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.adamfoerster.meshsync.core.designsystem.components.Tone
import com.adamfoerster.meshsync.feature.conflicts.presentation.text.Pick
import com.adamfoerster.meshsync.feature.conflicts.presentation.text.tone
import kotlin.test.Test

class PickToneTest {

    @Test
    fun pickButtonsUseTheColorOfTheSideTheyKeep() {
        // The first side is drawn green (Ok) and the second blue (Info).
        assertThat(Pick.FIRST.tone).isEqualTo(Tone.Ok)
        assertThat(Pick.SECOND.tone).isEqualTo(Tone.Info)
    }

    @Test
    fun sidesStayDistinguishableWhenBothAuthorsShareTheSameLabel() {
        assertThat(Pick.FIRST.tone).isNotEqualTo(Pick.SECOND.tone)
    }

    @Test
    fun keepingBothUsesTheNeutralAccent() {
        assertThat(Pick.BOTH.tone).isEqualTo(Tone.Accent)
    }
}
