package com.adamfoerster.meshsync.desktop

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.adamfoerster.meshsync.core.domain.preferences.AppPreferences
import com.adamfoerster.meshsync.core.domain.preferences.ThemeMode
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DesktopAppPreferencesTest {
    private lateinit var dir: File

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("meshsync-prefs").toFile()
    }

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `defaults for a fresh install`() {
        val prefs = DesktopAppPreferences(dir)
        assertThat(prefs.onboardingCompleted.value).isFalse()
        assertThat(prefs.autoAcceptKnownFolders.value).isEqualTo(AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS)
        assertThat(prefs.languageTag.value).isNull()
        assertThat(prefs.themeMode.value).isEqualTo(ThemeMode.System)
        assertThat(prefs.autostartInitialized).isFalse()
    }

    @Test
    fun `choices survive a restart`() {
        DesktopAppPreferences(dir).apply {
            setOnboardingCompleted(true)
            setAutoAcceptKnownFolders(false)
            setLanguageTag("pt")
            setThemeMode(ThemeMode.Dark)
            autostartInitialized = true
        }

        val reloaded = DesktopAppPreferences(dir)
        assertThat(reloaded.onboardingCompleted.value).isTrue()
        assertThat(reloaded.autoAcceptKnownFolders.value).isFalse()
        assertThat(reloaded.languageTag.value).isEqualTo("pt")
        assertThat(reloaded.themeMode.value).isEqualTo(ThemeMode.Dark)
        assertThat(reloaded.autostartInitialized).isTrue()
    }

    @Test
    fun `clearing the language goes back to the system language`() {
        val prefs = DesktopAppPreferences(dir)
        prefs.setLanguageTag("de")
        prefs.setLanguageTag(null)
        assertThat(DesktopAppPreferences(dir).languageTag.value).isNull()
    }

    @Test
    fun `an unsupported stored language is ignored`() {
        File(dir, "preferences.properties").writeText("language_tag=xx\n")
        assertThat(DesktopAppPreferences(dir).languageTag.value).isNull()
    }
}
