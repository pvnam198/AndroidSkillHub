package com.namstd.androidskillhub.core.config

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.namstd.androidskillhub.BuildConfig

/** Feature flags controlled from Firebase Remote Config. */
object RemoteConfig {
    private val remoteConfig = Firebase.remoteConfig

    fun initialize() {
        remoteConfig.setConfigSettingsAsync(
            remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            },
        )
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_USE_CUSTOM_INTERSTITIAL to false,
                KEY_USE_NATIVE_COLLAPSE_BANNER to false,
            ),
        )
        remoteConfig.fetchAndActivate()
    }

    /** Whether [com.namstd.androidskillhub.core.ui.base.BaseActivity.showInterstitial] should run the interstitial+2-native-ad break. */
    val useCustomInterstitial: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_CUSTOM_INTERSTITIAL)

    /** Whether [com.namstd.androidskillhub.core.ads.BannerAds.load] should build a plain banner + toggleable native ad instead of AdMob's collapsible banner. */
    val useNativeCollapseBanner: Boolean
        get() = remoteConfig.getBoolean(KEY_USE_NATIVE_COLLAPSE_BANNER)

    private const val KEY_USE_CUSTOM_INTERSTITIAL = "use_custom_interstitial"
    private const val KEY_USE_NATIVE_COLLAPSE_BANNER = "use_native_collapse_banner"
}
