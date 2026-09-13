package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.namstd.androidskillhub.databinding.AdBannerShimmerBinding

object BannerAds {
    fun load(
        activity: Activity,
        container: ViewGroup,
        collapsible: Boolean = false,
        onStatus: (String) -> Unit = {},
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
}
