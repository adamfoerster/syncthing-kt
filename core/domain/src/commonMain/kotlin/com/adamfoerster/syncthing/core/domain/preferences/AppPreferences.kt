package com.adamfoerster.syncthing.core.domain.preferences

import kotlinx.coroutines.flow.StateFlow

/** App-level choices the user makes in Settings; stored on the device, never sent to the Syncthing core. */
interface AppPreferences {
    /** When on, a known device offering a folder we already share is added to it without asking. */
    val autoAcceptKnownFolders: StateFlow<Boolean>

    /** BCP 47 tag from [AppLanguages.supported] that overrides the device language; null follows the device. */
    val languageTag: StateFlow<String?>

    fun setAutoAcceptKnownFolders(enabled: Boolean)
    fun setLanguageTag(tag: String?)

    companion object {
        const val DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS = true
    }
}

/** The languages the interface is translated to (English first, it is the fallback). */
object AppLanguages {
    val supported: List<String> = listOf("en", "pt", "es", "it", "de", "fr", "el")

    /** Drops tags we do not translate, so a stale or hand-edited value falls back to the device language. */
    fun sanitize(tag: String?): String? = tag?.takeIf { it in supported }
}
