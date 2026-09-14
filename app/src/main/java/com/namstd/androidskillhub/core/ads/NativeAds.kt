package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.namstd.androidskillhub.databinding.AdNativeBinding
import com.namstd.androidskillhub.databinding.AdNativeShimmerBinding

enum class NativePlacement {
    LANGUAGE, ONBOARDING_1, ONBOARDING_2, ONBOARDING_3, ONBOARDING_AD, HOME_FEED, POST_INTERSTITIAL,

    /** Shared app-wide slot for [BannerAds]' custom collapse banner - one placement for every collapsible banner call site. */
    COLLAPSE_BANNER,
}

/**
 * Per-placement pool of one [NativeAd]: a subscriber [take]s the ad to show it in its own view,
 * and [release]s it back once done so a later subscriber for the same placement can reuse it.
 * An ad is never handed to two subscribers at the same time, since a [NativeAd] can only be
 * registered to one [NativeAdView] at once.
 */
private class NativeAdSlot {
    private class Entry(val ad: NativeAd, val loadedAt: Long)

    private var freeAd: Entry? = null
    private var loading = false
    private var pendingCallback: ((NativeAd) -> Unit)? = null

    /** Removes and returns the free cached ad if one exists and isn't older than [ttlMs]. */
    @Synchronized
    fun take(ttlMs: Long): NativeAd? {
        val entry = freeAd ?: return null
        freeAd = null
        if (System.currentTimeMillis() - entry.loadedAt >= ttlMs) {
            entry.ad.destroy()
            return null
        }
        return entry.ad
    }

    /** Returns [ad] to the pool so the next subscriber can reuse it instead of reloading. */
    @Synchronized
    fun release(ad: NativeAd) {
        freeAd = Entry(ad, System.currentTimeMillis())
    }

    /**
     * Registers [callback] for the next ad to finish loading, or, if null, just ensures a load is
     * running so the result can be preloaded into [freeAd] for whoever asks next. Returns whether
     * to actually start a network load.
     */
    @Synchronized
    fun requestLoad(callback: ((NativeAd) -> Unit)?): Boolean {
        if (callback != null) pendingCallback = callback
        if (loading) return false
        loading = true
        return true
    }

    /** Whether a cached ad is sitting free and not yet expired, without consuming it. */
    @Synchronized
    fun hasFresh(ttlMs: Long): Boolean {
        val entry = freeAd ?: return false
        return System.currentTimeMillis() - entry.loadedAt < ttlMs
    }

    /** Un-registers [callback] if it's still the one waiting, so a late result isn't delivered to a gone view. */
    @Synchronized
    fun cancelLoad(callback: (NativeAd) -> Unit) {
        if (pendingCallback === callback) pendingCallback = null
    }

    /** Returns the callback to notify, or null if nobody is waiting (in which case [ad] is cached as free). */
    @Synchronized
    fun onAdLoaded(ad: NativeAd): ((NativeAd) -> Unit)? {
        loading = false
        val callback = pendingCallback
        pendingCallback = null
        if (callback == null) freeAd = Entry(ad, System.currentTimeMillis())
        return callback
    }

    @Synchronized
    fun onLoadFailed() {
        loading = false
        pendingCallback = null
    }

    @Synchronized
    fun clear() {
        freeAd?.ad?.destroy()
        freeAd = null
        loading = false
        pendingCallback = null
    }
}

/** Native ads, cached per placement so a screen doesn't reload one every time it's shown. */
object NativeAds {
    private val slots = mutableMapOf<NativePlacement, NativeAdSlot>()

    @Synchronized
    private fun slotFor(placement: NativePlacement) = slots.getOrPut(placement) { NativeAdSlot() }

    /** Starts [placement]'s waterfall now, ahead of any screen needing it, if not already loading or cached. */
    fun preload(placement: NativePlacement) {
        if (!Ads.adsEnabled || !Ads.isReady) return
        val slot = slotFor(placement)
        if (slot.hasFresh(TTL_MS)) return
        startLoad(placement, slot, onReady = null)
    }

    /**
     * Takes the preloaded ad for [placement], if one is ready, for the caller to display and
     * destroy on its own (no container/shimmer management, unlike [subscribe]) - for one-shot
     * full-screen placements that need the ad to already be sitting there with no load wait.
     * Immediately kicks off a reload so the slot is refilled for next time. Returns null if
     * nothing was preloaded in time; treat that the same as a no-fill.
     */
    fun poll(placement: NativePlacement): NativeAd? {
        if (!Ads.adsEnabled || !Ads.isReady) return null
        val ad = slotFor(placement).take(TTL_MS)
        preload(placement)
        return ad
    }

