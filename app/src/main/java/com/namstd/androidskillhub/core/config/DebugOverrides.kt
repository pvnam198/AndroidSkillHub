package com.namstd.androidskillhub.core.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Local DEBUG-only overrides for Remote Config values, keyed by the RC key name and stored as raw
 * strings in their own prefs file (separate from [AppPreferences] so they never collide with the
 * fetch cache). An override, when set, wins over Firebase/default in [RemoteConfig]. No-op until [init].
 */
object DebugOverrides {
    private const val FILE_NAME = "debug_overrides"
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    }

    fun get(key: String): String? = prefs?.getString(key, null)

    fun set(key: String, value: String) {
        prefs?.edit { putString(key, value) }
    }

    fun remove(key: String) {
        prefs?.edit { remove(key) }
    }

    fun clear() {
        prefs?.edit { clear() }
    }
}
