package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.config.RemoteConfig
import com.namstd.androidskillhub.databinding.AdBannerShimmerBinding
import com.namstd.androidskillhub.databinding.AdNativeCollapseBinding

object BannerAds {
    /**
     * Loads a banner into [container]. When [collapsible] is true, either shows AdMob's native
     * collapsible banner, or - if [RemoteConfig.useNativeCollapseBanner] is on - a plain banner
     * topped with a toggleable native ad strip that the user expands or collapses by tapping it
     * (the "custom collapse banner", backed by the shared [NativePlacement.COLLAPSE_BANNER] slot).
     * This is the single entry point for both variants so every screen's [collapsible] slot gets
     * the swap for free via Remote Config.
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

    /** The "custom collapse banner": a toggleable native ad strip stacked above a plain (non-collapsible) banner. */
    private fun loadNativeCollapseBanner(
        activity: Activity,
        container: ViewGroup,
        onStatus: (String) -> Unit,
    ): AdHandle {
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val nativeSlot = FrameLayout(activity)
        val bannerSlot = FrameLayout(activity)
        column.addView(nativeSlot)
        column.addView(bannerSlot)
        container.removeAllViews()
        container.addView(column)

        val nativeHandle =
            NativeAds.subscribe(activity, NativePlacement.COLLAPSE_BANNER, nativeSlot, render = ::renderCollapsibleNativeCard)
        val bannerHandle = load(activity, bannerSlot, collapsible = false, onStatus = onStatus)

        return AdHandle {
            nativeHandle.destroy()
            bannerHandle.destroy()
            container.removeAllViews()
        }
    }

    /** Renders a native ad that starts collapsed (icon + headline) and expands/collapses on tap. */
    private fun renderCollapsibleNativeCard(activity: Activity, ad: NativeAd): NativeAdView {
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
        binding.adBody.visibility = if (ad.body == null) View.GONE else View.VISIBLE
        binding.adAdvertiser.visibility = if (ad.advertiser == null) View.GONE else View.VISIBLE
        binding.adCallToAction.visibility = if (ad.callToAction == null) View.GONE else View.VISIBLE
        binding.adIcon.visibility = if (ad.icon == null) View.GONE else View.VISIBLE
        view.registerNativeAd(ad, binding.adMedia)

        var expanded = false
        binding.adToggle.setOnClickListener {
            expanded = !expanded
            TransitionManager.beginDelayedTransition(view, AutoTransition())
            binding.adExpandGroup.visibility = if (expanded) View.VISIBLE else View.GONE
            binding.adToggle.setText(if (expanded) R.string.ad_toggle_collapse else R.string.ad_toggle_expand)
        }
        return view
    }
}
