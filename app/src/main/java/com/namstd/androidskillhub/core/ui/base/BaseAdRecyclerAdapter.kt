package com.namstd.androidskillhub.core.ui.base

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.databinding.AdNativeBinding
import com.namstd.androidskillhub.databinding.AdNativeShimmerBinding

sealed interface AdFeedItem<out T> {
    data class Content<T>(val value: T) : AdFeedItem<T>
    data object AdSlot : AdFeedItem<Nothing>
}

private const val POLL_INTERVAL_MS = 400L

/**
 * RecyclerView adapter that interleaves native ads into a content list. [items] is built by the
 * screen itself - where [AdFeedItem.AdSlot] is placed and how far apart is entirely up to the
 * caller, this base only takes care of the native ad part of each slot.
 *
 * A slot never runs its own ad request: it repeatedly [NativeAds.poll]s the shared [placement]
 * slot while it's on screen, so whichever slot happens to be visible when the next ad finishes
 * loading is the one that wins it - a slot scrolled past before that stops polling
 * ([onViewRecycled]) and simply never claims an ad, instead of wasting a dedicated request of its
 * own. Once a position wins an ad it keeps it forever and never polls again for that position.
 *
 * Subclasses only provide how to create/bind the content item's own ViewHolder.
 */
abstract class BaseAdRecyclerAdapter<T>(
    private val activity: Activity,
    items: List<AdFeedItem<T>>,
    private val placement: NativePlacement,
) : BaseRecyclerAdapter<AdFeedItem<T>, RecyclerView.ViewHolder>(items) {

    private val loadedAds = mutableMapOf<Int, NativeAd>()

    protected abstract fun onCreateContentViewHolder(parent: ViewGroup): RecyclerView.ViewHolder
    protected abstract fun onBindContent(holder: RecyclerView.ViewHolder, value: T, position: Int)

    final override fun getItemViewType(position: Int) = when (items[position]) {
        is AdFeedItem.Content -> TYPE_CONTENT
        is AdFeedItem.AdSlot -> TYPE_NATIVE_AD
    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_NATIVE_AD) {
            val container = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_native_ad_slot, parent, false) as FrameLayout
            NativeAdViewHolder(activity, container)
        } else {
            onCreateContentViewHolder(parent)
        }

    final override fun bind(holder: RecyclerView.ViewHolder, item: AdFeedItem<T>, position: Int) {
        when (item) {
            is AdFeedItem.Content -> onBindContent(holder, item.value, position)
            is AdFeedItem.AdSlot -> bindAdSlot(holder as NativeAdViewHolder, position)
        }
    }

    private fun bindAdSlot(holder: NativeAdViewHolder, position: Int) {
        val cached = loadedAds[position]
        if (cached != null) {
            holder.show(cached)
            return
        }
        holder.showShimmer()
        holder.startPolling(placement) { ad -> loadedAds[position] = ad }
    }

    final override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? NativeAdViewHolder)?.cancelPolling()
    }

    /** Destroys every ad this list ever won and clears the cache - call when the screen is torn down. */
    fun release() {
        loadedAds.values.forEach(NativeAd::destroy)
        loadedAds.clear()
    }

    private class NativeAdViewHolder(
        private val activity: Activity,
        private val container: FrameLayout,
    ) : RecyclerView.ViewHolder(container) {

        private val pollRunnable = Runnable { poll() }
        private var placement: NativePlacement? = null
        private var onWon: ((NativeAd) -> Unit)? = null

        fun startPolling(placement: NativePlacement, onWon: (NativeAd) -> Unit) {
            this.placement = placement
            this.onWon = onWon
            poll()
        }

        private fun poll() {
            val placement = placement ?: return
            val ad = NativeAds.poll(placement)
            if (ad != null) {
                onWon?.invoke(ad)
                onWon = null
                show(ad)
            } else {
                container.postDelayed(pollRunnable, POLL_INTERVAL_MS)
            }
        }

        fun cancelPolling() {
            container.removeCallbacks(pollRunnable)
            onWon = null
        }

        fun show(ad: NativeAd) {
            val binding = AdNativeBinding.inflate(LayoutInflater.from(activity))
            populate(ad, binding)
            container.removeAllViews()
            container.addView(binding.root)
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

        fun showShimmer() {
            val shimmer = AdNativeShimmerBinding.inflate(LayoutInflater.from(activity))
            container.removeAllViews()
            container.addView(shimmer.root)
            shimmer.root.startShimmer()
        }
    }

    private companion object {
        const val TYPE_CONTENT = 0
        const val TYPE_NATIVE_AD = 1
    }
}
