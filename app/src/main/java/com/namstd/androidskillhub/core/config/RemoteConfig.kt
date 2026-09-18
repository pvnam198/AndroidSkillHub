package com.namstd.androidskillhub.core.config

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.namstd.androidskillhub.BuildConfig
import com.namstd.androidskillhub.core.preferences.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Feature flags controlled from Firebase Remote Config. */
object RemoteConfig {
    private val remoteConfig = Firebase.remoteConfig
    private lateinit var prefs: AppPreferences

    private val _isReady = MutableStateFlow(false)

    /** True once [fetchAndActivate] has completed (success or failure) and flag reads below are settled. */
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    /**
     * [context] is used to seed [RemoteConfig] with the values persisted (in the app's shared
     * [AppPreferences] file) from the last successful fetch, so reads below return last-known-good
     * values immediately on this launch instead of the hardcoded defaults while [fetchAndActivate]
     * is still in flight.
     */
    fun initialize(context: Context) {
        prefs = AppPreferences.getInstance(context)
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            },
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_USE_CUSTOM_INTERSTITIAL to prefs.getCachedBoolean(KEY_USE_CUSTOM_INTERSTITIAL, false),
                KEY_USE_NATIVE_COLLAPSE_BANNER to prefs.getCachedBoolean(KEY_USE_NATIVE_COLLAPSE_BANNER, false),
                KEY_INTER_INTER_GAP_SECONDS to prefs.getCachedLong(KEY_INTER_INTER_GAP_SECONDS, 0L),
                KEY_OPEN_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, 0L),
                KEY_INTER_OPEN_GAP_SECONDS to prefs.getCachedLong(KEY_INTER_OPEN_GAP_SECONDS, 0L),
                KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT to prefs.getCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, 2L),
                KEY_POST_INTERSTITIAL_NATIVE_AD_DURATION_SECONDS to
                    prefs.getCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_DURATION_SECONDS, 5L),
            ),
        )
        remoteConfig.fetchAndActivate().addOnCompleteListener {
            persistToCache()
            _isReady.value = true
        }
    }

    private fun persistToCache() {
        prefs.putCachedBoolean(KEY_USE_CUSTOM_INTERSTITIAL, useCustomInterstitial)
        prefs.putCachedBoolean(KEY_USE_NATIVE_COLLAPSE_BANNER, useNativeCollapseBanner)
        prefs.putCachedLong(KEY_INTER_INTER_GAP_SECONDS, interInterGapMs / 1000)
        prefs.putCachedLong(KEY_OPEN_OPEN_GAP_SECONDS, openOpenGapMs / 1000)
        prefs.putCachedLong(KEY_INTER_OPEN_GAP_SECONDS, interOpenGapMs / 1000)
        prefs.putCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT, postInterstitialNativeAdCount.toLong())
        prefs.putCachedLong(KEY_POST_INTERSTITIAL_NATIVE_AD_DURATION_SECONDS, postInterstitialNativeAdDurationMs / 1000)
    }

    /** Whether [com.namstd.androidskillhub.core.ui.base.BaseActivity.showInterstitial] should run the interstitial+2-native-ad break. */
    val useCustomInterstitial: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_CUSTOM_INTERSTITIAL)

    /** Whether [com.namstd.androidskillhub.core.ads.BannerAds.load] should build a plain banner + toggleable native ad instead of AdMob's collapsible banner. */
    val useNativeCollapseBanner: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_NATIVE_COLLAPSE_BANNER)

    /** Minimum time between two Interstitial shows. See [com.namstd.androidskillhub.core.ads.AdGapGate]. */
    val interInterGapMs: Long
        get() = remoteConfig.getLong(KEY_INTER_INTER_GAP_SECONDS) * 1000

    /** Minimum time between two App Open shows. See [com.namstd.androidskillhub.core.ads.AdGapGate]. */
    val openOpenGapMs: Long
        get() = remoteConfig.getLong(KEY_OPEN_OPEN_GAP_SECONDS) * 1000

    /** Minimum time between an Interstitial show and an App Open show, either direction. */
    val interOpenGapMs: Long
        get() = remoteConfig.getLong(KEY_INTER_OPEN_GAP_SECONDS) * 1000

    /** How many sequential native ads show in the post-interstitial break. Clamped to 1-2 - [com.namstd.androidskillhub.feature.ads.PostInterstitialNativeAdDialogFragment] only supports a 2-stage flow. */
    val postInterstitialNativeAdCount: Int
        get() = remoteConfig.getLong(KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT).toInt().coerceIn(1, 2)

    /** How long each native ad stage stays up before its tap target (next/close) becomes available. */
    val postInterstitialNativeAdDurationMs: Long
        get() = remoteConfig.getLong(KEY_POST_INTERSTITIAL_NATIVE_AD_DURATION_SECONDS) * 1000

    private const val KEY_USE_CUSTOM_INTERSTITIAL = "use_custom_interstitial"
    private const val KEY_USE_NATIVE_COLLAPSE_BANNER = "use_native_collapse_banner"
    private const val KEY_INTER_INTER_GAP_SECONDS = "inter_inter_gap_seconds"
    private const val KEY_OPEN_OPEN_GAP_SECONDS = "open_open_gap_seconds"
    private const val KEY_INTER_OPEN_GAP_SECONDS = "inter_open_gap_seconds"
    private const val KEY_POST_INTERSTITIAL_NATIVE_AD_COUNT = "post_interstitial_native_ad_count"
    private const val KEY_POST_INTERSTITIAL_NATIVE_AD_DURATION_SECONDS = "post_interstitial_native_ad_duration_seconds"
}
