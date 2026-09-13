package com.namstd.androidskillhub.core.ads

internal class RewardOnce(private val deliver: (AdReward) -> Unit) {
    private var delivered = false

    fun send(reward: AdReward) {
        if (delivered) return
        delivered = true
        deliver(reward)
    }
}
