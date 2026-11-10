package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class SingleInstanceTest {
    @Test
    fun `second instance is refused and asks the first to show itself`() {
        val dir = Files.createTempDirectory("meshsync-instance").toFile()
        val shown = CountDownLatch(1)
        val first = SingleInstance(dir) { shown.countDown() }
        val second = SingleInstance(dir) { error("the second instance must not serve") }
        try {
            assertThat(first.acquire()).isTrue()
            assertThat(second.acquire()).isFalse()
            assertThat(shown.await(5, TimeUnit.SECONDS)).isTrue()
        } finally {
            second.close()
            first.close()
            dir.deleteRecursively()
        }
    }

    @Test
    fun `can be acquired again after the first instance quits`() {
        val dir = Files.createTempDirectory("meshsync-instance").toFile()
        try {
            val first = SingleInstance(dir) {}
            assertThat(first.acquire()).isTrue()
            first.close()

            val again = SingleInstance(dir) {}
            assertThat(again.acquire()).isEqualTo(true)
            again.close()
        } finally {
            dir.deleteRecursively()
        }
    }
}
