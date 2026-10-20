package com.adamfoerster.syncthing.core.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.adamfoerster.syncthing.core.domain.preferences.ThemeMode
import kotlin.test.Test

class ThemeModeTest {

    @Test
    fun systemFollowsTheDevice() {
        assertThat(ThemeMode.System.isDark(systemDark = true)).isTrue()
        assertThat(ThemeMode.System.isDark(systemDark = false)).isFalse()
    }

    @Test
    fun anOverrideIgnoresTheDevice() {
        assertThat(ThemeMode.Light.isDark(systemDark = true)).isFalse()
        assertThat(ThemeMode.Dark.isDark(systemDark = false)).isTrue()
    }

    @Test
    fun storedNamesRoundTripAndUnknownOnesFollowTheDevice() {
        ThemeMode.entries.forEach { assertThat(ThemeMode.fromStored(it.name)).isEqualTo(it) }
        assertThat(ThemeMode.fromStored(null)).isEqualTo(ThemeMode.System)
        assertThat(ThemeMode.fromStored("sepia")).isEqualTo(ThemeMode.System)
    }
}
