package com.adamfoerster.meshsync.desktop

import com.adamfoerster.meshsync.core.domain.preferences.AppLanguages
import com.adamfoerster.meshsync.core.domain.preferences.AppPreferences
import com.adamfoerster.meshsync.core.domain.preferences.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Properties

/**
 * [AppPreferences] kept in `preferences.properties` under the app's data folder, read synchronously
 * so the language and theme are known before the first window. Same keys as the Android version.
 */
class DesktopAppPreferences(dataDir: File) : AppPreferences {
    private val file = File(dataDir, "preferences.properties")
    private val props = Properties()

    init {
        dataDir.mkdirs()
        if (file.exists()) file.inputStream().use(props::load)
    }

    private val _autoAccept = MutableStateFlow(
        props.getProperty(KEY_AUTO_ACCEPT)?.toBooleanStrictOrNull() ?: AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS,
    )
    private val _languageTag = MutableStateFlow(AppLanguages.sanitize(props.getProperty(KEY_LANGUAGE)))
    private val _themeMode = MutableStateFlow(ThemeMode.fromStored(props.getProperty(KEY_THEME)))
    private val _onboardingCompleted = MutableStateFlow(props.getProperty(KEY_ONBOARDING) == "true")

    override val autoAcceptKnownFolders: StateFlow<Boolean> = _autoAccept
    override val languageTag: StateFlow<String?> = _languageTag
    override val themeMode: StateFlow<ThemeMode> = _themeMode
    override val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted

    /** Whether the first-run "start with Windows" default was already applied; the user's later choice then wins. */
    var autostartInitialized: Boolean
        get() = props.getProperty(KEY_AUTOSTART_INIT) == "true"
        set(value) = put(KEY_AUTOSTART_INIT, value.toString())

    override fun setAutoAcceptKnownFolders(enabled: Boolean) {
        put(KEY_AUTO_ACCEPT, enabled.toString())
        _autoAccept.value = enabled
    }

    override fun setLanguageTag(tag: String?) {
        put(KEY_LANGUAGE, tag)
        _languageTag.value = tag
    }

    override fun setThemeMode(mode: ThemeMode) {
        put(KEY_THEME, mode.name)
        _themeMode.value = mode
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        put(KEY_ONBOARDING, completed.toString())
        _onboardingCompleted.value = completed
    }

    @Synchronized
    private fun put(key: String, value: String?) {
        if (value == null) props.remove(key) else props.setProperty(key, value)
        file.outputStream().use { props.store(it, "MeshSync preferences") }
    }

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_AUTO_ACCEPT = "auto_accept_known_folders"
        const val KEY_ONBOARDING = "onboarding_completed"
        const val KEY_LANGUAGE = "language_tag"
        const val KEY_AUTOSTART_INIT = "autostart_initialized"
    }
}
