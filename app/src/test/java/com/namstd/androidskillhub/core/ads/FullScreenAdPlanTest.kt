package com.namstd.androidskillhub.core.ads

import com.namstd.androidskillhub.core.config.EnableConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class FullScreenAdPlanTest {
    private fun cfg(adsSplash: Int = 2, inter: Int = 1) =
        EnableConfig(adsSplash = adsSplash, inter = inter, banner = 2, onresume = 1)

    @Test
    fun splashMapsAdsSplashValue() {
        assertEquals(FullScreenAdPlan.APP_OPEN, resolveFullScreenPlan(InterstitialPlacement.SPLASH, cfg(adsSplash = 1)))
        assertEquals(FullScreenAdPlan.INTERSTITIAL, resolveFullScreenPlan(InterstitialPlacement.SPLASH, cfg(adsSplash = 2)))
        assertEquals(FullScreenAdPlan.INTER_PLUS_NATIVE, resolveFullScreenPlan(InterstitialPlacement.SPLASH, cfg(adsSplash = 3)))
        assertEquals(FullScreenAdPlan.OFF, resolveFullScreenPlan(InterstitialPlacement.SPLASH, cfg(adsSplash = 0)))
    }

    @Test
    fun interPlacementsMapInterValue() {
        for (placement in listOf(InterstitialPlacement.LANGUAGE, InterstitialPlacement.INTRO)) {
            assertEquals(FullScreenAdPlan.INTERSTITIAL, resolveFullScreenPlan(placement, cfg(inter = 1)))
            assertEquals(FullScreenAdPlan.NATIVE_ONLY, resolveFullScreenPlan(placement, cfg(inter = 2)))
            assertEquals(FullScreenAdPlan.INTER_PLUS_NATIVE, resolveFullScreenPlan(placement, cfg(inter = 3)))
            assertEquals(FullScreenAdPlan.OFF, resolveFullScreenPlan(placement, cfg(inter = 0)))
        }
    }

    @Test
    fun unknownValuesFallBackToOff() {
        assertEquals(FullScreenAdPlan.OFF, resolveFullScreenPlan(InterstitialPlacement.SPLASH, cfg(adsSplash = 9)))
        assertEquals(FullScreenAdPlan.OFF, resolveFullScreenPlan(InterstitialPlacement.LANGUAGE, cfg(inter = 9)))
    }
}
