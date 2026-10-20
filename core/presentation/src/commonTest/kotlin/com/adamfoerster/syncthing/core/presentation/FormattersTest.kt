package com.adamfoerster.syncthing.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class FormattersTest {
    @Test
    fun `formatBytes uses the given decimal separator`() {
        val bytes = 4_500_000_000
        assertThat(formatBytes(bytes, decimalSeparator = ',')).isEqualTo("4,2 GB")
        assertThat(formatBytes(bytes, decimalSeparator = '.')).isEqualTo("4.2 GB")
    }

    @Test
    fun `formatBytes has no separator for whole values`() {
        assertThat(formatBytes(312L * 1024 * 1024, decimalSeparator = '.')).isEqualTo("312 MB")
        assertThat(formatBytes(0, decimalSeparator = ',')).isEqualTo("0 B")
    }
}
