package com.namstd.androidskillhub.feature.home

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.namstd.androidskillhub.R
import com.namstd.androidskillhub.core.ads.Ads
import com.namstd.androidskillhub.core.ads.InterstitialAds
import com.namstd.androidskillhub.feature.language.LanguageActivity
import com.namstd.androidskillhub.feature.main.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainFlowTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Before fun setUp() {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        Ads.testMode = true
        Ads.adsEnabled = true
        InterstitialAds.clear()
    }

    @After fun tearDown() {
        Ads.testMode = false
        Ads.adsEnabled = true
        InterstitialAds.clear()
    }

    @Test fun firstRunThenRelaunchCompletesFullFlowWithoutNetwork() {
        ActivityScenario.launch(LanguageActivity::class.java).use {
            onView(withText(R.string.choose_language)).check(matches(isDisplayed()))
            onView(withText("English")).perform(click())
            onView(withId(R.id.apply_button)).perform(click())
            onView(withText("1 / 4")).check(matches(isDisplayed()))
            onView(withId(R.id.next_button)).perform(click(), click(), click(), click())
            onView(withText(R.string.home_title)).check(matches(isDisplayed()))
            onView(withContentDescription("banner-ad")).check(matches(isDisplayed()))
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText(R.string.home_title)).check(matches(isDisplayed()))
        }
    }

    @Test fun settingsResetPrivacyAndPremiumRemovesAds() {
        preferences().edit().putBoolean("setup_completed", true).commit()
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.settings_button)).perform(click())
            onView(withId(R.id.privacy_button)).check(matches(isDisplayed())).perform(click())
            onView(withId(R.id.reset_onboarding_button)).perform(click())
            onView(withId(R.id.premium_button)).perform(click())
            onView(withId(R.id.upgrade_button)).perform(click())
            onView(withText(R.string.premium_active)).check(matches(isDisplayed()))
            onView(withContentDescription("banner-ad")).check(doesNotExist())
        }
    }

    private fun preferences() = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
}
