package com.namstd.androidskillhub.core.preferences

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.namstd.androidskillhub.core.language.AppLanguage
import androidx.core.content.edit

class AppPreferences private constructor(context: Context) {
    private val values = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var selectedLanguage: String
        get() = values.getString(KEY_LANGUAGE, AppLanguage.default.code) ?: AppLanguage.default.code
        set(value) {
            requireNotNull(AppLanguage.fromCode(value)) { "Unsupported language code: $value" }
            values.edit { putString(KEY_LANGUAGE, value) }
        }

    var setupCompleted: Boolean
        get() = values.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = values.edit { putBoolean(KEY_SETUP_COMPLETE, value) }

    var isPremium: Boolean
        get() = values.getBoolean(KEY_PREMIUM, false)
        set(value) = values.edit { putBoolean(KEY_PREMIUM, value) }

    fun applyLocale(language: String = selectedLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    /** Generic cache slot for other singletons (e.g. [com.namstd.androidskillhub.core.config.RemoteConfig]) to persist values by key into this same shared prefs file. */
    fun getCachedBoolean(key: String, default: Boolean): Boolean = values.getBoolean(key, default)

    fun putCachedBoolean(key: String, value: Boolean) {
        values.edit { putBoolean(key, value) }
    }

    fun getCachedLong(key: String, default: Long): Long = values.getLong(key, default)

    fun putCachedLong(key: String, value: Long) {
        values.edit { putLong(key, value) }
    }

    fun resetOnboarding() {
        setupCompleted = false
    }

    companion object {
        private const val FILE_NAME = "app_preferences"
        private const val KEY_LANGUAGE = "selected_language"
        private const val KEY_SETUP_COMPLETE = "setup_completed"
        private const val KEY_PREMIUM = "is_premium"

        @Volatile private var instance: AppPreferences? = null

        fun getInstance(context: Context): AppPreferences =
            instance ?: synchronized(this) {
                instance ?: AppPreferences(context.applicationContext).also { instance = it }
            }
    }
}
