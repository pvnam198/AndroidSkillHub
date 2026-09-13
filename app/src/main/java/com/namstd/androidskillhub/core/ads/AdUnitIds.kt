package com.namstd.androidskillhub.core.ads

/**
 * Ordered fallback lists. These defaults are Google's Android test ad units.
 * Replace every list, along with the manifest app ID, before publishing.
 */
data class AdUnitIds(
    val banner: List<String> = listOf(TEST_BANNER),
    val bannerCollapsible: List<String> = listOf(TEST_BANNER_COLLAPSIBLE),
    val interstitial: List<String> = listOf(TEST_INTERSTITIAL),
    val native: Map<NativePlacement, List<String>> = NativePlacement.entries.associateWith { listOf(TEST_NATIVE) },
    val rewarded: List<String> = listOf(TEST_REWARDED),
    val rewardedInterstitial: List<String> = listOf(TEST_REWARDED_INTERSTITIAL),
    val appOpen: List<String> = listOf(TEST_APP_OPEN),
) {
    companion object {
        const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_BANNER_COLLAPSIBLE = "ca-app-pub-3940256099942544/2014213617"
        const val TEST_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"
        const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_REWARDED_INTERSTITIAL = "ca-app-pub-3940256099942544/5354046379"
        const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
    }
}
