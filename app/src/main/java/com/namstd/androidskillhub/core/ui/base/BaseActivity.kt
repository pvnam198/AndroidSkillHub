package com.namstd.androidskillhub.core.ui.base

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.namstd.androidskillhub.core.ads.AdHandle
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.ads.BannerAds
import com.namstd.androidskillhub.core.ads.FullScreenGate
import com.namstd.androidskillhub.core.ads.InterstitialAds
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.core.preferences.AppPreferences
import com.namstd.androidskillhub.feature.ads.PostInterstitialNativeAdDialogFragment

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    protected val prefs: AppPreferences get() = AppPreferences.getInstance(this)

    private var bannerHandle: AdHandle? = null
    private var nativeHandle: AdHandle? = null

    /** Loads a banner into [container] and destroys it automatically in [onDestroy]. */
    protected fun loadBanner(container: ViewGroup, collapsible: Boolean = false) {
        container.post { bannerHandle = BannerAds.load(this, container, collapsible) }
    }

    /**
     * Loads a native ad for [placement] into [container] and destroys it automatically in
     * [onDestroy]. See [NativeAds.subscribe] for [preloadOnShow].
     */
    protected fun loadNative(container: FrameLayout, placement: NativePlacement, preloadOnShow: Boolean = false) {
        container.post { nativeHandle = NativeAds.subscribe(this, placement, container, preloadOnShow) }
    }

    /**
     * Interstitial, or "custom interstitial" (+2 native ad break) if enabled via Remote Config, then runs [onComplete].
     * Single implementation for the whole app - [BaseFragment.showInterstitial] delegates here instead of duplicating it.
     */
    internal fun showInterstitial(onComplete: () -> Unit) {
        if (!RemoteConfig.useCustomInterstitial) {
            if (!InterstitialAds.show(this, onComplete = onComplete)) onComplete()
            return
        }
        if (!Ads.adsEnabled || FullScreenGate.isBusy()) { onComplete(); return }
        NativeAds.preload(NativePlacement.POST_INTERSTITIAL)
        val afterInterstitial = { runNativeAdBreak(onComplete) }
        if (!InterstitialAds.show(this, onComplete = afterInterstitial)) afterInterstitial()
    }

    private fun runNativeAdBreak(onComplete: () -> Unit) {
        if (isFinishing || isDestroyed) return
        supportFragmentManager.setFragmentResultListener(
            PostInterstitialNativeAdDialogFragment.REQUEST_KEY, this,
        ) { _, _ -> onComplete() }
        PostInterstitialNativeAdDialogFragment().show(supportFragmentManager, PostInterstitialNativeAdDialogFragment.TAG)
    }

    protected abstract fun inflateBinding(inflater: LayoutInflater): VB

    protected open fun initConfig(savedInstanceState: Bundle?) = Unit

    protected open fun initViews() = Unit

    protected open fun initListeners() = Unit

    protected open fun observeData() = Unit

    protected open fun releaseResources() = Unit

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        applySystemBarPadding(binding.root)
        initConfig(savedInstanceState)
        initViews()
        initListeners()
        observeData()
    }

    /** Pads the root view so content isn't drawn under the status/navigation bars (edge-to-edge is enforced from SDK 35). */
    private fun applySystemBarPadding(root: View) {
        val initialPadding = Rect(root.paddingLeft, root.paddingTop, root.paddingRight, root.paddingBottom)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialPadding.left + bars.left,
                top = initialPadding.top + bars.top,
                right = initialPadding.right + bars.right,
                bottom = initialPadding.bottom + bars.bottom,
            )
            insets
        }
    }

    final override fun onDestroy() {
        try {
            releaseResources()
        } finally {
            bannerHandle?.destroy()
            bannerHandle = null
            nativeHandle?.destroy()
            nativeHandle = null
            super.onDestroy()
        }
    }
}
