package com.namstd.androidskillhub.feature.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.ads.InterstitialAds
import com.namstd.androidskillhub.core.ads.InterstitialState
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseActivity
import com.namstd.androidskillhub.databinding.ActivitySplashBinding
import com.namstd.androidskillhub.feature.language.LanguageActivity
import com.namstd.androidskillhub.feature.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    companion object {
        private val MIN_DELAY_MS = 3_000L.milliseconds
        private val MAX_DELAY_MS = 8_000L.milliseconds
        private val CHECK_INTERVAL_MS = 2_00L.milliseconds
    }

    override fun inflateBinding(inflater: LayoutInflater): ActivitySplashBinding =
        ActivitySplashBinding.inflate(inflater)

    override fun initViews() {
        if (prefs.isPremium) {
            binding.bannerContainer.isVisible = false
            lifecycleScope.launch {
                delay(MIN_DELAY_MS)
                showInterstitial(::navigateToMain)
            }
            return
        }

        Ads.initialize(this) { consentGranted ->
            runOnUiThread {
                if (consentGranted) {
                    loadBanner(binding.bannerContainer)
                    InterstitialAds.load()
                    NativeAds.preload(NativePlacement.LANGUAGE)
                    lifecycleScope.launch { waitForAdThenNavigate() }
                }
            }
        }
    }

    private suspend fun waitForAdThenNavigate() {
        delay(MIN_DELAY_MS)
        var waitedMs = MIN_DELAY_MS
        while (InterstitialAds.state.value != InterstitialState.READY && waitedMs < MAX_DELAY_MS) {
            delay(CHECK_INTERVAL_MS)
            waitedMs += CHECK_INTERVAL_MS
        }
        showInterstitial(::navigateToMain)
    }

    private fun navigateToMain() {
        if (isFinishing) return
        val next = if (prefs.setupCompleted) MainActivity::class.java else LanguageActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

}
