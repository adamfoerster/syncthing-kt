package com.adamfoerster.meshsync.desktop.core

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isTrue
import java.nio.file.Files
import kotlin.test.Test

class DesktopCoreConfigTest {
    @Test
    fun `api key and port are generated once and kept`() {
        val dir = Files.createTempDirectory("meshsync-config").toFile()
        try {
            val first = DesktopCoreConfig(dir)
            val second = DesktopCoreConfig(dir)

            assertThat(first.apiKey.length).isEqualTo(32)
            assertThat(first.port).isGreaterThan(1023)
            assertThat(second.apiKey).isEqualTo(first.apiKey)
            assertThat(second.port).isEqualTo(first.port)
            assertThat(second.baseUrl).isEqualTo("http://127.0.0.1:${first.port}")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `paused choice survives a restart of the app`() {
        val dir = Files.createTempDirectory("meshsync-config").toFile()
        try {
            DesktopCoreConfig(dir).stoppedByUser = true
            assertThat(DesktopCoreConfig(dir).stoppedByUser).isTrue()
        } finally {
            dir.deleteRecursively()
        }
    }
}
