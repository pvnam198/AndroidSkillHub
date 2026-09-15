package com.namstd.androidskillhub.feature.main

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.NativeAds
import com.namstd.androidskillhub.core.ads.NativePlacement
import com.namstd.androidskillhub.core.ui.base.BaseRecyclerAdapter
import com.namstd.androidskillhub.databinding.AdNativeBinding
import com.namstd.androidskillhub.databinding.AdNativeShimmerBinding

sealed interface MainFeedItem {
    data class Content(val title: String) : MainFeedItem
    data object AdSlot : MainFeedItem
}

/** Places an [MainFeedItem.AdSlot] at position [FIRST_AD_INDEX], then every [AD_INTERVAL] items after that. */
fun buildMainFeedItems(contentTitles: List<String>): List<MainFeedItem> {
    val result = mutableListOf<MainFeedItem>()
    var nextAdIndex = FIRST_AD_INDEX
    for (title in contentTitles) {
        if (result.size == nextAdIndex) {
            result += MainFeedItem.AdSlot
            nextAdIndex += AD_INTERVAL
        }
        result += MainFeedItem.Content(title)
    }
    return result
}

private const val FIRST_AD_INDEX = 2 // position 3, 0-based
private const val AD_INTERVAL = 8
private const val POLL_INTERVAL_MS = 400L

/**
 * A [MainFeedItem.AdSlot] never runs its own ad request: it repeatedly [NativeAds.poll]s the
 * shared [NativePlacement.HOME_FEED] slot while it's on screen, so whichever slot happens to be
 * visible when the next ad finishes loading is the one that wins it - a slot that gets scrolled
 * past before that stops polling ([onViewRecycled]) and simply never claims an ad, instead of
 * wasting a dedicated request of its own. Once a position wins an ad it keeps it forever in
 * [loadedAds] and never polls again for that position.
 */
class MainFeedAdapter(
    private val activity: Activity,
    items: List<MainFeedItem>,
) : BaseRecyclerAdapter<MainFeedItem, RecyclerView.ViewHolder>(items) {

    private val loadedAds = mutableMapOf<Int, NativeAd>()

    override fun getItemViewType(position: Int) = when (items[position]) {
        is MainFeedItem.Content -> TYPE_CONTENT
        is MainFeedItem.AdSlot -> TYPE_NATIVE_AD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        when (viewType) {
            TYPE_NATIVE_AD -> {
                val container = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_main_native_ad, parent, false) as FrameLayout
                NativeAdViewHolder(activity, container)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_main_content, parent, false) as TextView
                ContentViewHolder(view)
            }
        }

    override fun bind(holder: RecyclerView.ViewHolder, item: MainFeedItem, position: Int) {
        when {
            holder is ContentViewHolder && item is MainFeedItem.Content -> holder.bind(item.title)
            holder is NativeAdViewHolder && item is MainFeedItem.AdSlot -> bindAdSlot(holder, position)
        }
    }

    private fun bindAdSlot(holder: NativeAdViewHolder, position: Int) {
        val cached = loadedAds[position]
        if (cached != null) {
            holder.show(cached)
            return
        }
        holder.showShimmer()
        holder.startPolling { ad -> loadedAds[position] = ad }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? NativeAdViewHolder)?.cancelPolling()
    }

    /** Destroys every ad this list ever won and clears the cache - call when the screen is torn down. */
    fun release() {
        loadedAds.values.forEach(NativeAd::destroy)
        loadedAds.clear()
    }

    class ContentViewHolder(private val text: TextView) : RecyclerView.ViewHolder(text) {
        fun bind(title: String) {
            text.text = title
        }
    }

    class NativeAdViewHolder(
        private val activity: Activity,
        private val container: FrameLayout,
    ) : RecyclerView.ViewHolder(container) {

        private val pollRunnable = Runnable { poll() }
        private var onWon: ((NativeAd) -> Unit)? = null

        fun startPolling(onWon: (NativeAd) -> Unit) {
            this.onWon = onWon
            poll()
        }

        private fun poll() {
            val ad = NativeAds.poll(NativePlacement.HOME_FEED)
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
