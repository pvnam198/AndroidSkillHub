package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.databinding.AdBannerShimmerBinding
import com.namstd.androidskillhub.databinding.AdNativeCollapseBinding

object BannerAds {
    /**
     * Loads a banner into [container]. When [collapsible] is true, either shows AdMob's native
     * collapsible banner, or - if [RemoteConfig.useNativeCollapseBanner] is on - a plain banner in
     * [container] that gets fully covered by a native ad strip floated over the activity's content
     * (the "custom collapse banner", backed by the shared [NativePlacement.COLLAPSE_BANNER] slot).
     * The native strip is added outside [container]'s own layout flow (bottom-docked over the
     * activity's content root), so - like AdMob's real collapsible banner - it never pushes the
     * screen's content, it draws over it; [container] itself is just hidden (not resized) while the
     * strip is up, so nothing jumps when it's dismissed and the plain banner reappears. This is the
     * single entry point for both variants so every screen's [collapsible] slot gets the swap for
     * free via Remote Config. Pure routing - the actual AdMob load lives in [loadAdMobBanner].
     */
    fun load(
        activity: Activity,
        container: ViewGroup,
        collapsible: Boolean = false,
        onStatus: (String) -> Unit = {},
    ): AdHandle {
        if (collapsible && RemoteConfig.useNativeCollapseBanner) {
            return loadNativeCollapseBanner(activity, container, onStatus)
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
        var current: AdView? = null
        var destroyed = false
        var shimmer: AdBannerShimmerBinding? = showShimmer(activity, container)

        fun clearShimmer() {
            shimmer?.let { it.root.stopShimmer(); container.removeView(it.root) }
            shimmer = null
        }

        val widthPixels = if (container.width > 0) container.width else activity.resources.displayMetrics.widthPixels
        val widthDp = (widthPixels / activity.resources.displayMetrics.density).toInt().coerceAtLeast(320)
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
        val ids = if (collapsible) Ads.ids.bannerCollapsible else Ads.ids.banner
        WaterfallLoader<AdView>(ids).load(
            attempt = { id, loaded, failed ->
                if (!destroyed) onStatus("Banner: loading $id")
                val adView = AdView(activity)
                val request = BannerAdRequest.Builder(id, size).apply {
                    if (collapsible) setGoogleExtrasBundle(Bundle().apply { putString("collapsible", "bottom") })
                }.build()
                adView.loadAd(
                    request,
                    object : AdLoadCallback<BannerAd> {
                        override fun onAdLoaded(ad: BannerAd) = loaded(adView)
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            adView.destroy()
                            if (!destroyed) failed(adError.message)
                        }
                    },
                )
            },
            onLoaded = { adView, _ ->
                activity.runOnUiThread {
                    if (destroyed) adView.destroy() else {
                        clearShimmer()
                        current?.destroy()
                        current = adView
                        container.removeAllViews()
                        container.addView(adView)
                        onStatus("Banner: ready")
                    }
                }
            },
            onFailed = {
                if (!destroyed) {
                    onStatus("Banner: $it")
                    activity.runOnUiThread { clearShimmer() }
                }
            },
        )
        return AdHandle {
            destroyed = true
            activity.runOnUiThread {
                current?.destroy()
                current = null
                clearShimmer()
                container.removeAllViews()
            }
        }
    }

    /** Shows a shimmering skeleton in [container] so the banner slot never pops in from empty. */
    private fun showShimmer(activity: Activity, container: ViewGroup): AdBannerShimmerBinding {
        val binding = AdBannerShimmerBinding.inflate(LayoutInflater.from(activity))
        container.removeAllViews()
        container.addView(binding.root)
        binding.root.startShimmer()
        return binding
    }

    /**
     * The "custom collapse banner": [container] loads a plain banner as usual. A native ad for
     * [NativePlacement.COLLAPSE_BANNER] loads in the background into a detached [FrameLayout]; once
     * it's ready, that layout is docked to the bottom of the activity's content root (floating over
     * whatever's there, per [attachOverlay]) and [container] is hidden so the banner underneath
     * isn't obscured by it. Dismissing the strip detaches it for good and reveals the banner again.
     */
    private fun loadNativeCollapseBanner(
        activity: Activity,
        container: ViewGroup,
        onStatus: (String) -> Unit,
    ): AdHandle {
        val overlay = FrameLayout(activity)
        val bannerHandle = loadAdMobBanner(activity, container, collapsible = false, onStatus = onStatus)

        var nativeHandle: AdHandle? = null
        nativeHandle = NativeAds.subscribe(
            activity, NativePlacement.COLLAPSE_BANNER, overlay, preloadOnShow = true,
            render = { act, ad ->
                container.visibility = View.INVISIBLE
                attachOverlay(act, overlay)
                renderCollapsibleNativeCard(act, ad) {
                    detachOverlay(overlay)
                    container.visibility = View.VISIBLE
                    nativeHandle?.destroy()
                }
            },
        )

        return AdHandle {
            detachOverlay(overlay)
            nativeHandle.destroy()
            bannerHandle.destroy()
            container.visibility = View.VISIBLE
        }
    }

    /**
     * Docks [overlay] to the bottom of [activity]'s content root, outside any screen's own layout
     * flow, so it floats over the current content instead of pushing it - matching how AdMob's own
     * collapsible banner expands over the app rather than resizing it. Insets itself off the bottom
     * system bar since it sits outside the padding [BaseActivity] applies to the screen's own root.
     */
    private fun attachOverlay(activity: Activity, overlay: FrameLayout) {
        if (overlay.parent != null) return
        overlay.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM,
        )
        ViewCompat.setOnApplyWindowInsetsListener(overlay) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bars.bottom)
            insets
        }
        activity.window.decorView.findViewById<ViewGroup>(android.R.id.content).addView(overlay)
    }

    /** Removes [overlay] from wherever [attachOverlay] docked it, if anywhere. */
    private fun detachOverlay(overlay: FrameLayout) {
        (overlay.parent as? ViewGroup)?.removeView(overlay)
    }

    /** Renders a properly-sized native ad card (icon + headline + body + CTA) with a corner close button that dismisses it for good. */
    private fun renderCollapsibleNativeCard(activity: Activity, ad: NativeAd, onDismiss: () -> Unit): NativeAdView {
        val binding = AdNativeCollapseBinding.inflate(LayoutInflater.from(activity))
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
        binding.adIcon.visibility = if (ad.icon == null) View.GONE else View.VISIBLE
        binding.adBody.visibility = if (ad.body == null) View.GONE else View.VISIBLE
        binding.adAdvertiser.visibility = if (ad.advertiser == null) View.GONE else View.VISIBLE
        binding.adCallToAction.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
        view.registerNativeAd(ad, binding.adMedia)

        binding.adClose.setOnClickListener { onDismiss() }
        return view
    }
}
