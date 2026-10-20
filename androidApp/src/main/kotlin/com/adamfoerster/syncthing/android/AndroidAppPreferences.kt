package com.adamfoerster.syncthing.android

import android.content.Context
import android.content.res.Configuration
import com.adamfoerster.syncthing.core.domain.preferences.AppLanguages
import com.adamfoerster.syncthing.core.domain.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/** [AppPreferences] kept in SharedPreferences, which can be read synchronously before the UI exists. */
class AndroidAppPreferences(context: Context) : AppPreferences {
    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _autoAccept = MutableStateFlow(prefs.getBoolean(KEY_AUTO_ACCEPT, AppPreferences.DEFAULT_AUTO_ACCEPT_KNOWN_FOLDERS))
    private val _languageTag = MutableStateFlow(storedLanguageTag(context))

    override val autoAcceptKnownFolders: StateFlow<Boolean> = _autoAccept
    override val languageTag: StateFlow<String?> = _languageTag

    override fun setAutoAcceptKnownFolders(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ACCEPT, enabled).apply()
        _autoAccept.value = enabled
    }

    override fun setLanguageTag(tag: String?) {
        prefs.edit().apply { if (tag == null) remove(KEY_LANGUAGE) else putString(KEY_LANGUAGE, tag) }.apply()
        _languageTag.value = tag
    }

    companion object {
        private const val FILE = "app_preferences"
        private const val KEY_AUTO_ACCEPT = "auto_accept_known_folders"
        private const val KEY_LANGUAGE = "language_tag"

        /** The language the user picked in Settings, or null to follow the device. */
        fun storedLanguageTag(context: Context): String? =
            AppLanguages.sanitize(context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null))
    }
}

/** Applies the in-app language choice to a context (and to the JVM default, which Compose resources read). */
object LocaleSupport {
    /** [base] must be an unmodified system context so a null [tag] can restore the device language. */
    fun wrap(base: Context, tag: String?): Context {
        if (tag == null) {
            Locale.setDefault(base.resources.configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val configuration = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(configuration)
    }
}
