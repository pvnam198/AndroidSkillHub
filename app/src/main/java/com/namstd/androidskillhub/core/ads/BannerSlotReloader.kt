package com.namstd.androidskillhub.core.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner

/**
 * A banner slot's handle: [destroy] tears it down, [reload] swaps in a fresh ad right now - for
 * screens that reload by hand (on resume, on a tab switch...) instead of, or on top of, the timer.
 */
interface BannerSlotHandle : AdHandle {
    /** Reloads now: back to the small shimmer, then the new ad. Ignored while the slot is already loading. */
    fun reload()

    companion object {
        /** For a slot that never loaded (SDK not ready, ads off): nothing to reload or destroy. */
        val NONE = object : BannerSlotHandle {
            override fun reload() = Unit
            override fun destroy() = Unit
        }
    }
}

/**
 * The banner slot's reload timer - shared by [BannerAds]' AdMob banners and [NativeCollapseAd] (the
 * custom native that can take the banner slot's place); no other native ad uses it. That's the only
 * trigger built in: any other (resume, tab switch...) is the screen's own code calling [reloadNow].
 *
 * With [autoReload] on and [gapMs] > 0, the slot reloads [gapMs] after each ad is shown (or a load
 * fails). Both come from the caller - the base screens default them from Remote Config; core never
 * reads config itself. A reload falling due while the activity isn't resumed waits for onResume. Nothing reloads while a load is already in flight. The slot
 * reports its progress through [onLoadStarted], [onShown] and [onFailed]. Main thread only.
 */
internal class BannerSlotReloader(
    activity: Activity,
    /** Whether the timer runs at all. */
    private val autoReload: Boolean,
    /** Time from an ad being shown (or a load failing) to the timer's reload; 0 = no timer. */
    private val gapMs: Long,
    private val onReload: () -> Unit,
) {
    private val lifecycle: Lifecycle? = (activity as? LifecycleOwner)?.lifecycle
    private val handler = Handler(Looper.getMainLooper())
    private val tick = Runnable { fireTimer() }

    /** True from a load's start until its ad is shown or it fails. Starts true: the slot's first load is on its way. */
    private var busy = true
    /** A timer reload that fell due while the activity wasn't resumed. */
    private var due = false
    private var cancelled = false

    private val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            if (due) fireTimer()
        }
    }

    init {
        lifecycle?.addObserver(observer)
    }

    /** Reload by hand - the screen decides when; only an in-flight load stops it. */
    fun reloadNow() {
        if (cancelled || busy) return
        due = false
        onReload()
    }

    fun onLoadStarted() {
        busy = true
        due = false
        handler.removeCallbacks(tick)
    }

    /** The ad is on screen - the timer's gap starts now. */
    fun onShown() = settle()

    /** Nothing to show - try again one gap later. */
    fun onFailed() = settle()

    fun cancel() {
        cancelled = true
        due = false
        handler.removeCallbacks(tick)
        lifecycle?.removeObserver(observer)
    }

    private fun fireTimer() {
        if (cancelled || busy) return
        if (lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == false) {
            due = true
            return
        }
        reloadNow()
    }

    private fun settle() {
        busy = false
        handler.removeCallbacks(tick)
        // No gap, no timer: it would reload the instant each ad appears.
        if (cancelled || !autoReload || gapMs <= 0) return
        handler.postDelayed(tick, gapMs)
    }

    companion object {
        /** On a reload, the slot shows its small shimmer at least this long before the new ad appears. */
        const val MIN_RELOAD_SHIMMER_MS = 1_200L
    }
}
