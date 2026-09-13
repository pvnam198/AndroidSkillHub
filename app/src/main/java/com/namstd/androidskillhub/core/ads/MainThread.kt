package com.namstd.androidskillhub.core.ads

import android.os.Handler
import android.os.Looper

private val mainThreadHandler = Handler(Looper.getMainLooper())

/** Runs [block] on the main thread — ads-mobile-sdk fires full-screen-content callbacks off a background dispatcher. */
internal fun runOnMainThread(block: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) block() else mainThreadHandler.post(block)
}
