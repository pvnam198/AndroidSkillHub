package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.databinding.AdBannerShimmerBinding

object BannerAds {
    /**
     * Loads a banner into [container]. When [collapsible] is true, Remote Config picks one of two
     * variants: AdMob's own collapsible banner, or - if [RemoteConfig.useNativeCollapsible] is on -
     * the native collapsible ([NativeCollapseAd]: one native ad that opens large over the screen and
     * collapses into [container] as a small strip). This is the single entry point for both so every
     * screen's [collapsible] slot gets the swap for free. Pure routing - the actual loads live in
     * [loadAdMobBanner] and [NativeCollapseAd].
     */
    fun load(
        activity: Activity,
        container: ViewGroup,
        collapsible: Boolean = false,
        onStatus: (String) -> Unit = {},
    ): AdHandle {
        if (collapsible && RemoteConfig.useNativeCollapsible) {
            return NativeCollapseAd(activity, container, onStatus = onStatus).start()
        }
        return loadAdMobBanner(activity, container, collapsible, onStatus)
    }

    /** Loads a real AdMob banner into [container] - plain, or AdMob's own collapsible variant when [collapsible]. */
    private fun loadAdMobBanner(
        activity: Activity,
        container: ViewGroup,
        collapsible: Boolean,
        onStatus: (String) -> Unit,
    ): AdHandle {
        if (!Ads.isReady || !Ads.adsEnabled) {
            onStatus("Banner: SDK not ready")
            return AdHandle {}
        }
        return AdMobBannerAd(activity, container, collapsible, onStatus).also { it.load(isReload = false) }
    }
}

/**
 * One AdMob banner slot in [container]. Each load - the first one and every auto reload (see
 * [AutoReloadTimer]) - drops the previous banner and puts the slot back to its small shimmer
 * straight away, then swaps in the new banner once it's loaded; for [collapsible] that new banner
 * is requested collapsible again, so it opens expanded. On a reload the shimmer stays up at least
 * [AutoReloadTimer.MIN_RELOAD_SHIMMER_MS], so the swap never looks like a flicker. A banner loaded
 * while the activity isn't resumed waits for onResume, so the reload gap - which starts when the
 * banner is shown - only ever counts time it was actually on screen. Main thread only, except the
 * SDK callbacks, which hop back via [Activity.runOnUiThread].
 */
private class AdMobBannerAd(
    private val activity: Activity,
    private val container: ViewGroup,
    private val collapsible: Boolean,
    private val onStatus: (String) -> Unit,
) : AdHandle {
    private val handler = Handler(Looper.getMainLooper())
    private val reloadTimer = AutoReloadTimer(activity) { load(isReload = true) }
    private var current: AdView? = null
    /** A loaded banner waiting to be shown - for the reload's minimum shimmer, or for onResume. */
    private var pendingView: AdView? = null
    private var minShimmerUntil = 0L
    private val revealRunnable = Runnable { revealPending() }
    private val lifecycle: Lifecycle? = (activity as? LifecycleOwner)?.lifecycle
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            revealPending()
        }
    }
    private var shimmer: AdBannerShimmerBinding? = null
    /** Bumped on every load; a callback from an older load finds it changed and bows out. */
    @Volatile private var generation = 0
    @Volatile private var destroyed = false

    init {
        lifecycle?.addObserver(lifecycleObserver)
    }

    fun load(isReload: Boolean) {
        if (destroyed) return
        val gen = ++generation
        cancelPendingReveal()
        current?.destroy()
        current = null
        showShimmer()
        minShimmerUntil = if (isReload) SystemClock.elapsedRealtime() + AutoReloadTimer.MIN_RELOAD_SHIMMER_MS else 0L
        fun isStale() = destroyed || gen != generation

        val widthPixels = if (container.width > 0) container.width else activity.resources.displayMetrics.widthPixels
        val widthDp = (widthPixels / activity.resources.displayMetrics.density).toInt().coerceAtLeast(320)
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
        val ids = if (collapsible) Ads.ids.bannerCollapsible else Ads.ids.banner
        WaterfallLoader<AdView>(ids).load(
            attempt = { id, loaded, failed ->
                if (!isStale()) onStatus("Banner: loading $id")
                val adView = AdView(activity)
                val request = BannerAdRequest.Builder(id, size).apply {
                    if (collapsible) setGoogleExtrasBundle(Bundle().apply { putString("collapsible", "bottom") })
                }.build()
                adView.loadAd(
                    request,
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) {
                            ad.adEventCallback = object : BannerAdEventCallback {
                                override fun onAdPaid(adValue: AdValue) = AdjustRevenueLogger.log(adValue)
                            }
                            loaded(adView)
                        }
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            adView.destroy()
                            if (!isStale()) failed(adError.message)
                        }
                    },
                )
            },
            onLoaded = { adView, _ ->
                activity.runOnUiThread {
                    if (isStale()) {
                        adView.destroy()
                        return@runOnUiThread
                    }
                    pendingView = adView
                    revealPending()
                }
            },
            onFailed = { error ->
                activity.runOnUiThread {
                    if (isStale()) return@runOnUiThread
                    onStatus("Banner: $error")
                    clearShimmer()
                    reloadTimer.schedule()
                }
            },
        )
    }

    /** Shows [pendingView] once the minimum shimmer is over and the activity is resumed. */
    private fun revealPending() {
        val adView = pendingView ?: return
        handler.removeCallbacks(revealRunnable)
        val wait = minShimmerUntil - SystemClock.elapsedRealtime()
        if (wait > 0) {
            handler.postDelayed(revealRunnable, wait)
            return
        }
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == false) return // onResume calls back in.
        pendingView = null
        show(adView)
    }

    /** The banner is on screen from here - so this, not the load, is where the reload gap starts. */
    private fun show(adView: AdView) {
        clearShimmer()
        current = adView
        container.removeAllViews()
        container.addView(adView)
        onStatus("Banner: ready")
        reloadTimer.schedule()
    }

    private fun cancelPendingReveal() {
        handler.removeCallbacks(revealRunnable)
        pendingView?.destroy()
        pendingView = null
    }

    /** Shows a shimmering skeleton in [container] so the banner slot never pops in from empty. */
    private fun showShimmer() {
        val binding = AdBannerShimmerBinding.inflate(LayoutInflater.from(activity))
        container.removeAllViews()
        container.addView(binding.root)
        binding.root.startShimmer()
        shimmer = binding
    }

    private fun clearShimmer() {
        shimmer?.let { it.root.stopShimmer(); container.removeView(it.root) }
        shimmer = null
    }

    override fun destroy() {
        destroyed = true
        activity.runOnUiThread {
            reloadTimer.cancel()
            lifecycle?.removeObserver(lifecycleObserver)
            cancelPendingReveal()
            current?.destroy()
            current = null
            clearShimmer()
            container.removeAllViews()
        }
    }
}