    fun subscribe(
        activity: Activity,
        placement: NativePlacement,
        container: FrameLayout,
        render: (Activity, NativeAd) -> NativeAdView = ::renderDefaultCard,
    ): AdHandle {
        if (!Ads.adsEnabled) return AdHandle {}
        if (!Ads.isReady) return AdHandle {}
        val slot = slotFor(placement)

        var active = true
        var currentView: NativeAdView? = null
        var attachedAd: NativeAd? = null
        var shimmer: AdNativeShimmerBinding? = showShimmer(activity, container)

        val onReady: (NativeAd) -> Unit = { ad ->
            activity.runOnUiThread {
                if (!active) {
                    slot.release(ad)
                    return@runOnUiThread
                }
                shimmer?.let { it.root.stopShimmer(); container.removeView(it.root) }
                shimmer = null
                val view = render(activity, ad)
                container.removeAllViews()
                view.alpha = 0f
                container.addView(view)
                view.animate().alpha(1f).setDuration(FADE_IN_MS).start()
                currentView = view
                attachedAd = ad
            }
        }

        slot.take(TTL_MS)?.let(onReady) ?: startLoad(placement, slot, onReady)

        return AdHandle {
            active = false
            slot.cancelLoad(onReady)
            activity.runOnUiThread {
                currentView?.destroy()
                currentView = null
                shimmer?.let { it.root.stopShimmer(); container.removeView(it.root) }
                shimmer = null
                container.removeAllViews()
            }
            attachedAd?.let(slot::release)
            attachedAd = null
        }
    }

    /** Shows a shimmering skeleton in [container] so the ad slot never pops in from empty. */
    private fun showShimmer(activity: Activity, container: FrameLayout): AdNativeShimmerBinding {
        val binding = AdNativeShimmerBinding.inflate(LayoutInflater.from(activity))
        container.removeAllViews()
        container.addView(binding.root)
        binding.root.startShimmer()
        return binding
    }

    private fun startLoad(placement: NativePlacement, slot: NativeAdSlot, onReady: ((NativeAd) -> Unit)?) {
        if (!slot.requestLoad(onReady)) return
        WaterfallLoader<NativeAd>(Ads.ids.native[placement].orEmpty()).load(
            attempt = { id, loaded, failed ->
                val request = NativeAdRequest.Builder(id, listOf(NativeAd.NativeAdType.NATIVE)).build()
                NativeAdLoader.load(request, object : NativeAdLoaderCallback {
                    override fun onNativeAdLoaded(nativeAd: NativeAd) = loaded(nativeAd)
                    override fun onAdFailedToLoad(adError: LoadAdError) = failed(adError.message)
                })
            },
            onLoaded = { ad, _ -> slot.onAdLoaded(ad)?.invoke(ad) },
            onFailed = { slot.onLoadFailed() },
        )
    }

    private fun renderDefaultCard(activity: Activity, ad: NativeAd): NativeAdView {
        val binding = AdNativeBinding.inflate(LayoutInflater.from(activity))
        populate(ad, binding)
        return binding.root
    }

    private fun populate(ad: NativeAd, binding: AdNativeBinding) {
        val view = binding.root
        view.headlineView = binding.adHeadline
        view.bodyView = binding.adBody
        view.advertiserView = binding.adAdvertiser
        view.iconView = binding.adIcon
        view.callToActionView = binding.adCallToAction
        binding.adHeadline.text = ad.headline
        binding.adBody.text = ad.body
        binding.adAdvertiser.text = ad.advertiser
        binding.adCallToAction.text = ad.callToAction
        binding.adIcon.setImageDrawable(ad.icon?.drawable)
        binding.adBody.visibility = visibility(ad.body)
        binding.adAdvertiser.visibility = visibility(ad.advertiser)
        binding.adCallToAction.visibility = visibility(ad.callToAction)
        binding.adIcon.visibility = visibility(ad.icon)
        view.registerNativeAd(ad, binding.adMedia)
    }

    private fun visibility(value: Any?) = if (value == null) View.GONE else View.VISIBLE

    @Synchronized
    fun clear() {
        slots.values.forEach { it.clear() }
        slots.clear()
    }

    private const val TTL_MS = 60L * 60 * 1000
    private const val FADE_IN_MS = 200L
}
