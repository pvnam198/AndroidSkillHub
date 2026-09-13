package com.namstd.androidskillhub.core.ads

/** App-wide guard that prevents ads and dialogs from covering each other. */
object FullScreenGate {
    private var owner: Any? = null

    @Synchronized
    fun acquire(candidate: Any): Boolean {
        if (owner != null) return false
        owner = candidate
        return true
    }

    @Synchronized
    fun release(candidate: Any) {
        if (owner === candidate) owner = null
    }

    @Synchronized
    fun isBusy(): Boolean = owner != null

    internal fun clearForTest() {
        owner = null
    }
}
