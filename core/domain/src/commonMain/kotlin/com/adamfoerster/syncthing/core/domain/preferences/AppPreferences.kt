package com.adamfoerster.syncthing.core.domain.preferences

import kotlinx.coroutines.flow.StateFlow

/** App-level choices the user makes in Settings; stored on the device, never sent to the Syncthing core. */
interface AppPreferences {
    /** When on, a known device offering a folder we already share is added to it without asking. */
    val autoAcceptKnownFolders: StateFlow<Boolean>

    /** BCP 47 tag from [AppLanguages.supported] that overrides the device language; null follows the device. */
    val languageTag: StateFlow<String?>

    /** Light/dark choice; [ThemeMode.System] follows the device. */
    val themeMode: StateFlow<ThemeMode>

    fun setAutoAcceptKnownFolders(enabled: Boolean)
    fun setLanguageTag(tag: String?)
    fun setThemeMode(mode: ThemeMode)

    companion object {
        const val DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS = true
    }
}

/** Which palette the app uses; [System] is the default and tracks the device's light/dark setting. */
enum class ThemeMode {
    System,
    Light,
    Dark;

    /** [systemDark] is whether the device is currently in dark mode; it only matters for [System]. */
    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        /** Parses a stored name, falling back to [System] for a missing or unknown value. */
        fun fromStored(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: System
    }
}

/** The languages the interface is translated to (English first, it is the fallback). */
object AppLanguages {
    val supported: List<String> = listOf("en", "pt", "es", "it", "de", "fr", "el")

    /** Drops tags we do not translate, so a stale or hand-edited value falls back to the device language. */
    fun sanitize(tag: String?): String? = tag?.takeIf { it in supported }
}
