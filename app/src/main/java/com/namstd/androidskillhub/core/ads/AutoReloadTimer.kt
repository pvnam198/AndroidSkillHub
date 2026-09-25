package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.namstd.androidskillhub.core.config.RemoteConfig

/**
 * Banner slot auto reload, shared by [BannerAds]' AdMob banners and [NativeCollapseAd] (the custom
 * native that can take the banner slot's place) - not used by any other native ad. After
 * each [schedule], if [RemoteConfig.bannerSlotAutoReload] is on, fires [onReload] once
 * [RemoteConfig.bannerSlotReloadGapMs] has passed. Both are read at every [schedule], so a new
 * fetch applies from the next cycle. A reload falling due while the
 * activity isn't resumed waits for onResume, so a slot never reloads behind another screen.
 * Main thread only.
 */
internal class AutoReloadTimer(activity: Activity, private val onReload: () -> Unit) {
    private val lifecycle: Lifecycle? = (activity as? LifecycleOwner)?.lifecycle
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { fireOrDefer() }
    private var due = false
    private var cancelled = false

    private val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            if (due) fireOrDefer()
        }
    }

    init {
        lifecycle?.addObserver(observer)
    }

    /** (Re)starts the countdown to the next reload - call it each time the slot shows an ad or gives up on one. */
    fun schedule() {
        handler.removeCallbacks(tick)
        due = false
        if (cancelled || !RemoteConfig.bannerSlotAutoReload) return
        handler.postDelayed(tick, RemoteConfig.bannerSlotReloadGapMs)
    }

    fun cancel() {
        cancelled = true
        due = false
        handler.removeCallbacks(tick)
        lifecycle?.removeObserver(observer)
    }

    private fun fireOrDefer() {
        if (cancelled) return
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == false) {
            due = true
            return
        }
        due = false
        onReload()
    }

    companion object {
        /** On a reload, the slot shows its small shimmer at least this long before the new ad appears. */
        const val MIN_RELOAD_SHIMMER_MS = 1_200L
    }
}
